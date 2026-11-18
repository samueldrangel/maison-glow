package com.maisonglow.view.orders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One row of the list of orders.
 *
 * @param id            number of the order
 * @param customerName  who placed it
 * @param createdAt     when it was placed
 * @param total         what it adds up to
 * @param state         where it is
 * @param reservedUntil when the reservation of the stock ends; null when the order is no longer open
 */
public record OrderSummary(int id, String customerName, LocalDateTime createdAt, BigDecimal total, OrderState state,
                           LocalDateTime reservedUntil) {

    public OrderSummary {
        Objects.requireNonNull(customerName, "customerName");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(total, "total");
        Objects.requireNonNull(state, "state");
    }
}
