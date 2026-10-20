package com.maisonglow.util;

import com.maisonglow.model.Customer;
import com.maisonglow.model.Sale;
import com.maisonglow.model.SaleLine;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Builds the plain-text invoice of a sale (RF-27), with the business name,
 * the customer, one row per line and the total.
 */
public class InvoiceFormatter {

    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int WIDTH = 56;
    private static final String DOUBLE_RULE = "=".repeat(WIDTH);
    private static final String SINGLE_RULE = "-".repeat(WIDTH);

    private final String businessName;

    /**
     * Creates a formatter for the given business.
     *
     * @param businessName the name printed at the top of every invoice
     */
    public InvoiceFormatter(String businessName) {
        this.businessName = Objects.requireNonNull(businessName, "businessName");
    }

    /**
     * Builds the invoice text of a sale.
     *
     * @param sale the sale to invoice
     * @return the invoice, ready to print or send
     */
    public String format(Sale sale) {
        Customer customer = sale.getCustomer();
        StringBuilder invoice = new StringBuilder();
        invoice.append(DOUBLE_RULE).append('\n')
            .append(businessName).append('\n')
            .append("Factura de venta N.° ").append(sale.getId()).append('\n')
            .append("Fecha: ").append(sale.getSoldAt().format(DATE_FORMAT)).append('\n')
            .append("Cliente: ").append(customer.getName())
            .append(" (").append(customer.getDocument()).append(")\n")
            .append(SINGLE_RULE).append('\n');
        for (SaleLine line : sale.getLines()) {
            invoice.append(formatLine(line)).append('\n');
        }
        invoice.append(SINGLE_RULE).append('\n')
            .append(String.format(COLOMBIA, "%-40s%16s", "TOTAL", money(sale.calculateTotal()))).append('\n')
            .append(DOUBLE_RULE);
        return invoice.toString();
    }

    /**
     * Returns the business name printed on the invoices.
     *
     * @return the business name
     */
    public String getBusinessName() {
        return businessName;
    }

    private String formatLine(SaleLine line) {
        String description = line.getQuantity() + " x " + line.getItem().getName();
        return String.format(COLOMBIA, "%-28s%12s%16s",
            description, money(line.getUnitPrice()), money(line.calculateSubtotal()));
    }

    private String money(double value) {
        return String.format(COLOMBIA, "$ %,.0f", value);
    }
}
