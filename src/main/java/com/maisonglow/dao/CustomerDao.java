package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Customer;

import java.util.Optional;

/**
 * Data access for customers (table {@code customer}).
 */
public interface CustomerDao extends CrudDao<Customer> {

    /**
     * Looks up a customer by identity document.
     *
     * @param document the document to search for
     * @return the matching customer, or an empty {@link Optional}
     * @throws DataAccessException if the lookup fails
     */
    Optional<Customer> findByDocument(String document) throws DataAccessException;
}
