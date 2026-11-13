package com.maisonglow.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/** Data for appointment; business rules belong to services. */
public class Appointment {
    private int id;
    private final Customer customer;
    private final Professional professional;
    private final Treatment treatment;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final LocalDateTime createdAt;
    private AppointmentStatus status;
    private BigDecimal deposit = BigDecimal.ZERO;
    private LocalDateTime reminderAt;

    public Appointment(int id, Customer customer, Professional professional, Treatment treatment,
                       LocalDateTime start, LocalDateTime end, AppointmentStatus status, LocalDateTime createdAt) {
        this.id = id; this.customer = java.util.Objects.requireNonNull(customer, "customer");
        this.professional = java.util.Objects.requireNonNull(professional, "professional");
        this.treatment = java.util.Objects.requireNonNull(treatment, "treatment");
        this.start = java.util.Objects.requireNonNull(start, "start");
        this.end = java.util.Objects.requireNonNull(end, "end");
        this.status = java.util.Objects.requireNonNull(status, "status");
        this.createdAt = java.util.Objects.requireNonNull(createdAt, "createdAt");
        if (!end.isAfter(start)) throw new IllegalArgumentException("La cita debe terminar después de empezar.");
    }
    public static Appointment restore(int id, Customer customer, Professional professional, Treatment treatment,
                                      LocalDateTime start, LocalDateTime end, AppointmentStatus status, LocalDateTime createdAt) {
        return new Appointment(id, customer, professional, treatment, start, end, status, createdAt);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Customer getCustomer() { return customer; }
    public Professional getProfessional() { return professional; }
    public Treatment getTreatment() { return treatment; }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
    public BigDecimal getDeposit() { return deposit; }
    public void setDeposit(BigDecimal deposit) { this.deposit = deposit; }
    public LocalDateTime getReminderAt() { return reminderAt; }
    public void setReminderAt(LocalDateTime reminderAt) { this.reminderAt = reminderAt; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof Appointment value && id > 0 && id == value.id;
    }

    // Stable while persistence assigns an id: safe for HashSet/HashMap keys.
    @Override
    public int hashCode() { return Appointment.class.hashCode(); }
    @Override public String toString() {
        return treatment.getName() + " con " + professional.getName() + " el " + start + " (" + status.getLabel() + ")";
    }

}
