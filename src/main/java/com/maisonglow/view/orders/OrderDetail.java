package com.maisonglow.view.orders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An order with its lines, for the detail panel.
 *
 * @param summary       the row of the list
 * @param lines         what was ordered
 * @param paymentMethod how it was paid, empty while it is not paid
 * @param saleId        the sale that the payment generated, empty while it is not paid
 */
public record OrderDetail(OrderSummary summary, List<Line> lines, Optional<PaymentMethod> paymentMethod,
                          Optional<Integer> saleId) {

    /**
     * One line of the order.
     *
     * @param name      product ordered
     * @param quantity  units
     * @param unitPrice price of one unit when the order was placed
     */
    public record Line(String name, int quantity, BigDecimal unitPrice) {

        /** @return quantity times the unit price */
        public BigDecimal subtotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public OrderDetail {
        Objects.requireNonNull(summary, "summary");
        lines = List.copyOf(lines);
        Objects.requireNonNull(paymentMethod, "paymentMethod");
        Objects.requireNonNull(saleId, "saleId");
    }
}
