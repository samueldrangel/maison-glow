package com.maisonglow.dao;

import com.maisonglow.model.ProductCategory;

import java.nio.file.Path;

/**
 * Stores product categories in a text file, one per line with the fields
 * {@code id|name}.
 */
public class CategoryDaoText extends TextFileDao<ProductCategory> implements CategoryDao {

    /**
     * Creates the DAO over the given file.
     *
     * @param file the categories file, usually {@code data/categories.txt}
     */
    public CategoryDaoText(Path file) {
        super(file);
    }

    @Override
    protected String[] toFields(ProductCategory category) {
        return new String[] {String.valueOf(category.getId()), category.getName()};
    }

    @Override
    protected ProductCategory fromFields(String[] fields) {
        return new ProductCategory(Integer.parseInt(fields[0]), fields[1]);
    }

    @Override
    protected int idOf(ProductCategory category) {
        return category.getId();
    }

    @Override
    protected void assignId(ProductCategory category, int id) {
        category.setId(id);
    }
}
