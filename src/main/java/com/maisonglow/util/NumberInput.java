package com.maisonglow.util;

import com.maisonglow.exception.ValidationException;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Reads the numbers a user types in a form. Colombian pesos are written with
 * dots between thousands ({@code 28.000}) and a comma for decimals, but people
 * also type {@code 28000} or {@code $ 28.000}, so all of them are accepted. The
 * messages name the field, so {@code FieldValidator} can show them under it.
 */
public final class NumberInput {

    private static final Pattern THOUSANDS = Pattern.compile("\\d{1,3}(\\.\\d{3})+");
    private static final Pattern DECIMAL = Pattern.compile("\\d+(\\.\\d+)?");

    private NumberInput() {
    }

    /**
     * Reads an amount of money.
     *
     * @param text      what the user typed, for example {@code 28000}, {@code 28.000} or {@code $ 28.000,50}
     * @param fieldName the name of the field, used in the message
     * @return the amount
     * @throws ValidationException if the text is empty or is not a number
     */
    public static BigDecimal money(String text, String fieldName) throws ValidationException {
        Validator.requireText(text, fieldName);
        String clean = text.replace("$", "").replace(" ", "").trim();
        if (clean.contains(",")) {
            clean = clean.replace(".", "").replace(',', '.');
        } else if (THOUSANDS.matcher(clean).matches()) {
            clean = clean.replace(".", "");
        }
        boolean negative = clean.startsWith("-");
        String digits = negative || clean.startsWith("+") ? clean.substring(1) : clean;
        if (!DECIMAL.matcher(digits).matches()) {
            throw new ValidationException("El campo " + fieldName + " debe ser un número, por ejemplo 28000.");
        }
        BigDecimal amount = new BigDecimal(digits);
        return negative ? amount.negate() : amount;
    }

    /**
     * Reads a whole number, such as a quantity of units.
     *
     * @param text      what the user typed; a sign is allowed
     * @param fieldName the name of the field, used in the message
     * @return the number
     * @throws ValidationException if the text is empty, is not a whole number or does not fit
     */
    public static int wholeNumber(String text, String fieldName) throws ValidationException {
        Validator.requireText(text, fieldName);
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("El campo " + fieldName + " debe ser un número entero.", e);
        }
    }
}
