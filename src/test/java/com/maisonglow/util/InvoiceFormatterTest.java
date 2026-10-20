package com.maisonglow.util;

import com.maisonglow.model.Customer;
import com.maisonglow.model.Product;
import com.maisonglow.model.ProductCategory;
import com.maisonglow.model.Sale;
import com.maisonglow.model.SaleLine;
import com.maisonglow.model.Treatment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InvoiceFormatter tests")
class InvoiceFormatterTest {

    private final InvoiceFormatter formatter = new InvoiceFormatter("Maison Glow");

    private Sale mixedSale() throws Exception {
        Customer ana = new Customer(1, "1065123456", "Ana Pérez", "3001234567", "ana@correo.com");
        Product shampoo = new Product(1, "Shampoo", new ProductCategory(1, "Cabello"), 28000, 10, 2);
        Treatment haircut = new Treatment(1, "Corte de cabello", "Corte y secado", 35000, 45);
        return Sale.restore(12, ana, LocalDateTime.of(2026, 10, 13, 15, 30),
            List.of(SaleLine.restore(shampoo, 2, 28000), SaleLine.restore(haircut, 1, 35000)));
    }

    @Test
    @DisplayName("the invoice has the header with business, number, date and customer")
    void header() throws Exception {
        String invoice = formatter.format(mixedSale());

        assertThat(invoice).contains("Maison Glow", "Factura de venta N.° 12", "13/10/2026 15:30",
            "Cliente: Ana Pérez (1065123456)");
    }

    @Test
    @DisplayName("each line shows quantity, name, unit price and subtotal")
    void lines() throws Exception {
        String invoice = formatter.format(mixedSale());

        assertThat(invoice).contains("2 x Shampoo", "$ 28.000", "$ 56.000");
        assertThat(invoice).contains("1 x Corte de cabello", "$ 35.000");
    }

    @Test
    @DisplayName("the total adds every line")
    void total() throws Exception {
        String invoice = formatter.format(mixedSale());

        assertThat(invoice).containsPattern("TOTAL\\s+\\$ 91\\.000");
    }

    @Test
    @DisplayName("the unit price is the one saved in the line, not the current price")
    void priceAtSaleTime() throws Exception {
        Sale sale = mixedSale();
        ((Product) sale.getLines().get(0).getItem()).setPrice(30000);

        assertThat(formatter.format(sale)).contains("$ 56.000").doesNotContain("$ 60.000");
    }
}
