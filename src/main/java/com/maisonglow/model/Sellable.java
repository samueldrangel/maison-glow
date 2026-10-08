package com.maisonglow.model;

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
}
