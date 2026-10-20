package com.maisonglow.service;

import com.maisonglow.exception.ValidationException;
import com.maisonglow.util.Validator;

/**
 * Groups the data entered for a product, so the product methods do not
 * need a long list of parameters. The stock is not part of it: it only
 * changes through registration and stock adjustments.
 *
 * @param name         the commercial name
 * @param categoryId   the id of an existing category
 * @param price        the sale price, greater than zero
 * @param minimumStock the stock level that triggers a low-stock alert, zero or more
 */
public record ProductData(String name, int categoryId, double price, int minimumStock) {

    /**
     * Checks that the name is present, the price is positive and the minimum stock is not negative.
     *
     * @throws ValidationException if a field is missing or out of range
     */
    public void validate() throws ValidationException {
        Validator.requireText(name, "nombre");
        Validator.requirePositive(price, "precio");
        Validator.requireNonNegative(minimumStock, "stock mínimo");
    }
}
