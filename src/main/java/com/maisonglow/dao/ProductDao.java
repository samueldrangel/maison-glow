package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Product;

import java.util.Optional;

/**
 * Data access for products (table {@code product}).
 */
public interface ProductDao extends CrudDao<Product> {

    /**
     * Looks up a product by name, ignoring case.
     *
     * @param name the name to search for
     * @return the matching product, or an empty {@link Optional}
     * @throws DataAccessException if the lookup fails
     */
    Optional<Product> findByName(String name) throws DataAccessException;
}
