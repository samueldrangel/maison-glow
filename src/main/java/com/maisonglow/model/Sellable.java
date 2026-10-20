package com.maisonglow.model;

import com.maisonglow.exception.BusinessRuleException;

/**
 * Represents anything the business can sell, such as a physical product or a
 * beauty service. Sales treat every sellable item uniformly through this
 * contract, so new kinds of items can be added without modifying them.
 */
public interface Sellable {

    /**
     * Returns the unique identifier of this item.
     *
     * @return the item id
     */
    int getId();

    /**
     * Returns the display name of this item, as shown on invoices.
     *
     * @return the item name
     */
    String getName();

    /**
     * Returns the current unit price of this item.
     *
     * @return the unit price
     */
    double getPrice();

    /**
     * Indicates whether there are enough units to sell. Items without stock,
     * such as treatments, can always be sold.
     *
     * @param quantity the units requested
     * @return {@code true} if the item can be sold in that quantity
     */
    default boolean hasStock(int quantity) {
        return true;
    }

    /**
     * Removes the units sold from the stock. Items without stock do nothing.
     *
     * @param quantity the units sold; must be greater than zero
     * @throws BusinessRuleException if the quantity is not positive or there are not enough units
     */
    default void deductStock(int quantity) throws BusinessRuleException {
    }

    /**
     * Gives back units to the stock, for example when a sale is undone. Items
     * without stock do nothing.
     *
     * @param quantity the units to give back; must be greater than zero
     * @throws BusinessRuleException if the quantity is not positive
     */
    default void addStock(int quantity) throws BusinessRuleException {
    }
}
