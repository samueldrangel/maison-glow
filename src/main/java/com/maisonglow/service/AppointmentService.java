package com.maisonglow.service;

import com.maisonglow.dao.AppointmentDao;
import com.maisonglow.dao.CrudDao;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.rule.AppointmentRule;
import com.maisonglow.util.Validator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Coordinates the booking and management of appointments (RF-10 to RF-13).
 * Guarantees that a professional is never booked twice at the same time and
 * applies the booking rules received in the constructor, in order, so a new
 * rule never requires changing this class (ADR-0007).
 */
public class AppointmentService {

    private static final int NOT_STORED_ID = 0;

    private final AppointmentDao appointmentDao;
    private final CrudDao<Customer> customerDao;
    private final List<AppointmentRule> rules;

    /**
     * Creates the service.
     *
     * @param appointmentDao where appointments are stored
     * @param customerDao    where customers are stored, to save their no-shows
     * @param rules          the booking rules, applied in this order
     */
    public AppointmentService(AppointmentDao appointmentDao, CrudDao<Customer> customerDao,
                              List<AppointmentRule> rules) {
        this.appointmentDao = Objects.requireNonNull(appointmentDao, "appointmentDao");
        this.customerDao = Objects.requireNonNull(customerDao, "customerDao");
        this.rules = List.copyOf(rules);
    }

    /**
     * Books a pending appointment. The professional must be active, offer the
     * treatment, work during the whole slot and have no other active
     * appointment in it; then every booking rule is applied and the
     * appointment is stored.
     *
     * @param customer     who books the appointment
     * @param professional who attends it
     * @param treatment    the treatment booked
     * @param start        when it starts
     * @param deposit      the amount paid in advance; 0 if none
     * @return the stored appointment, with its id assigned
     * @throws ValidationException   if a required value is missing or a rule parameter is invalid
     * @throws BusinessRuleException if the slot is not available or a rule is broken
     * @throws DataAccessException   if the appointments cannot be read or stored
     */
    public Appointment book(Customer customer, Professional professional, Treatment treatment,
                            LocalDateTime start, double deposit)
        throws ValidationException, BusinessRuleException, DataAccessException {
        Validator.requirePresent(customer, "cliente");
        Validator.requirePresent(professional, "profesional");
        Validator.requirePresent(treatment, "tratamiento");
        Validator.requirePresent(start, "fecha y hora");
        requireBookable(professional, treatment);
        Appointment appointment = new Appointment(NOT_STORED_ID, customer, professional, treatment, start);
        requireFreeSlot(appointment);
        appointment.setDeposit(deposit);
        applyRules(appointment);
        return appointmentDao.create(appointment);
    }

    /**
     * Moves an active appointment to a new start, keeping its duration. The
     * new slot is checked like a new booking, ignoring the appointment's own
     * old slot, and the rules are applied again.
     *
     * @param appointmentId the appointment to move
     * @param newStart      when it starts now
     * @return the moved appointment, already stored
     * @throws ValidationException   if the new start is missing or a rule parameter is invalid
     * @throws BusinessRuleException if the appointment does not exist or is closed,
     *                               the new slot is not available or a rule is broken
     * @throws DataAccessException   if the appointments cannot be read or stored
     */
    public Appointment reschedule(int appointmentId, LocalDateTime newStart)
        throws ValidationException, BusinessRuleException, DataAccessException {
        Validator.requirePresent(newStart, "nueva fecha y hora");
        Appointment moved = find(appointmentId).rescheduledTo(newStart);
        requireBookable(moved.getProfessional(), moved.getTreatment());
        requireFreeSlot(moved);
        applyRules(moved);
        appointmentDao.update(moved);
        return moved;
    }

    /**
     * Records that the customer confirmed the appointment.
     *
     * @param appointmentId the appointment to confirm
     * @throws BusinessRuleException if the appointment does not exist or is not pending
     * @throws DataAccessException   if the appointment cannot be read or stored
     */
    public void confirm(int appointmentId) throws BusinessRuleException, DataAccessException {
        changeStatus(appointmentId, Appointment::confirm);
    }

    /**
     * Cancels the appointment, freeing the professional's schedule.
     *
     * @param appointmentId the appointment to cancel
     * @throws BusinessRuleException if the appointment does not exist or is no longer active
     * @throws DataAccessException   if the appointment cannot be read or stored
     */
    public void cancel(int appointmentId) throws BusinessRuleException, DataAccessException {
        changeStatus(appointmentId, Appointment::cancel);
    }

