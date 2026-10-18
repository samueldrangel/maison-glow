package com.maisonglow.service;

import com.maisonglow.dao.CategoryDao;
import com.maisonglow.dao.ProductDao;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Product;
import com.maisonglow.model.ProductCategory;
import com.maisonglow.util.Validator;

import java.util.List;

/**
 * Manages the product inventory (RF-03, 04, 05 and 19): registration with a
 * unique name and an existing category, updates, stock adjustments and the
 * low-stock list.
 */
public class ProductService {

    private final ProductDao productDao;
    private final CategoryDao categoryDao;

    /**
     * Creates the service.
     *
     * @param productDao  the products storage
     * @param categoryDao the categories storage
     */
    public ProductService(ProductDao productDao, CategoryDao categoryDao) {
        this.productDao = productDao;
        this.categoryDao = categoryDao;
    }

    /**
     * Registers a new product.
     *
     * @param data         the name (unique), category, price and minimum stock
     * @param initialStock the units available at registration, zero or more
     * @return the stored product, with its id
     * @throws ValidationException   if a field is missing or out of range
     * @throws BusinessRuleException if the name is taken or the category does not exist
     * @throws DataAccessException   if the product cannot be stored
     */
    public Product register(ProductData data, int initialStock)
        throws ValidationException, BusinessRuleException, DataAccessException {
        data.validate();
        Validator.requireNonNegative(initialStock, "stock");
        String name = data.name().trim();
        requireUniqueName(name, 0);
        ProductCategory category = findCategory(data.categoryId());
        return productDao.create(new Product(0, name, category, data.price(), initialStock, data.minimumStock()));
    }

    /**
     * Changes the data of an existing product. The stock is changed with
     * {@link #adjustStock(int, int)}.
     *
     * @param id   the product id
     * @param data the new name (unique), category, price and minimum stock
     * @return the updated product
     * @throws ValidationException   if a field is missing or out of range
     * @throws BusinessRuleException if the product or the category does not exist, or the name is taken
     * @throws DataAccessException   if the product cannot be stored
     */
    public Product update(int id, ProductData data)
        throws ValidationException, BusinessRuleException, DataAccessException {
        data.validate();
        Product product = findById(id);
        String name = data.name().trim();
        requireUniqueName(name, id);
        product.setCategory(findCategory(data.categoryId()));
        product.setName(name);
        product.setPrice(data.price());
        product.setMinimumStock(data.minimumStock());
        productDao.update(product);
        return product;
    }

    /**
     * Adds or removes units, for example after a restock or a count.
     *
     * @param id    the product id
     * @param delta the units to add (positive) or remove (negative); never zero
     * @return the updated product
     * @throws ValidationException   if the delta is zero
     * @throws BusinessRuleException if the product does not exist or there are not enough units
     * @throws DataAccessException   if the product cannot be stored
     */
    public Product adjustStock(int id, int delta)
        throws ValidationException, BusinessRuleException, DataAccessException {
        if (delta == 0) {
            throw new ValidationException("El ajuste de stock no puede ser cero.");
        }
        Product product = findById(id);
        if (delta > 0) {
            product.addStock(delta);
        } else {
            product.deductStock(-delta);
        }
        productDao.update(product);
        return product;
    }

    /**
     * Returns the active products whose stock is at or below their minimum (RF-19).
     *
     * @return the products to restock
     * @throws DataAccessException if the products cannot be read
     */
    public List<Product> listLowStock() throws DataAccessException {
        return productDao.findAll().stream()
            .filter(Product::isActive)
            .filter(Product::isBelowMinimum)
            .toList();
    }

    /**
     * Returns the product with the given id.
     *
     * @param id the product id
     * @return the product
     * @throws BusinessRuleException if no product has that id
     * @throws DataAccessException   if the lookup fails
     */
    public Product findById(int id) throws BusinessRuleException, DataAccessException {
        return productDao.findById(id)
            .orElseThrow(() -> new BusinessRuleException("No existe el producto con id " + id + "."));
    }

    /**
     * Returns every product.
     *
     * @return the products
     * @throws DataAccessException if the products cannot be read
     */
    public List<Product> listAll() throws DataAccessException {
        return productDao.findAll();
    }

    /**
     * Returns the products currently for sale.
     *
     * @return the active products
     * @throws DataAccessException if the products cannot be read
     */
    public List<Product> listActive() throws DataAccessException {
        return productDao.findAll().stream().filter(Product::isActive).toList();
    }

    /**
     * Stops selling a product without deleting its history.
     *
     * @param id the product id
     * @throws BusinessRuleException if the product does not exist
     * @throws DataAccessException   if the product cannot be stored
     */
    public void deactivate(int id) throws BusinessRuleException, DataAccessException {
        Product product = findById(id);
        product.deactivate();
        productDao.update(product);
    }

    /**
     * Sells a product again.
     *
     * @param id the product id
     * @throws BusinessRuleException if the product does not exist
     * @throws DataAccessException   if the product cannot be stored
     */
    public void activate(int id) throws BusinessRuleException, DataAccessException {
        Product product = findById(id);
        product.activate();
        productDao.update(product);
    }

    /**
     * Returns the product categories, to choose one when registering.
     *
     * @return the categories
     * @throws DataAccessException if the categories cannot be read
     */
    public List<ProductCategory> listCategories() throws DataAccessException {
        return categoryDao.findAll();
    }

    private void requireUniqueName(String name, int ownId) throws BusinessRuleException, DataAccessException {
        boolean taken = productDao.findByName(name)
            .filter(existing -> existing.getId() != ownId)
            .isPresent();
        if (taken) {
            throw new BusinessRuleException("Ya existe un producto llamado " + name + ".");
        }
    }

    private ProductCategory findCategory(int categoryId) throws BusinessRuleException, DataAccessException {
        return categoryDao.findById(categoryId)
            .orElseThrow(() -> new BusinessRuleException("No existe la categoría con id " + categoryId + "."));
    }
}
