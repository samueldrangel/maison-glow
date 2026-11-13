package com.maisonglow.model;
import java.math.BigDecimal;
/** Catalog data shared by products and treatments; inventory is managed separately. */
public interface Sellable {
    int getId();
    String getName();
    BigDecimal getPrice();
}
