package com.maisonglow.model;

import java.math.BigDecimal;
/** Data for saleline; business rules belong to services. */
public class SaleLine {
    private final Sellable item;
    private final BigDecimal unitPrice;
    private int quantity;

    public SaleLine(Sellable item, int quantity, BigDecimal unitPrice) {
        this.item = java.util.Objects.requireNonNull(item, "item");
        this.quantity = quantity; this.unitPrice = java.util.Objects.requireNonNull(unitPrice, "unitPrice");
    }
    public static SaleLine restore(Sellable item, int quantity, BigDecimal unitPrice) {
        return new SaleLine(item, quantity, unitPrice);
    }
    public static SaleLine restore(Sellable item, int quantity, double unitPrice) {
        return restore(item, quantity, BigDecimal.valueOf(unitPrice));
    }

    public Sellable getItem() { return item; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    @Override public String toString() { return quantity + " x " + item.getName() + " a $" + unitPrice; }

}
