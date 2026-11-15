package com.maisonglow.view.product;

import com.maisonglow.model.Product;
import com.maisonglow.service.ProductService;

import java.util.Objects;

/**
 * What a product is, for whoever looks at the inventory: shown as a chip in the
 * table. An inactive product is first of all inactive, whatever its stock.
 */
public enum ProductStatus {
    INACTIVE("Inactivo", "chip-neutral"),
    OUT_OF_STOCK("Sin stock", "chip-warning"),
    LOW_STOCK("Stock bajo", "chip-gold"),
    AVAILABLE("Disponible", "chip-success");

    private final String label;
    private final String chipStyle;

    ProductStatus(String label, String chipStyle) {
        this.label = label;
        this.chipStyle = chipStyle;
    }

    /**
     * @param product a product with its current stock
     * @return its status
     */
    public static ProductStatus of(Product product) {
        Objects.requireNonNull(product, "product");
        if (!product.isActive()) {
            return INACTIVE;
        }
        if (product.getStock() <= 0) {
            return OUT_OF_STOCK;
        }
        return ProductService.isBelowMinimum(product) ? LOW_STOCK : AVAILABLE;
    }

    /** @return the text of the chip */
    public String label() {
        return label;
    }

    /** @return the style class of the chip in styles.css */
    public String chipStyle() {
        return chipStyle;
    }

    /** @return true when the product has to be restocked: it is out of stock or at its minimum */
    public boolean needsRestock() {
        return this == OUT_OF_STOCK || this == LOW_STOCK;
    }
}
