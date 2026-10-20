package com.maisonglow.service;

import com.maisonglow.exception.ValidationException;
import com.maisonglow.util.Validator;

/**
 * Groups the data entered for a treatment, so the catalog methods do not
 * need a long list of parameters.
 *
 * @param name            the treatment name
 * @param description     a short description; may be empty or {@code null}
 * @param price           the price, greater than zero
 * @param durationMinutes the duration, greater than zero
 */
public record TreatmentData(String name, String description, double price, int durationMinutes) {

    /**
     * Checks that the name is present and that price and duration are positive.
     *
     * @throws ValidationException if a field is missing or out of range
     */
    public void validate() throws ValidationException {
        Validator.requireText(name, "nombre");
        Validator.requirePositive(price, "precio");
        Validator.requirePositive(durationMinutes, "duración");
    }

    /**
     * Returns the description without surrounding spaces, or an empty text if there is none.
     *
     * @return the cleaned description
     */
    public String cleanDescription() {
        return description == null ? "" : description.trim();
    }
}
