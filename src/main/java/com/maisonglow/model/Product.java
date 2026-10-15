package com.maisonglow.model;

import com.maisonglow.exception.BusinessRuleException;

import java.util.Objects;

/**
 * Represents a physical item sold by the business. Protects the consistency
 * of its own stock: units can never go below zero.
 */
public class Product implements Sellable {

    private int id;
    private String name;
    private ProductCategory category;
    private double price;
    private int stock;
    private int minimumStock;
    private boolean active;

    /**
     * Creates an active product.
     *
     * @param id           the identifier; 0 if the product is not stored yet
     * @param name         the commercial name
     * @param category     the category it belongs to
     * @param price        the sale price
     * @param stock        the units available
     * @param minimumStock the stock level that triggers a low-stock alert
     */
    public Product(int id, String name, ProductCategory category, double price, int stock, int minimumStock) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.minimumStock = minimumStock;
        this.active = true;
    }

    /**
     * Indicates whether there are enough units for a sale.
     *
     * @param quantity the units requested
     * @return {@code true} if the stock covers the quantity
     */
    public boolean hasStock(int quantity) {
        return stock >= quantity;
    }

    /**
     * Removes units from the stock, for example after a sale.
     *
     * @param quantity the units to remove; must be greater than zero
     * @throws BusinessRuleException if the quantity is not positive or there are not enough units
     */
    public void deductStock(int quantity) throws BusinessRuleException {
        requirePositive(quantity);
        if (!hasStock(quantity)) {
            throw new BusinessRuleException("Stock insuficiente de " + name + ": hay " + stock
                + " y se pidieron " + quantity + ".");
        }
        stock -= quantity;
    }

    /**
     * Adds units to the stock, for example after a restock.
     *
     * @param quantity the units to add; must be greater than zero
     * @throws BusinessRuleException if the quantity is not positive
     */
    public void addStock(int quantity) throws BusinessRuleException {
        requirePositive(quantity);
        stock += quantity;
    }

    private static void requirePositive(int quantity) throws BusinessRuleException {
        if (quantity <= 0) {
            throw new BusinessRuleException("La cantidad debe ser mayor que cero.");
        }
    }

    /**
     * Indicates whether the stock reached the alert level (RF-19).
     *
     * @return {@code true} if the stock is at or below the minimum
     */
    public boolean isBelowMinimum() {
        return stock <= minimumStock;
    }

    @Override
    public int getId() {
        return id;
    }

    /**
     * Assigns the identifier given by the data store.
     *
     * @param id the new identifier
     */
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Changes the commercial name.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the category of the product.
     *
     * @return the category
     */
    public ProductCategory getCategory() {
        return category;
    }

    /**
     * Moves the product to another category.
     *
     * @param category the new category
     */
    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    @Override
    public double getPrice() {
        return price;
    }

    /**
     * Changes the sale price.
     *
     * @param price the new price
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * Returns the units available.
     *
     * @return the stock
     */
    public int getStock() {
        return stock;
    }

    /**
     * Returns the stock level that triggers a low-stock alert.
     *
     * @return the minimum stock
     */
    public int getMinimumStock() {
        return minimumStock;
    }

    /**
     * Changes the stock level that triggers a low-stock alert.
     *
     * @param minimumStock the new minimum stock
     */
    public void setMinimumStock(int minimumStock) {
        this.minimumStock = minimumStock;
    }

    /**
     * Indicates whether the product is available for sale.
     *
     * @return {@code true} if the product is active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Makes the product available for sale.
     */
    public void activate() {
        active = true;
    }

    /**
     * Withdraws the product from sale without deleting its history.
     */
    public void deactivate() {
        active = false;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Product product)) {
            return false;
        }
        return id == product.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name + " (" + stock + " und., $" + price + ")";
    }
}
