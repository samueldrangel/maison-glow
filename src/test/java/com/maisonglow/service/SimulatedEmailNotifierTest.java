package com.maisonglow.service;

import com.maisonglow.model.Customer;
import com.maisonglow.model.Sale;
import com.maisonglow.model.Treatment;
import com.maisonglow.util.InvoiceFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SimulatedEmailNotifier tests")
class SimulatedEmailNotifierTest {

    private final Customer ana = new Customer(1, "1065123456", "Ana Pérez", "3001234567", "ana@correo.com");
    private ByteArrayOutputStream printed;
    private SimulatedEmailNotifier notifier;

    @BeforeEach
    void setUp() {
        printed = new ByteArrayOutputStream();
        notifier = new SimulatedEmailNotifier(new InvoiceFormatter("Maison Glow"),
            new PrintStream(printed, true, StandardCharsets.UTF_8));
    }

    private Sale manicureSale(int id) throws Exception {
        Sale sale = new Sale(ana);
        sale.setId(id);
        sale.addLine(new Treatment(1, "Manicure", "Limado y esmalte", 25000, 40), 1);
        return sale;
    }

    @Test
    @DisplayName("sending an invoice prints the email and keeps a copy")
    void sendInvoice() throws Exception {
        EmailNotifier contract = notifier;

        contract.sendInvoice(ana, manicureSale(3));

        assertThat(notifier.getSentMessages()).singleElement().satisfies(message -> {
            assertThat(message).startsWith("Para: ana@correo.com");
            assertThat(message).contains("Asunto: Factura N.° 3 - Maison Glow", "1 x Manicure");
        });
        assertThat(printed.toString(StandardCharsets.UTF_8)).contains("[Correo simulado]", "ana@correo.com");
    }

    @Test
    @DisplayName("the sent emails are kept in order")
    void keepsOrder() throws Exception {
        notifier.sendInvoice(ana, manicureSale(1));
        notifier.sendInvoice(ana, manicureSale(2));

        assertThat(notifier.getSentMessages()).hasSize(2);
        assertThat(notifier.getSentMessages().get(1)).contains("Factura N.° 2");
    }
}
