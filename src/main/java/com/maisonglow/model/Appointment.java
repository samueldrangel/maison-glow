package com.maisonglow.model;

import com.maisonglow.exception.BusinessRuleException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Represents the booking of a treatment with a professional for a customer
 * at a given time. Knows its own time slot, whether it overlaps another one
 * and which status changes are allowed.
 *
 * <p>The end is fixed when the appointment is booked, so changing the
 * duration of the treatment later does not move existing appointments.</p>
 */
public class Appointment {

    private int id;
    private final Customer customer;
    private final Professional professional;
    private final Treatment treatment;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final LocalDateTime createdAt;
    private AppointmentStatus status;
    private double deposit;
    private LocalDateTime reminderAt;

    /**
     * Books a new pending appointment, without deposit or reminder. Its end is
     * the start plus the duration of the treatment.
     *
     * @param id           the identifier; 0 if the appointment is not stored yet
     * @param customer     who books the appointment
     * @param professional who attends it
     * @param treatment    the treatment booked
     * @param start        when it starts
     * @throws IllegalArgumentException if the treatment has no positive duration
     */
    public Appointment(int id, Customer customer, Professional professional, Treatment treatment,
                       LocalDateTime start) {
        this(id, customer, professional, treatment, start, endOf(treatment, start),
            AppointmentStatus.PENDING, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    private Appointment(int id, Customer customer, Professional professional, Treatment treatment,
                        LocalDateTime start, LocalDateTime end, AppointmentStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.customer = Objects.requireNonNull(customer, "customer");
        this.professional = Objects.requireNonNull(professional, "professional");
        this.treatment = Objects.requireNonNull(treatment, "treatment");
        this.start = Objects.requireNonNull(start, "start");
        this.end = Objects.requireNonNull(end, "end");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("La cita debe terminar después de empezar: " + start + " a " + end);
        }
    }

    private static LocalDateTime endOf(Treatment treatment, LocalDateTime start) {
        Objects.requireNonNull(treatment, "treatment");
        Objects.requireNonNull(start, "start");
        return treatment.calculateEnd(start);
    }

    /**
     * Rebuilds an appointment that was already stored, keeping its original
     * end, status and creation time. Meant for the data access layer; the
     * deposit and the reminder are restored with their setters.
     *
     * @param id           the stored identifier
     * @param customer     who booked the appointment
     * @param professional who attends it
     * @param treatment    the treatment booked
     * @param start        when it starts
     * @param end          when it ends, as stored
     * @param status       the stored status
     * @param createdAt    when it was booked
     * @return the rebuilt appointment
     * @throws IllegalArgumentException if the end is not after the start
     */
    public static Appointment restore(int id, Customer customer, Professional professional, Treatment treatment,
                                      LocalDateTime start, LocalDateTime end, AppointmentStatus status,
                                      LocalDateTime createdAt) {
        return new Appointment(id, customer, professional, treatment, start, end, status, createdAt);
    }

    /**
     * Indicates whether the time slots of two appointments overlap. Touching
     * slots (one ends exactly when the other starts) do not overlap. Only the
     * times are compared; checking the professional and the status is up to
     * the caller.
     *
     * @param other the appointment to compare with
     * @return {@code true} if both slots share some time
     */
    public boolean overlapsWith(Appointment other) {
        return overlapsWith(other.start, other.end);
    }

    /**
     * Indicates whether this appointment shares some time with the given
     * slot. Touching slots do not overlap.
     *
     * @param otherStart the start of the slot
     * @param otherEnd   the end of the slot
     * @return {@code true} if both slots share some time
     */
    public boolean overlapsWith(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return start.isBefore(otherEnd) && end.isAfter(otherStart);
    }

    /**
     * Returns a copy of this appointment moved to a new start, with the same
     * id, duration, status and deposit. The reminder is not copied because it
     * belonged to the old start; the booking rules schedule it again. This
     * appointment is left unchanged, so a rejected move has no effect.
     *
     * @param newStart when the moved appointment starts
     * @return the moved copy
     * @throws BusinessRuleException if the appointment is no longer active
     */
    public Appointment rescheduledTo(LocalDateTime newStart) throws BusinessRuleException {
        Objects.requireNonNull(newStart, "newStart");
        if (!blocksSchedule()) {
            throw new BusinessRuleException("No se puede reprogramar una cita " + status.getLabel() + ".");
        }
        LocalDateTime newEnd = newStart.plus(Duration.between(start, end));
        Appointment moved = new Appointment(id, customer, professional, treatment, newStart, newEnd, status, createdAt);
        moved.deposit = deposit;
        return moved;
    }

    /**
     * Indicates whether the appointment still occupies the professional's schedule.
     *
     * @return {@code true} if it is pending or confirmed
     */
    public boolean blocksSchedule() {
        return status.blocksSchedule();
    }

    /**
     * Records that the customer confirmed the appointment.
     *
     * @throws BusinessRuleException if the appointment is not pending
     */
    public void confirm() throws BusinessRuleException {
        changeTo(AppointmentStatus.CONFIRMED, "confirmar");
    }

    /**
     * Cancels the appointment, freeing the professional's schedule.
     *
     * @throws BusinessRuleException if the appointment is no longer active
     */
    public void cancel() throws BusinessRuleException {
        changeTo(AppointmentStatus.CANCELLED, "cancelar");
    }

    /**
     * Records that the treatment was provided.
     *
     * @throws BusinessRuleException if the appointment is no longer active
     */
    public void complete() throws BusinessRuleException {
        changeTo(AppointmentStatus.COMPLETED, "completar");
    }

    /**
     * Records that the customer did not show up and adds a no-show to the
     * customer. The caller must save both the appointment and the customer.
     *
     * @throws BusinessRuleException if the appointment is no longer active
     */
    public void markNoShow() throws BusinessRuleException {
        changeTo(AppointmentStatus.NO_SHOW, "marcar como inasistencia");
        customer.registerNoShow();
    }

    private void changeTo(AppointmentStatus next, String action) throws BusinessRuleException {
        if (!status.canChangeTo(next)) {
            throw new BusinessRuleException("No se puede " + action + " una cita " + status.getLabel() + ".");
        }
        status = next;
    }

    /**
     * Returns the identifier.
     *
     * @return the identifier
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
     * Returns who booked the appointment.
     *
     * @return the customer
     */
    public Customer getCustomer() {
        return customer;
    }

    /**
     * Returns who attends the appointment.
     *
     * @return the professional
     */
    public Professional getProfessional() {
        return professional;
    }

    /**
     * Returns the treatment booked.
     *
     * @return the treatment
     */
    public Treatment getTreatment() {
        return treatment;
    }

    /**
     * Returns when the appointment starts.
     *
     * @return the start
     */
    public LocalDateTime getStart() {
        return start;
    }

    /**
     * Returns when the appointment ends.
     *
     * @return the end, fixed when the appointment was booked
     */
    public LocalDateTime getEnd() {
        return end;
    }

    /**
     * Returns the current status.
     *
     * @return the status
     */
    public AppointmentStatus getStatus() {
        return status;
    }

    /**
     * Returns the deposit paid when booking.
     *
     * @return the deposit; 0 if none was paid
     */
    public double getDeposit() {
        return deposit;
    }

    /**
     * Records the deposit paid when booking.
     *
     * @param deposit the amount paid
     * @throws BusinessRuleException if the amount is negative or not a finite number
     */
    public void setDeposit(double deposit) throws BusinessRuleException {
        if (!Double.isFinite(deposit) || deposit < 0) {
            throw new BusinessRuleException("El anticipo no puede ser negativo.");
        }
        this.deposit = deposit;
    }

    /**
     * Returns when the reminder must be sent.
     *
     * @return the reminder time, or {@code null} if none is scheduled
     */
    public LocalDateTime getReminderAt() {
        return reminderAt;
    }

    /**
     * Schedules the reminder.
     *
     * @param reminderAt when to send it, or {@code null} to remove it
     */
    public void setReminderAt(LocalDateTime reminderAt) {
        this.reminderAt = reminderAt;
    }

    /**
     * Returns when the appointment was booked.
     *
     * @return the creation time
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Appointment appointment)) {
            return false;
        }
        return id == appointment.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return treatment.getName() + " con " + professional.getName() + " el " + start
            + " (" + status.getLabel() + ")";
    }
}
