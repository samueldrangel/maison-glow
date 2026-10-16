package com.maisonglow.model;

import com.maisonglow.exception.BusinessRuleException;

import java.util.Objects;

/**
 * One line of a sale: a product or a treatment, how many units were sold and
 * the unit price at the moment of the sale. Later changes to the catalog
 * price do not change lines already sold.
 *
 * <p>New lines are created only by {@link Sale#addLine(Sellable, int)}.</p>
 */
public class SaleLine {

    private final Sellable item;
    private final double unitPrice;
    private int quantity;

    SaleLine(Sellable item, int quantity) throws BusinessRuleException {
        this(item, quantity, Objects.requireNonNull(item, "item").getPrice());
    }

    private SaleLine(Sellable item, int quantity, double unitPrice) throws BusinessRuleException {
        this.item = Objects.requireNonNull(item, "item");
        requirePositiveQuantity(quantity);
        if (!Double.isFinite(unitPrice) || unitPrice <= 0) {
            throw new BusinessRuleException("El precio de " + item.getName() + " debe ser mayor que cero.");
        }
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    /**
     * Rebuilds a line that was already stored, with the price it was sold at.
     * Meant for the data access layer.
     *
     * @param item      the product or treatment sold
     * @param quantity  the units sold
     * @param unitPrice the unit price at the moment of the sale
     * @return the rebuilt line
     * @throws BusinessRuleException if the quantity or the price is not positive
     */
    public static SaleLine restore(Sellable item, int quantity, double unitPrice) throws BusinessRuleException {
        return new SaleLine(item, quantity, unitPrice);
    }

    /**
     * Calculates the amount of this line.
     *
     * @return the quantity multiplied by the unit price
     */
    public double calculateSubtotal() {
        return quantity * unitPrice;
    }

    /**
     * Indicates whether this line sells the given item.
     *
     * @param other the item to compare with
     * @return {@code true} if it is the same product or treatment
     */
    public boolean sells(Sellable other) {
        return item.equals(other);
    }

    void addQuantity(int extra) throws BusinessRuleException {
        requirePositiveQuantity(extra);
        quantity += extra;
    }

    private static void requirePositiveQuantity(int quantity) throws BusinessRuleException {
        if (quantity <= 0) {
            throw new BusinessRuleException("La cantidad debe ser mayor que cero.");
        }
    }

    /**
     * Returns the product or treatment sold.
     *
     * @return the item
     */
    public Sellable getItem() {
        return item;
    }

    /**
     * Returns the units sold.
     *
     * @return the quantity
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Returns the unit price at the moment of the sale.
     *
     * @return the unit price
     */
    public double getUnitPrice() {
        return unitPrice;
    }

    @Override
    public String toString() {
        return quantity + " x " + item.getName() + " a $" + unitPrice;
    }
}
