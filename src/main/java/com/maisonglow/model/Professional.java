package com.maisonglow.model;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a specialist of the business, with the treatments they provide and
 * their working hours. Scheduling rules are implemented by services.
 */
public class Professional extends Person {

    private String specialty;
    private LocalTime workStart;
    private LocalTime workEnd;
    private boolean active;
    private List<Treatment> treatments;

    public void setTreatments(List<Treatment> treatments) { this.treatments = new ArrayList<>(treatments); }
    public void setActive(boolean active) { this.active = active; }

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

    @Override
    public String getRoleDescription() {
        return "Profesional";
    }
}
