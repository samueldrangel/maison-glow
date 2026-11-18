package com.maisonglow.view.shop;

/**
 * How the shop tells a customer whether a product can be had: a word and the style of its chip. The customer never sees
 * the exact stock or the minimum of the business (that is an internal figure); only that it is available, that few units are
 * left, or that it is sold out. The units counted are the ones available after the reservations of other orders.
 */
public enum ProductAvailability {

    AVAILABLE("Disponible", "chip-success"),
    FEW("Pocas unidades", "chip-gold"),
    SOLD_OUT("Agotado", "chip-neutral");

    /** Up to this many units available, the shop says that few are left. */
    public static final int FEW_UNITS = 3;

    private final String label;
    private final String chipStyle;

    ProductAvailability(String label, String chipStyle) {
        this.label = label;
        this.chipStyle = chipStyle;
    }

    /**
     * @param availableStock units that can still be ordered
     * @return how that is said to the customer
     */
    public static ProductAvailability of(int availableStock) {
        if (availableStock <= 0) {
            return SOLD_OUT;
        }
        return availableStock <= FEW_UNITS ? FEW : AVAILABLE;
    }

    /** @return the word shown in the chip */
    public String label() {
        return label;
    }

    /** @return the style class of the chip */
    public String chipStyle() {
        return chipStyle;
    }

    /** @return true if the customer can still order it */
    public boolean canBeOrdered() {
        return this != SOLD_OUT;
    }
}
