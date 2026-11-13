package com.maisonglow.ai;

import com.maisonglow.exception.ValidationException;
import com.maisonglow.util.Validator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

/** Shared conversion of untrusted tool parameters into service arguments. */
final class ToolArguments {
    private ToolArguments() { }

    static String text(Map<String, String> parameters, String name) throws ValidationException {
        Validator.requirePresent(parameters, "parámetros");
        String value = parameters.get(name);
        Validator.requireText(value, name);
        return value.trim();
    }

    static int id(Map<String, String> parameters, String name) throws ValidationException {
        try {
            int id = Integer.parseInt(text(parameters, name));
            if (id > 0) return id;
        } catch (NumberFormatException ignored) {
            // Report the same validation error for malformed and non-positive ids.
        }
        throw new ValidationException(name + " debe ser un entero positivo.");
    }

    static LocalDate date(Map<String, String> parameters, String name) throws ValidationException {
        try {
            return LocalDate.parse(text(parameters, name));
        } catch (DateTimeParseException e) {
            throw new ValidationException(name + " debe tener formato yyyy-MM-dd.", e);
        }
    }

    static LocalDateTime dateTime(Map<String, String> parameters, String name) throws ValidationException {
        try {
            return LocalDateTime.parse(text(parameters, name));
        } catch (DateTimeParseException e) {
            throw new ValidationException(name + " debe tener formato yyyy-MM-ddTHH:mm.", e);
        }
    }

    static BigDecimal deposit(Map<String, String> parameters) throws ValidationException {
        if (!parameters.containsKey("deposit")) return BigDecimal.ZERO;
        try {
            BigDecimal value = new BigDecimal(text(parameters, "deposit"));
            if (value.signum() >= 0) return value;
        } catch (NumberFormatException ignored) {
            // Reject non-numeric values without leaking a parsing exception.
        }
        throw new ValidationException("El anticipo debe ser un número mayor o igual a cero.");
    }
}
