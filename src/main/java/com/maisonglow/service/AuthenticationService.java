package com.maisonglow.service;

import com.maisonglow.dao.AccountDao;
import com.maisonglow.dao.CustomerDao;
import com.maisonglow.exception.AuthorizationException;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.UserAccount;
import com.maisonglow.model.UserRole;
import com.maisonglow.service.auth.PasswordHasher;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/** Account lifecycle and authorization for one desktop session. No implicit administrator session. */
public final class AuthenticationService {
    private final AccountDao accounts;
    private final CustomerDao customers;
    private final CustomerService customerService;
    private final TransactionRunner transactions;
    private final PasswordHasher passwords;
    private final Clock clock;
    private Session session;

    private record Session(UserAccount account, int version, Instant expiresAt) { }

    public AuthenticationService(AccountDao accounts, CustomerDao customers, CustomerService customerService,
                                 TransactionRunner transactions, PasswordHasher passwords, Clock clock) {
        this.accounts = accounts;
        this.customers = customers;
        this.customerService = customerService;
        this.transactions = transactions;
        this.passwords = passwords;
        this.clock = clock;
    }

    /** Atomically creates a NEW customer and its CUSTOMER account. Does not claim existing records. */
    public synchronized UserAccount registerCustomer(ContactData contact, String username, char[] password)
        throws ValidationException, BusinessRuleException, DataAccessException {
        if (contact == null) throw new ValidationException("Los datos del cliente son obligatorios.");
        contact.validate();
        String normalized = normalizeUsername(username);
        String hash = passwords.hash(password);
        return transactions.execute(() -> {
            requireAvailable(normalized);
            var customer = customerService.register(contact);
            return accounts.create(normalized, hash, UserRole.CUSTOMER, customer.getId());
        });
    }

    /** Local installation setup only; refused permanently once an administrator exists. */
    public synchronized UserAccount createFirstAdmin(String username, char[] password)
        throws ValidationException, BusinessRuleException, DataAccessException {
        String normalized = normalizeUsername(username);
        String hash = passwords.hash(password);
        return transactions.execute(() -> {
            if (accounts.countAdministrators(false) != 0)
                throw new BusinessRuleException("La instalación ya tiene un administrador.");
            requireAvailable(normalized);
            return accounts.create(normalized, hash, UserRole.ADMIN, null);
        });
    }

    /** Administrator-verified linking of an existing customer; never available to public signup. */
    public synchronized UserAccount linkCustomerAccount(int customerId, String username, char[] password)
        throws ValidationException, BusinessRuleException, DataAccessException {
        requireAdmin();
        String normalized = normalizeUsername(username);
        String hash = passwords.hash(password);
        return transactions.execute(() -> {
            requireAdmin();
            if (customers.findById(customerId).isEmpty()) throw new BusinessRuleException("El cliente no existe.");
            requireAvailable(normalized);
            return accounts.create(normalized, hash, UserRole.CUSTOMER, customerId);
        });
    }

    /** Starting any login attempt discards the previous account, including on failure. */
    public synchronized UserAccount login(String username, char[] password)
        throws ValidationException, AuthorizationException, DataAccessException {
        logout();
        String normalized = normalizeUsername(username);
        var credentials = accounts.findByUsername(normalized).orElse(null);
        if (credentials == null || !passwords.matches(password, credentials.passwordHash())
            || !credentials.account().active())
            throw new AuthorizationException("Usuario o contraseña incorrectos, o cuenta no disponible.");
        session = new Session(credentials.account(), credentials.authVersion(), clock.instant().plus(Duration.ofHours(8)));
        return session.account();
    }

    /** Reloads active status and credential version on every authorization, including across app instances. */
    public synchronized Optional<UserAccount> currentSession() throws DataAccessException {
        if (session == null) return Optional.empty();
        if (!clock.instant().isBefore(session.expiresAt())) { logout(); return Optional.empty(); }
        var stored = accounts.findById(session.account().id()).orElse(null);
        if (stored == null || !stored.account().active() || stored.authVersion() != session.version()
            || !stored.account().equals(session.account())) { logout(); return Optional.empty(); }
        return Optional.of(stored.account());
    }

    public synchronized UserAccount requireAuthenticated() throws AuthorizationException, DataAccessException {
        return currentSession().orElseThrow(() -> new AuthorizationException("Inicia sesión para continuar."));
    }

    public synchronized UserAccount requireAdmin() throws AuthorizationException, DataAccessException {
        UserAccount account = requireAuthenticated();
        if (account.role() != UserRole.ADMIN) throw new AuthorizationException("Esta acción requiere un administrador.");
        return account;
    }

    /** For customer self-service methods: the id comes from the session, never a form selector. */
    public synchronized int currentCustomerId() throws AuthorizationException, DataAccessException {
        UserAccount account = requireAuthenticated();
        if (account.role() != UserRole.CUSTOMER || account.customerId() == null)
            throw new AuthorizationException("Esta acción requiere una cuenta de cliente.");
        return account.customerId();
    }

    /** For operations that may be performed by the owner or an administrator. */
    public synchronized void requireCustomerAccess(int customerId) throws AuthorizationException, DataAccessException {
        UserAccount account = requireAuthenticated();
        if (customerId <= 0 || (account.role() != UserRole.ADMIN && !Integer.valueOf(customerId).equals(account.customerId())))
            throw new AuthorizationException("No tienes permiso para acceder a estos datos.");
    }

    /** Successful password changes invalidate this and all other sessions for the account. */
    public synchronized void changePassword(char[] oldPassword, char[] newPassword)
        throws ValidationException, BusinessRuleException, DataAccessException {
        UserAccount account = requireAuthenticated();
        String hash = passwords.hash(newPassword);
        transactions.execute(() -> {
            requireAuthenticated();
            var stored = accounts.findById(account.id()).orElseThrow(() -> new AuthorizationException("La cuenta ya no existe."));
            if (!passwords.matches(oldPassword, stored.passwordHash()))
                throw new AuthorizationException("La contraseña actual no es correcta.");
            accounts.changePassword(account.id(), hash);
            return null;
        });
        logout();
    }

    /** A state change increments the credential version, so reactivation cannot revive old sessions. */
    public synchronized void setActive(int userId, boolean active)
        throws ValidationException, BusinessRuleException, DataAccessException {
        transactions.execute(() -> {
            requireAdmin();
            var stored = accounts.findById(userId).orElseThrow(() -> new BusinessRuleException("La cuenta no existe."));
            if (stored.account().active() == active) return null;
            if (!active && stored.account().role() == UserRole.ADMIN && accounts.countAdministrators(true) <= 1)
                throw new BusinessRuleException("No se puede desactivar el último administrador activo.");
            accounts.setActive(userId, active);
            return null;
        });
    }

    public synchronized void logout() { session = null; }

    private void requireAvailable(String username) throws BusinessRuleException, DataAccessException {
        if (accounts.findByUsername(username).isPresent()) throw new BusinessRuleException("El nombre de usuario ya está registrado.");
    }

    private static String normalizeUsername(String username) throws ValidationException {
        if (username == null) throw new ValidationException("El nombre de usuario es obligatorio.");
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9._@+-]{2,79}"))
            throw new ValidationException("El usuario debe tener entre 3 y 80 caracteres: letras sin tildes, números o . _ @ + -.");
        return normalized;
    }
}
