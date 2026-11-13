package com.maisonglow.model;

import java.math.BigDecimal;
/** Data for product; business rules belong to services. */
public class Product implements Sellable {
    private int id;
    private String name;
    private ProductCategory category;
    private BigDecimal price;
    private int stock;
    private int minimumStock;
    private boolean active = true;

    public Product(int id, String name, ProductCategory category, BigDecimal price, int stock, int minimumStock) {
        this.id = id; this.name = name; this.category = category; this.price = price;
        this.stock = stock; this.minimumStock = minimumStock;
    }

    public Product(int id, String name, ProductCategory category, double price, int stock, int minimumStock) {
        this(id, name, category, BigDecimal.valueOf(price), stock, minimumStock);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public int getMinimumStock() { return minimumStock; }
    public void setMinimumStock(int minimumStock) { this.minimumStock = minimumStock; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof Product value && id > 0 && id == value.id;
    }

    // Stable while persistence assigns an id: safe for HashSet/HashMap keys.
    @Override
    public int hashCode() { return Product.class.hashCode(); }
    public void setPrice(double price) { setPrice(BigDecimal.valueOf(price)); }
    @Override public String toString() { return name + " (" + stock + " und., $" + price + ")"; }

}
