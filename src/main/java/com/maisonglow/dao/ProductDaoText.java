package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Product;
import com.maisonglow.model.ProductCategory;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Stores products in a text file with the columns of the {@code product}
 * table: {@code id|name|category_id|price|stock|minimum_stock|active}. The
 * category is stored by id and rebuilt with the {@link CategoryDao}.
 */
public class ProductDaoText extends TextFileDao<Product> implements ProductDao {

    private static final int ID = 0;
    private static final int NAME = 1;
    private static final int CATEGORY_ID = 2;
    private static final int PRICE = 3;
    private static final int STOCK = 4;
    private static final int MINIMUM_STOCK = 5;
    private static final int ACTIVE = 6;
    private static final int FIELD_COUNT = 7;
    private static final String ACTIVE_FLAG = "1";
    private static final String INACTIVE_FLAG = "0";

    private final CategoryDao categoryDao;

    /**
     * Creates the DAO over the given file.
     *
     * @param file        the products file, usually {@code data/products.txt}
     * @param categoryDao where to find the category of each product
     */
    public ProductDaoText(Path file, CategoryDao categoryDao) {
        super(file);
        this.categoryDao = categoryDao;
    }

    @Override
    public Optional<Product> findByName(String name) throws DataAccessException {
        return findAll().stream()
            .filter(product -> product.getName().equalsIgnoreCase(name))
            .findFirst();
    }

    @Override
    protected String[] toFields(Product product) {
        String[] fields = new String[FIELD_COUNT];
        fields[ID] = String.valueOf(product.getId());
        fields[NAME] = product.getName();
        fields[CATEGORY_ID] = String.valueOf(product.getCategory().getId());
        fields[PRICE] = String.valueOf(product.getPrice());
        fields[STOCK] = String.valueOf(product.getStock());
        fields[MINIMUM_STOCK] = String.valueOf(product.getMinimumStock());
        fields[ACTIVE] = product.isActive() ? ACTIVE_FLAG : INACTIVE_FLAG;
        return fields;
    }

    @Override
    protected Product fromFields(String[] fields) throws DataAccessException {
        if (fields.length != FIELD_COUNT) {
            throw new DataAccessException("Se esperaban " + FIELD_COUNT + " campos y hay " + fields.length + ".");
        }
        Product product = new Product(Integer.parseInt(fields[ID]), fields[NAME], findCategory(fields[CATEGORY_ID]),
            Double.parseDouble(fields[PRICE]), Integer.parseInt(fields[STOCK]),
            Integer.parseInt(fields[MINIMUM_STOCK]));
        if (INACTIVE_FLAG.equals(fields[ACTIVE])) {
            product.deactivate();
        }
        return product;
    }

    @Override
    protected int getId(Product product) {
        return product.getId();
    }

    @Override
    protected void setId(Product product, int id) {
        product.setId(id);
    }

    private ProductCategory findCategory(String idField) throws DataAccessException {
        int id = Integer.parseInt(idField);
        return categoryDao.findById(id).orElseThrow(() ->
            new DataAccessException("El producto referencia una categoría con id " + id + " que no existe."));
    }
}
