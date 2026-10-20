package com.maisonglow.service;

import com.maisonglow.model.Customer;
import com.maisonglow.model.Sale;
import com.maisonglow.util.InvoiceFormatter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Simulates sending invoices: prints each email instead of sending it and
 * keeps a copy, so the system works without a mail server or internet.
 */
public class SimulatedEmailNotifier implements EmailNotifier {

    private final InvoiceFormatter formatter;
    private final PrintStream output;
    private final List<String> sentMessages = new ArrayList<>();

    /**
     * Creates the notifier.
     *
     * @param formatter builds the invoice text
     * @param output    where the simulated emails are printed, usually {@code System.out}
     */
    public SimulatedEmailNotifier(InvoiceFormatter formatter, PrintStream output) {
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        this.output = Objects.requireNonNull(output, "output");
    }

    @Override
    public void sendInvoice(Customer customer, Sale sale) {
        String message = "Para: " + customer.getEmail() + '\n'
            + "Asunto: Factura N.° " + sale.getId() + " - " + formatter.getBusinessName() + "\n\n"
            + formatter.format(sale);
        sentMessages.add(message);
        output.println("[Correo simulado]");
        output.println(message);
    }

    /**
     * Returns the emails sent so far, oldest first.
     *
     * @return a read-only list of the messages
     */
    public List<String> getSentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }
}
