package com.maisonglow.service;

import com.maisonglow.dao.SaleDao;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Product;
import com.maisonglow.model.Sale;
import com.maisonglow.model.SaleLine;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Suggests discounts on low-rotation products that a customer has not bought
 * yet (RF-31). It only suggests them; nothing is stored, because the
 * {@code promotion} table arrives in delivery 2.
 */
public class PromotionService {

    private static final double FULL_PERCENTAGE = 100;

    private final LowRotationService lowRotation;
    private final SaleDao saleDao;
    private final SettingsService settings;

    /**
     * Creates the service.
     *
     * @param lowRotation finds the low-rotation products
     * @param saleDao     the sales storage, to know what the customer already bought
     * @param settings    provides the discount percentage
     */
    public PromotionService(LowRotationService lowRotation, SaleDao saleDao, SettingsService settings) {
        this.lowRotation = lowRotation;
        this.saleDao = saleDao;
        this.settings = settings;
    }

    /**
     * Suggests the configured discount on each low-rotation product the
     * customer has never bought.
     *
     * @param customerId the customer id
     * @param today      the last day of the low-rotation window, usually the current date
     * @return the suggestions, lowest sales first; empty if there is nothing new to offer
     * @throws ValidationException if the date is missing or a setting is invalid
     * @throws DataAccessException if the data cannot be read
     */
    public List<PromotionSuggestion> suggestPromotions(int customerId, LocalDate today)
        throws ValidationException, DataAccessException {
        Set<Integer> alreadyBought = productsBoughtBy(customerId);
        double discount = settings.lowRotationDiscountPercentage();
        return lowRotation.lowRotationProducts(today).stream()
            .map(ProductSales::product)
            .filter(product -> !alreadyBought.contains(product.getId()))
            .map(product -> suggest(product, discount))
            .toList();
    }

    private static PromotionSuggestion suggest(Product product, double discount) {
        double promotionalPrice = product.getPrice() * (FULL_PERCENTAGE - discount) / FULL_PERCENTAGE;
        return new PromotionSuggestion(product.getName(), product.getPrice(), discount, promotionalPrice);
    }

    private Set<Integer> productsBoughtBy(int customerId) throws DataAccessException {
        Set<Integer> productIds = new HashSet<>();
        for (Sale sale : saleDao.findByCustomer(customerId)) {
            for (SaleLine line : sale.getLines()) {
                if (line.getItem() instanceof Product product) {
                    productIds.add(product.getId());
                }
            }
        }
        return productIds;
    }
}
