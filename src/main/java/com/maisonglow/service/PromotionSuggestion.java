package com.maisonglow.service;

/**
 * A suggested discount on a low-rotation product for a customer (RF-31). It
 * is only a suggestion; stored promotions arrive in delivery 2.
 *
 * @param productName        the product to promote
 * @param regularPrice       the current price
 * @param discountPercentage the suggested discount, from the business settings
 * @param promotionalPrice   the price after the discount
 */
public record PromotionSuggestion(String productName, double regularPrice, double discountPercentage,
                                  double promotionalPrice) {
}
