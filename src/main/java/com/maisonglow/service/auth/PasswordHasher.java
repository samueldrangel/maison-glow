package com.maisonglow.service.auth;

import com.maisonglow.exception.ValidationException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** JDK-only PBKDF2-HMAC-SHA256, with random per-password salt and versioned parameters. */
public final class PasswordHasher {
    private static final int ITERATIONS = 600_000;
    private final SecureRandom random = new SecureRandom();

    /** Does not modify the caller's array; the caller must clear it after use. */
    public String hash(char[] password) throws ValidationException {
        validate(password);
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return "pbkdf2-sha256$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
            + "$" + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }

    /** Malformed/unsupported hashes fail closed without exposing the stored credential. */
    public boolean matches(char[] password, String encoded) {
        if (password == null || password.length == 0 || password.length > 128
            || encoded == null || encoded.length() > 256) return false;
        try {
            String[] parts = encoded.split("\\$", -1);
            if (parts.length != 4 || !parts[0].equals("pbkdf2-sha256")) return false;
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < ITERATIONS || iterations > 2_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            return salt.length == 16 && expected.length == 32
                && MessageDigest.isEqual(expected, derive(password, salt, iterations));
        } catch (IllegalArgumentException e) { return false; }
    }

    /** Passwords are not trimmed, preserving exactly what the user entered. */
    public void validate(char[] password) throws ValidationException {
        if (password == null || password.length < 12 || password.length > 128)
            throw new ValidationException("La contraseña debe tener entre 12 y 128 caracteres.");
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (GeneralSecurityException e) { throw new IllegalStateException("No se pudo proteger la contraseña.", e); }
        finally { spec.clearPassword(); }
    }
}
