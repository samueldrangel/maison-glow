package com.maisonglow.view.access;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.util.TextSearch;
import com.maisonglow.util.Validator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory double of the account service for the tests of the access screens: nothing is written to disk and the
 * accounts disappear with the object. It has no administrator of its own; {@link #addAdministrator} creates one. The
 * rules it applies (a password of at least {@value #MIN_PASSWORD_LENGTH} characters, unique usernames) mirror the real
 * service, which the integration test of the adapter checks.
 */
public final class InMemoryAccessGateway implements AccessGateway {

    /** Shortest password accepted. */
    public static final int MIN_PASSWORD_LENGTH = 12;

    private final Map<String, Account> accounts = new HashMap<>();
    private AccessSession session;

    private record Account(String username, String displayName, String document, AccessRole role, String hash) { }

    /**
     * Creates an administrator account; the public sign-up cannot do this.
     *
     * @param username the identifier to sign in with
     * @param name     how to greet the administrator
     * @param password the password
     */
    public void addAdministrator(String username, String name, String password) {
        String key = key(username);
        accounts.put(key, new Account(key, name, "", AccessRole.ADMIN, hash(password)));
    }

    @Override
    public AccessSession login(String username, String password) throws ValidationException, BusinessRuleException {
        Validator.requireText(username, "usuario");
        Validator.requireText(password, "contraseña");
        Account account = accounts.get(key(username));
        if (account == null || !account.hash().equals(hash(password))) {
            throw new BusinessRuleException("Usuario o contraseña incorrectos.");
        }
        session = new AccessSession(account.username(), account.displayName(), account.role());
        return session;
    }

    @Override
    public AccessSession register(RegistrationData data) throws ValidationException, BusinessRuleException {
        Objects.requireNonNull(data, "data");
        Validator.requireText(data.name(), "nombre");
        Validator.requireText(data.document(), "documento");
        Validator.requirePhone(data.phone());
        Validator.requireEmail(data.email());
        Validator.requireText(data.username(), "usuario");
        requirePassword(data.password(), "contraseña");
        if (!Objects.equals(data.password(), data.confirmation())) {
            throw new ValidationException("La confirmación de la contraseña no coincide.");
        }
        String key = key(data.username());
        if (accounts.containsKey(key)) {
            throw new BusinessRuleException("Ya existe una cuenta con el usuario " + data.username().trim() + ".");
        }
        String document = data.document().trim();
        if (accounts.values().stream().anyMatch(account -> account.document().equals(document))) {
            throw new BusinessRuleException("Ya existe una cuenta con el documento " + document + ".");
        }
        accounts.put(key, new Account(key, data.name().trim(), document, AccessRole.CUSTOMER, hash(data.password())));
        session = new AccessSession(key, data.name().trim(), AccessRole.CUSTOMER);
        return session;
    }

    @Override
    public void changePassword(String current, String replacement, String confirmation)
        throws ValidationException, BusinessRuleException {
        Validator.requireText(current, "contraseña actual");
        requirePassword(replacement, "contraseña nueva");
        if (!Objects.equals(replacement, confirmation)) {
            throw new ValidationException("La confirmación de la contraseña no coincide.");
        }
        if (session == null) {
            throw new BusinessRuleException("Tu sesión terminó. Inicia sesión de nuevo.");
        }
        Account account = accounts.get(session.username());
        if (!account.hash().equals(hash(current))) {
            throw new BusinessRuleException("La contraseña actual no es correcta.");
        }
        accounts.put(account.username(), new Account(account.username(), account.displayName(), account.document(),
            account.role(), hash(replacement)));
    }

    @Override
    public void logout() {
        session = null;
    }

    @Override
    public Optional<AccessSession> current() {
        return Optional.ofNullable(session);
    }

    private static void requirePassword(String password, String field) throws ValidationException {
        Validator.requireText(password, field);
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("La " + field + " debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }
    }

    private static String key(String username) {
        return TextSearch.normalize(String.valueOf(username).trim());
    }

    private static String hash(String password) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(String.valueOf(password).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
