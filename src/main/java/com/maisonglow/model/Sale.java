package com.maisonglow.model;

import com.maisonglow.exception.BusinessRuleException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A sale made to a customer. Creates and groups its lines, which may sell
 * products and treatments alike through {@link Sellable}, and calculates its
 * total. It can be linked to the appointment whose treatment it charges.
 */
public class Sale {

    private int id;
    private final Customer customer;
    private final LocalDateTime soldAt;
    private final List<SaleLine> lines;
    private Appointment appointment;

    /**
     * Starts a sale to the given customer, sold now and without lines.
     *
     * @param customer who buys
     */
    public Sale(Customer customer) {
        this(0, customer, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    private Sale(int id, Customer customer, LocalDateTime soldAt) {
        this.id = id;
        this.customer = Objects.requireNonNull(customer, "customer");
        this.soldAt = Objects.requireNonNull(soldAt, "soldAt");
        this.lines = new ArrayList<>();
    }

    /**
     * Rebuilds a sale that was already stored, with its lines in their stored
     * order. Meant for the data access layer; the appointment is restored with
     * {@link #linkAppointment(Appointment)}.
     *
     * @param id       the stored identifier
     * @param customer who bought
     * @param soldAt   when the sale was made
     * @param lines    the stored lines
     * @return the rebuilt sale
     */
    public static Sale restore(int id, Customer customer, LocalDateTime soldAt, List<SaleLine> lines) {
        Sale sale = new Sale(id, customer, soldAt);
        sale.lines.addAll(lines);
        return sale;
    }

    /**
     * Adds units of a product or treatment to the sale. The line keeps the
     * item's current price; if the item is already in the sale, the units
     * are added to its line instead.
     *
     * @param item     what is sold
     * @param quantity how many units
     * @return the line that holds the item
     * @throws BusinessRuleException if the quantity or the item price is not positive
     */
    public SaleLine addLine(Sellable item, int quantity) throws BusinessRuleException {
        Optional<SaleLine> existing = findLine(item);
        if (existing.isPresent()) {
            existing.get().addQuantity(quantity);
            return existing.get();
        }
        SaleLine line = new SaleLine(item, quantity);
        lines.add(line);
        return line;
    }

    private Optional<SaleLine> findLine(Sellable item) {
        return lines.stream().filter(line -> line.sells(item)).findFirst();
    }

    /**
     * Calculates the amount to pay.
     *
     * @return the sum of the subtotals of every line; 0 if there are none
     */
    public double calculateTotal() {
        return lines.stream().mapToDouble(SaleLine::calculateSubtotal).sum();
    }

    /**
     * Links the sale to the appointment whose treatment it charges. A sale
     * can be linked to only one appointment, of the same customer.
     *
     * @param appointment the appointment charged
     * @throws BusinessRuleException if the sale is already linked or the
     *                               appointment belongs to another customer
     */
    public void linkAppointment(Appointment appointment) throws BusinessRuleException {
        Objects.requireNonNull(appointment, "appointment");
        if (this.appointment != null) {
            throw new BusinessRuleException("La venta ya está enlazada a una cita.");
        }
        if (!appointment.getCustomer().equals(customer)) {
            throw new BusinessRuleException("La cita es de otro cliente.");
        }
        this.appointment = appointment;
    }

    /**
     * Returns the identifier.
     *
     * @return the identifier; 0 if the sale is not stored yet
     */
    public int getId() {
        return id;
    }

    /**
     * Assigns the identifier given by the data store.
     *
     * @param id the new identifier
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Returns who bought.
     *
     * @return the customer
     */
    public Customer getCustomer() {
        return customer;
    }

    /**
     * Returns when the sale was made.
     *
     * @return the sale time
     */
    public LocalDateTime getSoldAt() {
        return soldAt;
    }

    /**
     * Returns the lines of the sale.
     *
     * @return a read-only view of the lines, in the order they were added
     */
    public List<SaleLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    /**
     * Returns the appointment charged by this sale.
     *
     * @return the appointment, or empty if the sale is not linked to one
     */
    public Optional<Appointment> getAppointment() {
        return Optional.ofNullable(appointment);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Sale sale)) {
            return false;
        }
        return id == sale.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Venta " + id + " a " + customer.getName() + " por $" + calculateTotal();
    }
}
