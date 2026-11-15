package com.maisonglow.view.product;

import com.maisonglow.model.Product;
import com.maisonglow.util.TextSearch;

/**
 * What the products screen is showing: the words typed in the search box, the
 * category chosen with the chips and whether only the products to restock are
 * wanted. It is a value: changing one thing gives a new filter.
 *
 * @param text        the search text; empty means no search
 * @param categoryId  the id of the chosen category, or null for all of them
 * @param onlyRestock true to show only the products that are out of stock or at their minimum
 */
public record ProductFilter(String text, Integer categoryId, boolean onlyRestock) {

    /** @return a filter that lets every product through */
    public static ProductFilter all() {
        return new ProductFilter("", null, false);
    }

    /** @return the same filter with another search text */
    public ProductFilter withText(String newText) {
        return new ProductFilter(newText == null ? "" : newText, categoryId, onlyRestock);
    }

    /** @return the same filter for another category; null shows every category */
    public ProductFilter withCategory(Integer newCategoryId) {
        return new ProductFilter(text, newCategoryId, onlyRestock);
    }

    /** @return the same filter with the restock switch changed */
    public ProductFilter withOnlyRestock(boolean restock) {
        return new ProductFilter(text, categoryId, restock);
    }

    /**
     * @param product the product to check
     * @return true if the product passes the search, the category and the restock switch
     */
    public boolean matches(Product product) {
        boolean byText = TextSearch.contains(product.getName(), text);
        boolean byCategory = categoryId == null || product.getCategory().getId() == categoryId;
        boolean byStock = !onlyRestock || ProductStatus.of(product).needsRestock();
        return byText && byCategory && byStock;
    }
}
