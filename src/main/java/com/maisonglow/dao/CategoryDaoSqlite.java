package com.maisonglow.dao;

import com.maisonglow.model.ProductCategory;



/** SQLite persistence for the product_category table. */
public class CategoryDaoSqlite extends SqliteCrudDao<ProductCategory> implements CategoryDao {


    public CategoryDaoSqlite(SqliteDatabase database) {
        super(database, "product_category", "id", "name");
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
    protected int getId(ProductCategory category) {
        return category.getId();
    }

    @Override
    protected void setId(ProductCategory category, int id) {
        category.setId(id);
    }
}
