package com.maisonglow.view.customer;

import com.maisonglow.model.Customer;
import com.maisonglow.model.Product;
import com.maisonglow.model.ProductCategory;
import com.maisonglow.model.Sale;
import com.maisonglow.model.SaleLine;
import com.maisonglow.model.Treatment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PurchaseSummary tests")
class PurchaseSummaryTest {

    private static final Customer ANA = new Customer(1, "1001", "Ana García", "3001112233", "ana@correo.com");
    private static final Product SHAMPOO = new Product(1, "Champú Nutritivo", new ProductCategory(1, "Cabello"), new BigDecimal("28000"), 10, 5);
    private static final Treatment CUT = new Treatment(1, "Corte", "Corte de cabello", new BigDecimal("45000"), 60);

    private static Sale sale(int day, SaleLine... lines) {
        BigDecimal total = List.of(lines).stream().map(line -> line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Sale.restore(day, ANA, LocalDateTime.of(2026, 11, day, 10, 0), List.of(lines), total);
    }

    @Test
    @DisplayName("keeps the most recent sales first and no more than the limit")
    void recentFirst() {
        List<Sale> sales = List.of(sale(1, new SaleLine(SHAMPOO, 1, SHAMPOO.getPrice())), sale(9, new SaleLine(CUT, 1, CUT.getPrice())),
            sale(5, new SaleLine(SHAMPOO, 1, SHAMPOO.getPrice())), sale(7, new SaleLine(SHAMPOO, 1, SHAMPOO.getPrice())));

        List<PurchaseSummary> recent = PurchaseSummary.recent(sales, 3);

        assertThat(recent).extracting(summary -> summary.soldAt().getDayOfMonth()).containsExactly(9, 7, 5);
    }

    @Test
    @DisplayName("two sales in the same second keep the later one first")
    void sameSecondKeepsTheLaterNumberFirst() {
        Sale earlier = sale(5, new SaleLine(SHAMPOO, 1, SHAMPOO.getPrice()));
        Sale later = Sale.restore(6, ANA, earlier.getSoldAt(), List.of(new SaleLine(CUT, 1, CUT.getPrice())), CUT.getPrice());

        assertThat(PurchaseSummary.recent(List.of(earlier, later), 2)).extracting(PurchaseSummary::items).containsExactly("Corte", "Champú Nutritivo");
    }

    @Test
    @DisplayName("describes the first item with its quantity and how many more there were")
    void describesTheItems() {
        Sale one = sale(2, new SaleLine(SHAMPOO, 1, SHAMPOO.getPrice()));
        Sale several = sale(3, new SaleLine(SHAMPOO, 2, SHAMPOO.getPrice()), new SaleLine(CUT, 1, CUT.getPrice()));

        List<PurchaseSummary> summaries = PurchaseSummary.recent(List.of(one, several), 2);

        assertThat(summaries).extracting(PurchaseSummary::items).containsExactly("Champú Nutritivo ×2 y 1 más", "Champú Nutritivo");
        assertThat(summaries.get(0).total()).isEqualByComparingTo("101000");
    }

    @Test
    @DisplayName("gives an empty list when there are no sales")
    void noSales() {
        assertThat(PurchaseSummary.recent(List.of(), 3)).isEmpty();
    }
}
