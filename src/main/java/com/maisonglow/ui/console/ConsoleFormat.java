package com.maisonglow.ui.console;

import com.maisonglow.model.Appointment;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Product;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Sale;
import com.maisonglow.model.SaleLine;
import com.maisonglow.model.Treatment;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Turns the entities into the one-line texts the console shows. Every line
 * starts with the id in brackets, because the user types ids to pick a record.
 */
final class ConsoleFormat {

    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final String SEPARATOR = " · ";

    private ConsoleFormat() {
    }

    static String money(double amount) {
        return String.format(COLOMBIA, "$ %,.0f", amount);
    }

    static String dateTime(LocalDateTime value) {
        return DATE_TIME.format(value);
    }

    static String customer(Customer customer) {
        return "[" + customer.getId() + "] " + customer.getName() + SEPARATOR + "doc. " + customer.getDocument()
            + SEPARATOR + customer.getPhone() + SEPARATOR + customer.getEmail()
            + SEPARATOR + customer.getNoShowCount() + " inasistencia(s)";
    }

    static String treatment(Treatment treatment) {
        return "[" + treatment.getId() + "] " + treatment.getName() + SEPARATOR + treatment.getDurationMinutes()
            + " min" + SEPARATOR + money(treatment.getPrice()) + SEPARATOR + state(treatment.isActive());
    }

    static String professional(Professional professional) {
        String treatments = professional.getTreatments().stream().map(Treatment::getName)
            .collect(Collectors.joining(", "));
        return "[" + professional.getId() + "] " + professional.getName() + SEPARATOR + professional.getSpecialty()
            + SEPARATOR + TIME.format(professional.getWorkStart()) + "-" + TIME.format(professional.getWorkEnd())
            + SEPARATOR + state(professional.isActive())
            + SEPARATOR + "servicios: " + (treatments.isEmpty() ? "ninguno" : treatments);
    }

    static String product(Product product) {
        return "[" + product.getId() + "] " + product.getName() + SEPARATOR + product.getCategory().getName()
            + SEPARATOR + money(product.getPrice()) + SEPARATOR + product.getStock() + " und. (mínimo "
            + product.getMinimumStock() + ")" + SEPARATOR + state(product.isActive());
    }

    static String appointment(Appointment appointment) {
        return "[" + appointment.getId() + "] " + dateTime(appointment.getStart()) + "-"
            + TIME.format(appointment.getEnd()) + SEPARATOR + appointment.getCustomer().getName()
            + SEPARATOR + appointment.getTreatment().getName() + " con " + appointment.getProfessional().getName()
            + SEPARATOR + appointment.getStatus().getLabel() + SEPARATOR + "anticipo "
            + money(appointment.getDeposit());
    }

    static String sale(Sale sale) {
        return "[" + sale.getId() + "] " + dateTime(sale.getSoldAt()) + SEPARATOR + sale.getCustomer().getName()
            + SEPARATOR + "total " + money(sale.calculateTotal());
    }

    static String saleLine(SaleLine line) {
        return line.getQuantity() + " x " + line.getItem().getName() + " a " + money(line.getUnitPrice())
            + " = " + money(line.calculateSubtotal());
    }

    static void printAll(ConsoleIo console, String emptyMessage, List<String> lines) {
        if (lines.isEmpty()) {
            console.println(emptyMessage);
            return;
        }
        lines.forEach(line -> console.println("  " + line));
    }

    private static String state(boolean active) {
        return active ? "activo" : "inactivo";
    }
}
