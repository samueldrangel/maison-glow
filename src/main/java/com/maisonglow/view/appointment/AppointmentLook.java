package com.maisonglow.view.appointment;

import com.maisonglow.model.AppointmentStatus;

/**
 * How the state of an appointment looks on the agenda: the word shown in its chip and the style of the chip.
 * Pending ones are gold because they wait for an answer, confirmed ones are green, and the ones that no longer
 * hold the schedule are quiet (completed in rose, cancelled in grey) except a no-show, which warns.
 */
public enum AppointmentLook {

    PENDING("Pendiente", "chip-gold"),
    CONFIRMED("Confirmada", "chip-success"),
    COMPLETED("Completada", "chip-rose"),
    CANCELLED("Cancelada", "chip-neutral"),
    NO_SHOW("Inasistencia", "chip-warning");

    private final String label;
    private final String chipStyle;

    AppointmentLook(String label, String chipStyle) {
        this.label = label;
        this.chipStyle = chipStyle;
    }

    /**
     * @param status the state of an appointment
     * @return how that state looks
     */
    public static AppointmentLook of(AppointmentStatus status) {
        return valueOf(status.name());
    }

    /** @return the word shown in the chip */
    public String label() {
        return label;
    }

    /** @return the style class of the chip */
    public String chipStyle() {
        return chipStyle;
    }
}
