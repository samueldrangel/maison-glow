package com.maisonglow.view.shop;

import com.maisonglow.model.Commerce.CatalogProduct;
import com.maisonglow.util.TextSearch;

import java.util.Objects;

/**
 * What the customer chose to see in the shop: a category (all when null) and a text that is looked for in the name of the
 * product, ignoring case and accents.
 *
 * @param category the category chosen, or null for every category
 * @param text     what was typed; empty matches everything
 */
public record ShopFilter(String category, String text) {

    public ShopFilter {
        text = text == null ? "" : text.trim();
    }

    /** @return the filter the shop opens with: everything */
    public static ShopFilter all() {
        return new ShopFilter(null, "");
    }

    /**
     * @param newCategory the chip pressed, null for all
     * @return the same text over another category
     */
    public ShopFilter withCategory(String newCategory) {
        return new ShopFilter(newCategory, text);
    }

    /**
     * @param newText what was typed
     * @return the same category with another text
     */
    public ShopFilter withText(String newText) {
        return new ShopFilter(category, newText);
    }

    /**
     * @param product a published product
     * @return true if it is in the category and its name matches the text
     */
    public boolean matches(CatalogProduct product) {
        Objects.requireNonNull(product, "product");
        return (category == null || category.equals(product.category()))
            && (text.isEmpty() || TextSearch.contains(product.name(), text));
    }
}
