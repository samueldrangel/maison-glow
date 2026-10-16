package com.maisonglow.service;

import com.maisonglow.model.Product;

/**
 * The units of a product sold in a period, used by the low-rotation report
 * (RF-29).
 *
 * @param product   the product
 * @param unitsSold the units sold in the period; 0 if it did not sell
 */
public record ProductSales(Product product, int unitsSold) {
}
