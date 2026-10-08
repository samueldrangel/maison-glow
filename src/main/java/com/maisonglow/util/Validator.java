package com.maisonglow.util;

import com.maisonglow.exception.ValidationException;

import java.util.regex.Pattern;

/**
 * Provides reusable checks for user input. Every method throws a
 * {@link ValidationException} with a Spanish message that can be shown
 * directly to the user.
 */
public final class Validator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?\\d{7,12}$");

    private Validator() {
    }

    /**
     * Checks that a text value is neither {@code null} nor blank.
     *
     * @param value     the text to check
     * @param fieldName the field name used in the error message
     * @throws ValidationException if the text is {@code null} or blank
     */
    public static void requireText(String value, String fieldName) throws ValidationException {
        if (value == null || value.isBlank()) {
            throw new ValidationException("El campo " + fieldName + " es obligatorio.");
        }
    }

    /**
     * Checks that a text value has a valid email format.
     *
     * @param email the email address to check
     * @throws ValidationException if the email is blank or malformed
     */
    public static void requireEmail(String email) throws ValidationException {
        requireText(email, "correo");
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException("El correo electrónico no tiene un formato válido.");
        }
    }

    /**
     * Checks that a text value is a phone number of 7 to 12 digits,
     * optionally preceded by a plus sign.
     *
     * @param phone the phone number to check
     * @throws ValidationException if the phone is blank or malformed
     */
    public static void requirePhone(String phone) throws ValidationException {
        requireText(phone, "teléfono");
        if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new ValidationException("El teléfono debe tener entre 7 y 12 dígitos.");
        }
    }

    /**
     * Checks that a number is strictly greater than zero.
     *
     * @param value     the number to check
     * @param fieldName the field name used in the error message
     * @throws ValidationException if the number is zero or negative
     */
    public static void requirePositive(double value, String fieldName) throws ValidationException {
        if (value <= 0) {
            throw new ValidationException("El campo " + fieldName + " debe ser mayor que cero.");
        }
    }

    /**
     * Checks that a whole number is zero or greater.
     *
     * @param value     the number to check
     * @param fieldName the field name used in the error message
     * @throws ValidationException if the number is negative
     */
    public static void requireNonNegative(int value, String fieldName) throws ValidationException {
        if (value < 0) {
            throw new ValidationException("El campo " + fieldName + " no puede ser negativo.");
        }
    }
}