    /**
     * Records that the treatment was provided.
     *
     * @param appointmentId the appointment to complete
     * @throws BusinessRuleException if the appointment does not exist or is no longer active
     * @throws DataAccessException   if the appointment cannot be read or stored
     */
    public void complete(int appointmentId) throws BusinessRuleException, DataAccessException {
        changeStatus(appointmentId, Appointment::complete);
    }

    /**
     * Records that the customer did not show up and stores the customer's new
     * no-show count.
     *
     * @param appointmentId the appointment the customer missed
     * @throws BusinessRuleException if the appointment does not exist or is no longer active
     * @throws DataAccessException   if the appointment or the customer cannot be read or stored
     */
    public void markNoShow(int appointmentId) throws BusinessRuleException, DataAccessException {
        Appointment appointment = changeStatus(appointmentId, Appointment::markNoShow);
        customerDao.update(appointment.getCustomer());
    }

    /**
     * Returns the schedule of a day, in every status.
     *
     * @param date the day
     * @return the appointments ordered by start time; empty if there are none
     * @throws ValidationException if the date is missing
     * @throws DataAccessException if the appointments cannot be read
     */
    public List<Appointment> listByDate(LocalDate date) throws ValidationException, DataAccessException {
        Validator.requirePresent(date, "fecha");
        return appointmentDao.findByDate(date);
    }

    /**
     * Indicates whether a professional can take an appointment in the given
     * slot: they are active, work during the whole slot and have no other
     * active appointment in it.
     *
     * @param professional the professional
     * @param start        the start of the slot
     * @param end          the end of the slot
     * @return {@code true} if the slot is available
     * @throws ValidationException if a required value is missing
     * @throws DataAccessException if the appointments cannot be read
     */
    public boolean checkAvailability(Professional professional, LocalDateTime start, LocalDateTime end)
        throws ValidationException, DataAccessException {
        Validator.requirePresent(professional, "profesional");
        Validator.requirePresent(start, "inicio");
        Validator.requirePresent(end, "fin");
        return professional.isActive()
            && professional.worksDuring(start, end)
            && !hasConflict(professional, start, end, NOT_STORED_ID);
    }

    private void requireBookable(Professional professional, Treatment treatment) throws BusinessRuleException {
        if (!professional.isActive()) {
            throw new BusinessRuleException("El profesional " + professional.getName() + " no está activo.");
        }
        if (!treatment.isActive()) {
            throw new BusinessRuleException("El tratamiento " + treatment.getName() + " no está disponible.");
        }
        if (!professional.offers(treatment)) {
            throw new BusinessRuleException(professional.getName() + " no ofrece " + treatment.getName() + ".");
        }
    }

    private void requireFreeSlot(Appointment appointment) throws BusinessRuleException, DataAccessException {
        Professional professional = appointment.getProfessional();
        if (!professional.worksDuring(appointment.getStart(), appointment.getEnd())) {
            throw new BusinessRuleException(professional.getName() + " solo atiende de "
                + professional.getWorkStart() + " a " + professional.getWorkEnd() + ".");
        }
        if (hasConflict(professional, appointment.getStart(), appointment.getEnd(), appointment.getId())) {
            throw new BusinessRuleException(professional.getName() + " ya tiene una cita en ese horario.");
        }
    }

    private boolean hasConflict(Professional professional, LocalDateTime start, LocalDateTime end, int ignoredId)
        throws DataAccessException {
        return appointmentDao.findByProfessional(professional.getId()).stream()
            .filter(Appointment::blocksSchedule)
            .filter(other -> other.getId() != ignoredId)
            .anyMatch(other -> other.overlapsWith(start, end));
    }

    private void applyRules(Appointment appointment)
        throws BusinessRuleException, ValidationException, DataAccessException {
        for (AppointmentRule rule : rules) {
            rule.apply(appointment);
        }
    }

    private Appointment changeStatus(int appointmentId, StatusChange change)
        throws BusinessRuleException, DataAccessException {
        Appointment appointment = find(appointmentId);
        change.applyTo(appointment);
        appointmentDao.update(appointment);
        return appointment;
    }

    private Appointment find(int appointmentId) throws BusinessRuleException, DataAccessException {
        return appointmentDao.findById(appointmentId).orElseThrow(() ->
            new BusinessRuleException("No existe la cita con id " + appointmentId + "."));
    }

    /** A status change of an appointment, such as confirming or cancelling it. */
    @FunctionalInterface
    private interface StatusChange {

        void applyTo(Appointment appointment) throws BusinessRuleException;
    }
}
