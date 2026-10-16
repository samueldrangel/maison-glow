package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Sale;

import java.time.LocalDate;
import java.util.List;

/**
 * Data access for sales (tables {@code sale} and {@code sale_line}): a sale
 * is always stored, loaded and removed together with its lines.
 */
public interface SaleDao extends CrudDao<Sale> {

    /**
     * Returns the purchases of a customer.
     *
     * @param customerId the customer's identifier
     * @return the sales ordered by sale time; empty if there are none
     * @throws DataAccessException if the sales cannot be read
     */
    List<Sale> findByCustomer(int customerId) throws DataAccessException;

    /**
     * Returns the sales made between two days, both included.
     *
     * @param from the first day
     * @param to   the last day
     * @return the sales ordered by sale time; empty if there are none
     * @throws DataAccessException if the sales cannot be read
     */
    List<Sale> findBetween(LocalDate from, LocalDate to) throws DataAccessException;
}
