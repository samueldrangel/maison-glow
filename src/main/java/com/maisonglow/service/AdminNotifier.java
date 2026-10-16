package com.maisonglow.service;

import java.util.List;

/**
 * Notifies the administrator about business alerts (RF-30). The statistics
 * depend on this contract, so a real channel can replace the simulated one in
 * delivery 2.
 */
public interface AdminNotifier {

    /**
     * Reports the products with the lowest sales.
     *
     * @param products   the products and their units sold, lowest first
     * @param windowDays the days of sales history analyzed
     */
    void notifyLowRotation(List<ProductSales> products, int windowDays);
}
