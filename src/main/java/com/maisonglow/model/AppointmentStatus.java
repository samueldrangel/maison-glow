package com.maisonglow.model;

/**
 * Lists the stages in the life cycle of an appointment.
 */
public enum AppointmentStatus {

    /** The appointment was booked and is waiting for confirmation. */
    PENDING,

    /** The customer confirmed the appointment. */
    CONFIRMED,

    /** The service was provided. */
    COMPLETED,

    /** The appointment was cancelled before taking place. */
    CANCELLED,

    /** The customer did not show up. */
    NO_SHOW;

    /**
     * Indicates whether an appointment in this status still occupies the
     * professional's schedule.
     *
     * @return {@code true} if the status is {@link #PENDING} or {@link #CONFIRMED}
     */
    public boolean blocksSchedule() {
        return this == PENDING || this == CONFIRMED;
    }
}
