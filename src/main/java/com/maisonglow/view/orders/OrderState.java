package com.maisonglow.view.orders;

/**
 * The state of an order as the administrator sees it, with the word shown in its chip and the style of the chip. An order
 * that is waiting for payment is never shown as paid: the stock is only reserved, not sold, until the payment is
 * confirmed. Cancelled and expired orders have released their reservation.
 */
public enum OrderState {

    PENDING_PAYMENT("Pendiente de pago", "chip-gold"),
    PAID("Pagado", "chip-success"),
    CANCELLED("Cancelado", "chip-neutral"),
    EXPIRED("Vencido", "chip-warning");

    private final String label;
    private final String chipStyle;

    OrderState(String label, String chipStyle) {
        this.label = label;
        this.chipStyle = chipStyle;
    }

    /** @return the word shown in the chip */
    public String label() {
        return label;
    }

    /** @return the style class of the chip */
    public String chipStyle() {
        return chipStyle;
    }

    /** @return true while the order holds stock and can still be paid or cancelled */
    public boolean isOpen() {
        return this == PENDING_PAYMENT;
    }

    /** @return true once the order became a sale, so there is a receipt to show */
    public boolean hasReceipt() {
        return this == PAID;
    }
}
