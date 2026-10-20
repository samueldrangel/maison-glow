package com.maisonglow.service.rule;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Appointment;
import com.maisonglow.service.SettingsService;

import java.util.Objects;

/**
 * Schedules the reminder of an appointment a configurable number of hours
 * before it starts (RF-26). When an appointment is moved, the service applies
 * the rules again and the reminder follows the new start.
 */
public class ReminderRule implements AppointmentRule {

    private final SettingsService settings;

    /**
     * Creates the rule.
     *
     * @param settings provides how many hours before the start the reminder is due
     */
    public ReminderRule(SettingsService settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    @Override
    public void apply(Appointment appointment) throws ValidationException, DataAccessException {
        appointment.setReminderAt(appointment.getStart().minusHours(settings.reminderHoursBefore()));
    }

    @Override
    public String getName() {
        return "Recordatorio";
    }
}
