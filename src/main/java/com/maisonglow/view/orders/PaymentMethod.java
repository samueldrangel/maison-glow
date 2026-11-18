package com.maisonglow.view.orders;

/** How the customer paid at the shop (table {@code payment}); the order is paid and picked up at the business. */
public enum PaymentMethod {

    CASH("Efectivo"),
    CARD("Tarjeta"),
    TRANSFER("Transferencia");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    /** @return the word shown in the list of methods */
    public String label() {
        return label;
    }
}
