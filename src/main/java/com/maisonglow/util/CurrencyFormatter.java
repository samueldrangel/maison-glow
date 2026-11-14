package com.maisonglow.util;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

/**
 * The one place that writes an amount of money as the user reads it: Colombian
 * pesos, thousands separated by dots and no decimals ({@code $ 28.000}). Views
 * and reports call this instead of formatting amounts on their own.
 */
public final class CurrencyFormatter {

    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");
    private static final String PATTERN = "$ %,.0f";

    private CurrencyFormatter() {
    }

    /**
     * Formats an amount, rounding half up to whole pesos.
     *
     * @param amount the amount to write
     * @return for example {@code $ 28.000}
     */
    public static String format(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount");
        return String.format(COLOMBIA, PATTERN, amount);
    }

    /**
     * Formats an amount held as a {@code double}.
     *
     * @param amount the amount to write
     * @return for example {@code $ 1.250.000}
     */
    public static String format(double amount) {
        return String.format(COLOMBIA, PATTERN, amount);
    }
}
