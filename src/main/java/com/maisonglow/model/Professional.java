package com.maisonglow.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a specialist of the business, with the treatments they provide and
 * their working hours. Knows whether they can attend an appointment in a
 * given time slot.
 */
public class Professional extends Person {

    private String specialty;
    private LocalTime workStart;
    private LocalTime workEnd;
    private boolean active;
    private final List<Treatment> treatments;

    /**
     * Creates an active professional who does not offer any treatment yet.
     *
     * @param id        the identifier; 0 if the professional is not stored yet
     * @param document  the identity document
     * @param name      the full name
     * @param phone     the contact phone number
     * @param email     the contact email address
     * @param specialty the main area of work, such as hairdressing or makeup
     * @param workStart the time the working day starts
     * @param workEnd   the time the working day ends
     */
    public Professional(int id, String document, String name, String phone, String email,
                        String specialty, LocalTime workStart, LocalTime workEnd) {
        super(id, document, name, phone, email);
        this.specialty = specialty;
        this.workStart = workStart;
        this.workEnd = workEnd;
        this.active = true;
        this.treatments = new ArrayList<>();
    }

    /**
     * Adds a treatment to the ones this professional provides. Adding a treatment
     * that is already listed has no effect.
     *
     * @param treatment the treatment to add
     */
    public void addTreatment(Treatment treatment) {
        if (!treatments.contains(treatment)) {
            treatments.add(treatment);
        }
    }

    /**
     * Removes a treatment from the ones this professional provides.
     *
     * @param treatment the treatment to remove
     */
    public void removeTreatment(Treatment treatment) {
        treatments.remove(treatment);
    }

    /**
     * Indicates whether this professional provides the given treatment.
     *
     * @param treatment the treatment to check
     * @return {@code true} if the treatment is in the professional's list
     */
    public boolean offers(Treatment treatment) {
        return treatments.contains(treatment);
    }

    /**
     * Indicates whether a time slot falls entirely within the working hours
     * of a single day.
     *
     * @param start the start of the slot
     * @param end   the end of the slot
     * @return {@code true} if the slot starts and ends on the same day, inside
     *         the working hours; the limits themselves are included
     */
    public boolean worksDuring(LocalDateTime start, LocalDateTime end) {
        return start.toLocalDate().equals(end.toLocalDate())
            && start.isBefore(end)
            && !start.toLocalTime().isBefore(workStart)
            && !end.toLocalTime().isAfter(workEnd);
    }

    /**
     * Returns the treatments this professional provides.
     *
     * @return a read-only view of the treatments
     */
    public List<Treatment> getTreatments() {
        return Collections.unmodifiableList(treatments);
    }

    /**
     * Returns the main area of work.
     *
     * @return the specialty
     */
    public String getSpecialty() {
        return specialty;
    }

    /**
     * Changes the main area of work.
     *
     * @param specialty the new specialty
     */
    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    /**
     * Returns the time the working day starts.
     *
     * @return the start of the working day
     */
    public LocalTime getWorkStart() {
        return workStart;
    }

    /**
     * Returns the time the working day ends.
     *
     * @return the end of the working day
     */
    public LocalTime getWorkEnd() {
        return workEnd;
    }

    /**
     * Changes the working hours.
     *
     * @param workStart the new start of the working day
     * @param workEnd   the new end of the working day
     */
    public void setWorkingHours(LocalTime workStart, LocalTime workEnd) {
        this.workStart = workStart;
        this.workEnd = workEnd;
    }

    /**
     * Indicates whether the professional currently attends appointments.
     *
     * @return {@code true} if the professional is active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Allows the professional to receive appointments again.
     */
    public void activate() {
        active = true;
    }

    /**
     * Stops the professional from receiving new appointments.
     */
    public void deactivate() {
        active = false;
    }

    @Override
    public String getRoleDescription() {
        return "Profesional";
    }
}
