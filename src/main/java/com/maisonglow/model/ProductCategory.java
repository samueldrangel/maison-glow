package com.maisonglow.model;

import java.util.Objects;

/**
 * Groups products by type, such as hair care or makeup, to organize the
 * inventory and the reports.
 */
public class ProductCategory {

    private int id;
    private String name;

    /**
     * Creates a category.
     *
     * @param id   the identifier; 0 if the category is not stored yet
     * @param name the category name
     */
    public ProductCategory(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Returns the identifier.
     *
     * @return the identifier
     */
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

    /**
     * Returns the category name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Changes the category name.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProductCategory category)) {
            return false;
        }
        return id == category.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name;
    }
}
