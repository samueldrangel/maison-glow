package com.maisonglow.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a specialist of the business, with the services they provide and
 * their working hours. Knows whether they can attend an appointment in a
 * given time slot.
 */
public class Professional extends Person {

    private String specialty;
    private LocalTime workStart;
    private LocalTime workEnd;
    private boolean active;
    private final List<BeautyService> services;

    /**
     * Creates an active professional who does not offer any service yet.
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
        this.services = new ArrayList<>();
    }

    /**
     * Adds a service to the ones this professional provides. Adding a service
     * that is already listed has no effect.
     *
     * @param service the service to add
     */
    public void addService(BeautyService service) {
        if (!services.contains(service)) {
            services.add(service);
        }
    }

    /**
     * Removes a service from the ones this professional provides.
     *
     * @param service the service to remove
     */
    public void removeService(BeautyService service) {
        services.remove(service);
    }

    /**
     * Indicates whether this professional provides the given service.
     *
     * @param service the service to check
     * @return {@code true} if the service is in the professional's list
     */
    public boolean offers(BeautyService service) {
        return services.contains(service);
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
     * Returns the services this professional provides.
     *
     * @return a read-only view of the services
     */
    public List<BeautyService> getServices() {
        return Collections.unmodifiableList(services);
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
