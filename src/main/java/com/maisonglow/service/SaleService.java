package com.maisonglow.service;

import com.maisonglow.dao.ProductDao;
import com.maisonglow.dao.SaleDao;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Product;
import com.maisonglow.model.Sale;
import com.maisonglow.model.SaleLine;
import com.maisonglow.util.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Registers sales and consults the purchases of a customer (RF-14 to RF-17).
 * A sale is all or nothing: every line is checked before anything changes,
 * and if storing fails the stock already deducted is given back.
 */
public class SaleService {

    private static final int NOT_STORED_ID = 0;

    private final SaleDao saleDao;
    private final ProductDao productDao;

    /**
     * Creates the service.
     *
     * @param saleDao    where sales are stored
     * @param productDao where products are stored, to save their new stock
     */
    public SaleService(SaleDao saleDao, ProductDao productDao) {
        this.saleDao = Objects.requireNonNull(saleDao, "saleDao");
        this.productDao = Objects.requireNonNull(productDao, "productDao");
    }

    /**
     * Registers a sale: checks it has lines and enough stock for all of them,
     * deducts the stock of the products sold and stores the sale. If the
     * sale cannot be registered, no stock is deducted.
     *
     * @param sale the sale to register, with its lines already added
     * @return the stored sale, with its id assigned
     * @throws ValidationException   if the sale is missing or has no lines
     * @throws BusinessRuleException if the sale was already registered or the
     *                               stock of a product does not cover its line
     * @throws DataAccessException   if the sale or the products cannot be stored
     */
    public Sale register(Sale sale) throws ValidationException, BusinessRuleException, DataAccessException {
        Validator.requirePresent(sale, "venta");
        if (sale.getLines().isEmpty()) {
            throw new ValidationException("La venta debe tener al menos una línea.");
        }
        if (sale.getId() != NOT_STORED_ID) {
            throw new BusinessRuleException("La venta ya fue registrada.");
        }
        requireStockForAll(sale);
        List<SaleLine> deducted = new ArrayList<>();
        try {
            deductStock(sale, deducted);
            saveProducts(sale);
            return saleDao.create(sale);
        } catch (DataAccessException | BusinessRuleException e) {
            giveBackStock(deducted, e);
            throw e;
        }
    }

    /**
     * Lists the purchases of a customer.
     *
     * @param customerId the customer's identifier
     * @return the sales ordered by sale time; empty if there are none
     * @throws ValidationException if the identifier is not positive
     * @throws DataAccessException if the sales cannot be read
     */
    public List<Sale> listByCustomer(int customerId) throws ValidationException, DataAccessException {
        Validator.requirePositive(customerId, "cliente");
        return saleDao.findByCustomer(customerId);
    }

    private void requireStockForAll(Sale sale) throws BusinessRuleException {
        for (SaleLine line : sale.getLines()) {
            if (!line.getItem().hasStock(line.getQuantity())) {
                throw new BusinessRuleException("Stock insuficiente de " + line.getItem().getName()
                    + " para vender " + line.getQuantity() + " unidades.");
            }
        }
    }

    private void deductStock(Sale sale, List<SaleLine> deducted) throws BusinessRuleException {
        for (SaleLine line : sale.getLines()) {
            line.getItem().deductStock(line.getQuantity());
            deducted.add(line);
        }
    }

    private void saveProducts(Sale sale) throws DataAccessException {
        for (SaleLine line : sale.getLines()) {
            if (line.getItem() instanceof Product product) {
                productDao.update(product);
            }
        }
    }

    private void giveBackStock(List<SaleLine> deducted, Exception cause) {
        for (SaleLine line : deducted) {
            try {
                line.getItem().addStock(line.getQuantity());
                if (line.getItem() instanceof Product product) {
                    productDao.update(product);
                }
            } catch (BusinessRuleException | DataAccessException e) {
                if (e != cause) {
                    cause.addSuppressed(e);
                }
            }
        }
    }
}
