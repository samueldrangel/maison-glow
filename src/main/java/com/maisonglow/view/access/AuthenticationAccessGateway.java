package com.maisonglow.view.access;

import com.maisonglow.exception.AuthorizationException;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Customer;
import com.maisonglow.model.UserAccount;
import com.maisonglow.model.UserRole;
import com.maisonglow.service.AuthenticationService;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.CustomerService;
import com.maisonglow.util.Validator;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * The account service ({@link AuthenticationService}) seen by the access screens. It translates what the forms hold
 * (texts) into what the service asks for (passwords as {@code char[]}, wiped after each call) and its accounts into
 * an {@link AccessSession} for the window. It adds no rule of its own beyond what a form needs: the confirmation of
 * the password, which only exists on screen, and the messages for empty fields. Credentials, permissions and the
 * session itself stay in the service; this class keeps no copy of any of them.
 */
public final class AuthenticationAccessGateway implements AccessGateway {

    /** What the service says when there is no session; the screens treat it as an ended session. */
    private static final String NO_SESSION = "Inicia sesión para continuar.";

    private final AuthenticationService auth;
    private final CustomerService customers;

    /**
     * Creates the adapter.
     *
     * @param auth      the account service that holds the session
     * @param customers gives the name of the customer behind an account
     */
    public AuthenticationAccessGateway(AuthenticationService auth, CustomerService customers) {
        this.auth = Objects.requireNonNull(auth, "auth");
        this.customers = Objects.requireNonNull(customers, "customers");
    }

    @Override
    public AccessSession login(String username, String password)
        throws ValidationException, BusinessRuleException, DataAccessException {
        Validator.requireText(username, "usuario");
        char[] secret = existingPassword(password, "contraseña");
        try {
            return toSession(auth.login(username, secret));
        } finally {
            Arrays.fill(secret, '\0');
        }
    }

    @Override
    public AccessSession register(RegistrationData data)
        throws ValidationException, BusinessRuleException, DataAccessException {
        Objects.requireNonNull(data, "data");
        Validator.requireText(data.username(), "usuario");
        Validator.requireText(data.password(), "contraseña");
        requireSame(data.password(), data.confirmation());
        char[] secret = data.password().toCharArray();
        try {
            auth.registerCustomer(new ContactData(trim(data.document()), trim(data.name()), trim(data.phone()),
                trim(data.email())), data.username(), secret);
            // The service does not sign in a new account by itself.
            return toSession(auth.login(data.username(), secret));
        } finally {
            Arrays.fill(secret, '\0');
        }
    }

    @Override
    public void changePassword(String current, String replacement, String confirmation)
        throws ValidationException, BusinessRuleException, DataAccessException {
        Validator.requireText(replacement, "contraseña nueva");
        requireSame(replacement, confirmation);
        char[] old = existingPassword(current, "contraseña actual");
        char[] fresh = replacement.toCharArray();
        try {
            auth.changePassword(old, fresh);
        } catch (AuthorizationException e) {
            throw NO_SESSION.equals(e.getMessage()) ? new SessionExpiredException() : e;
        } finally {
            Arrays.fill(old, '\0');
            Arrays.fill(fresh, '\0');
        }
    }

    @Override
    public void logout() {
        auth.logout();
    }

    @Override
    public Optional<AccessSession> current() {
        try {
            Optional<UserAccount> account = auth.currentSession();
            return account.isPresent() ? Optional.of(toSession(account.get())) : Optional.empty();
        } catch (DataAccessException e) {
            return Optional.empty();
        }
    }

    private AccessSession toSession(UserAccount account) {
        AccessRole role = account.role() == UserRole.ADMIN ? AccessRole.ADMIN : AccessRole.CUSTOMER;
        return new AccessSession(account.username(), nameOf(account), role);
    }

    /** The name of the customer for a customer account; the username for an administrator or if the record is gone. */
    private String nameOf(UserAccount account) {
        if (account.customerId() == null) {
            return account.username();
        }
        try {
            Customer customer = customers.findById(account.customerId());
            return customer.getName();
        } catch (BusinessRuleException | DataAccessException e) {
            return account.username();
        }
    }

    private static void requireSame(String password, String confirmation) throws ValidationException {
        if (!Objects.equals(password, confirmation)) {
            throw new ValidationException("La confirmación de la contraseña no coincide.");
        }
    }

    private static char[] existingPassword(String password, String field) throws ValidationException {
        if (password == null || password.isEmpty()) {
            throw new ValidationException("El campo " + field + " es obligatorio.");
        }
        return password.toCharArray();
    }

    private static String trim(String text) {
        return text == null ? "" : text.trim();
    }
}
