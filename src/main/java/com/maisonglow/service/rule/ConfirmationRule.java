package com.maisonglow.service.rule;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Appointment;
import com.maisonglow.service.SettingsService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Requires a minimum notice between now and the start of the appointment, so
 * the salon has time to confirm it with the customer (RF-26). The notice in
 * hours comes from the business settings.
 */
public class ConfirmationRule implements AppointmentRule {

    private final SettingsService settings;
    private final Clock clock;

    /**
     * Creates the rule.
     *
     * @param settings provides the minimum notice in hours
     * @param clock    gives the current time; tests pass a fixed clock
     */
    public ConfirmationRule(SettingsService settings, Clock clock) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void apply(Appointment appointment)
        throws BusinessRuleException, ValidationException, DataAccessException {
        int noticeHours = settings.minBookingNoticeHours();
        LocalDateTime earliest = LocalDateTime.now(clock).plusHours(noticeHours);
        if (appointment.getStart().isBefore(earliest)) {
            throw new BusinessRuleException("Las citas se agendan con al menos " + noticeHours
                + " horas de anticipación.");
        }
    }

    @Override
    public String getName() {
        return "Aviso mínimo";
    }
}
