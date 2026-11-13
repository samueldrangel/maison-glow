package com.maisonglow.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
/** Data for sale; business rules belong to services. */
public class Sale {
    private int id;
    private final Customer customer;
    private final LocalDateTime soldAt;
    private List<SaleLine> lines = new java.util.ArrayList<>();
    private Appointment appointment;
    private BigDecimal total = BigDecimal.ZERO;

    public Sale(Customer customer) {
        this(0, customer, LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
    }
    private Sale(int id, Customer customer, LocalDateTime soldAt) {
        this.id = id; this.customer = java.util.Objects.requireNonNull(customer, "customer");
        this.soldAt = java.util.Objects.requireNonNull(soldAt, "soldAt");
    }
    public static Sale restore(int id, Customer customer, LocalDateTime soldAt, List<SaleLine> lines, BigDecimal total) {
        Sale sale = new Sale(id, customer, soldAt); sale.setLines(lines); sale.setTotal(total); return sale;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Customer getCustomer() { return customer; }
    public LocalDateTime getSoldAt() { return soldAt; }
    public List<SaleLine> getLines() { return java.util.Collections.unmodifiableList(lines); }
    public void setLines(List<SaleLine> lines) { this.lines = new java.util.ArrayList<>(lines); }
    public java.util.Optional<Appointment> getAppointment() { return java.util.Optional.ofNullable(appointment); }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof Sale value && id > 0 && id == value.id;
    }

    // Stable while persistence assigns an id: safe for HashSet/HashMap keys.
    @Override
    public int hashCode() { return Sale.class.hashCode(); }
    @Override public String toString() { return "Venta " + id + " a " + customer.getName() + " por $" + total; }

}
