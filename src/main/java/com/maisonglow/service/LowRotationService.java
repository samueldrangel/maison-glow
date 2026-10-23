package com.maisonglow.service;

import com.maisonglow.dao.CrudDao;
import com.maisonglow.dao.SaleDao;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Product;
import com.maisonglow.model.Sale;
import com.maisonglow.model.SaleLine;
import com.maisonglow.util.Validator;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detects the products that sell the least in a recent period and warns the
 * administrator (RF-29, RF-30). The length of the period and how many
 * products to report come from the business settings, never from fixed
 * numbers.
 */
public class LowRotationService {

    private final SaleDao saleDao;
    private final CrudDao<Product> productDao;
    private final SettingsService settings;

    /**
     * Creates the service.
     *
     * @param saleDao    the sales storage
     * @param productDao the products storage
     * @param settings   provides the analysis window and how many products to report
     */
    public LowRotationService(SaleDao saleDao, CrudDao<Product> productDao, SettingsService settings) {
        this.saleDao = saleDao;
        this.productDao = productDao;
        this.settings = settings;
    }

    /**
     * Returns the active products with the fewest units sold in the window
     * that ends on the given day, including products that did not sell at all
     * (RF-29).
     *
     * @param today the last day of the window, usually the current date
     * @return the products and their units sold, lowest first; ties by name
     * @throws ValidationException if the date is missing or a setting is invalid
     * @throws DataAccessException if the data cannot be read
     */
    public List<ProductSales> lowRotationProducts(LocalDate today) throws ValidationException, DataAccessException {
        Validator.requirePresent(today, "fecha");
        Map<Integer, Integer> units = unitsSoldSince(today.minusDays(settings.lowRotationWindowDays() - 1L), today);
        return productDao.findAll().stream()
            .filter(Product::isActive)
            .map(product -> new ProductSales(product, units.getOrDefault(product.getId(), 0)))
            .sorted(Comparator.comparingInt(ProductSales::unitsSold)
                .thenComparing(sales -> sales.product().getName()))
            .limit(settings.lowRotationTopN())
            .toList();
    }

    /**
     * Sends the low-rotation products to the administrator (RF-30). The
     * notifier is received here, not in the constructor, because only this
     * report uses it.
     *
     * @param notifier the channel that receives the alert
     * @param today    the last day of the window, usually the current date
     * @return the products reported, lowest first
     * @throws ValidationException if the date is missing or a setting is invalid
     * @throws DataAccessException if the data cannot be read
     */
    public List<ProductSales> notifyAdministrator(AdminNotifier notifier, LocalDate today)
        throws ValidationException, DataAccessException {
        List<ProductSales> products = lowRotationProducts(today);
        notifier.notifyLowRotation(products, settings.lowRotationWindowDays());
        return products;
    }

    private Map<Integer, Integer> unitsSoldSince(LocalDate from, LocalDate to) throws DataAccessException {
        Map<Integer, Integer> units = new HashMap<>();
        for (Sale sale : saleDao.findBetween(from, to)) {
            for (SaleLine line : sale.getLines()) {
                if (line.getItem() instanceof Product product) {
                    units.merge(product.getId(), line.getQuantity(), Integer::sum);
                }
            }
        }
        return units;
    }
}
