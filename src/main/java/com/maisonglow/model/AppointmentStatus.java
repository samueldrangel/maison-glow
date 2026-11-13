package com.maisonglow.model;

/**
 * Lists appointment states. Transition rules belong to AppointmentService.
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
     * Returns the Spanish name of the status, for messages shown to the user.
     *
     * @return the label, such as "pendiente"
     */
    public String getLabel() {
        return label;
    }
}
