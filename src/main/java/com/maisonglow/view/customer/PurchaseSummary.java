package com.maisonglow.view.customer;

import com.maisonglow.model.Sale;
import com.maisonglow.model.SaleLine;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * One line of "Últimas compras" in the customer panel: when it was bought, what in a few words and how much.
 *
 * @param soldAt when the sale was registered
 * @param items  the first item with its quantity, and how many more there were
 * @param total  what the customer paid
 */
public record PurchaseSummary(LocalDateTime soldAt, String items, BigDecimal total) {

    /**
     * @param sales the sales of one customer, in any order
     * @param limit how many to keep
     * @return the most recent sales first (the later number wins a tie in the time), at most {@code limit} of them
     */
    public static List<PurchaseSummary> recent(List<Sale> sales, int limit) {
        return sales.stream()
            .sorted(Comparator.comparing(Sale::getSoldAt).thenComparing(Sale::getId).reversed())
            .limit(limit)
            .map(PurchaseSummary::of)
            .toList();
    }

    private static PurchaseSummary of(Sale sale) {
        List<SaleLine> lines = sale.getLines();
        String items = "";
        if (!lines.isEmpty()) {
            SaleLine first = lines.get(0);
            items = first.getItem().getName() + (first.getQuantity() > 1 ? " ×" + first.getQuantity() : "");
            if (lines.size() > 1) {
                items += " y " + (lines.size() - 1) + " más";
            }
        }
        return new PurchaseSummary(sale.getSoldAt(), items, sale.getTotal());
    }
}
