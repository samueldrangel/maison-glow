package com.maisonglow.service;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Simulates the administrator alerts: prints each notification and keeps a
 * copy, so the system works without a mail server or internet.
 */
public class SimulatedAdminNotifier implements AdminNotifier {

    private final PrintStream output;
    private final List<String> notifications = new ArrayList<>();

    /**
     * Creates the notifier.
     *
     * @param output where the notifications are printed, usually {@code System.out}
     */
    public SimulatedAdminNotifier(PrintStream output) {
        this.output = Objects.requireNonNull(output, "output");
    }

    @Override
    public void notifyLowRotation(List<ProductSales> products, int windowDays) {
        StringBuilder message = new StringBuilder("Productos de baja rotación en los últimos ")
            .append(windowDays).append(" días:");
        if (products.isEmpty()) {
            message.append("\n  (ninguno)");
        }
        for (ProductSales sales : products) {
            message.append("\n  - ").append(sales.product().getName()).append(": ")
                .append(sales.unitsSold()).append(" und. vendidas");
        }
        notifications.add(message.toString());
        output.println("[Aviso al administrador]");
        output.println(message);
    }

    /**
     * Returns the notifications sent so far, oldest first.
     *
     * @return a read-only list of the notifications
     */
    public List<String> getNotifications() {
        return Collections.unmodifiableList(notifications);
    }
}
