package com.maisonglow.model;

/**
 * Lists the stages in the life cycle of an appointment and which changes
 * between them are allowed. Only an active appointment (pending or
 * confirmed) can change; completed, cancelled and no-show are final.
 */
public enum AppointmentStatus {

    /** The appointment was booked and is waiting for confirmation. */
    PENDING("pendiente"),

    /** The customer confirmed the appointment. */
    CONFIRMED("confirmada"),

    /** The treatment was provided. */
    COMPLETED("completada"),

    /** The appointment was cancelled before taking place. */
    CANCELLED("cancelada"),

    /** The customer did not show up. */
    NO_SHOW("marcada como inasistencia");

    private final String label;

    AppointmentStatus(String label) {
        this.label = label;
    }

    /**
     * Indicates whether an appointment in this status still occupies the
     * professional's schedule.
     *
     * @return {@code true} if the status is {@link #PENDING} or {@link #CONFIRMED}
     */
    public boolean blocksSchedule() {
        return this == PENDING || this == CONFIRMED;
    }

    /**
     * Indicates whether an appointment in this status may move to the given one.
     * Only a pending appointment can be confirmed; any active appointment can
     * be completed, cancelled or marked as a no-show.
     *
     * @param next the status to move to
     * @return {@code true} if the change is allowed
     */
    public boolean canChangeTo(AppointmentStatus next) {
        return switch (next) {
            case PENDING -> false;
            case CONFIRMED -> this == PENDING;
            case COMPLETED, CANCELLED, NO_SHOW -> blocksSchedule();
        };
    }

    /**
     * Returns the Spanish name of the status, for messages shown to the user.
     *
     * @return the label, such as "pendiente"
     */
    public String getLabel() {
        return label;
    }
}
