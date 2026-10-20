package com.maisonglow.service;

import com.maisonglow.exception.ValidationException;
import com.maisonglow.util.Validator;

/**
 * Groups the contact data entered for a person, so registration methods do
 * not need a long list of parameters.
 *
 * @param document the identity document
 * @param name     the full name
 * @param phone    the contact phone number
 * @param email    the contact email address
 */
public record ContactData(String document, String name, String phone, String email) {

    /**
     * Checks that every field is present and well formed.
     *
     * @throws ValidationException if a field is missing or malformed
     */
    public void validate() throws ValidationException {
        Validator.requireText(document, "documento");
        Validator.requireText(name, "nombre");
        Validator.requirePhone(phone);
        Validator.requireEmail(email);
    }
}
