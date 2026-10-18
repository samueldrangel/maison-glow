package com.maisonglow.service.rule;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Appointment;
import com.maisonglow.service.SettingsService;

import java.util.Locale;
import java.util.Objects;

/**
 * Requires a minimum deposit when booking: a percentage of the treatment
 * price, or the full price for customers who reached the no-show threshold
 * (RF-26). Both values come from the business settings.
 */
public class DepositRule implements AppointmentRule {

    private static final double FULL_PERCENTAGE = 100;
    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");

    private final SettingsService settings;

    /**
     * Creates the rule.
     *
     * @param settings provides the deposit percentage and the no-show threshold
     */
    public DepositRule(SettingsService settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    @Override
    public void apply(Appointment appointment)
        throws BusinessRuleException, ValidationException, DataAccessException {
        double required = requiredDeposit(appointment);
        if (appointment.getDeposit() < required) {
            throw new BusinessRuleException(String.format(COLOMBIA,
                "La cita requiere un anticipo mínimo de $ %,.0f y se registraron $ %,.0f.",
                required, appointment.getDeposit()));
        }
    }

    /**
     * Calculates the minimum deposit of an appointment.
     *
     * @param appointment the appointment being booked
     * @return the minimum deposit
     * @throws ValidationException if a setting has an invalid value
     * @throws DataAccessException if the settings cannot be read
     */
    public double requiredDeposit(Appointment appointment) throws ValidationException, DataAccessException {
        double percentage = isPenalized(appointment) ? FULL_PERCENTAGE : settings.depositPercentage();
        return appointment.getTreatment().getPrice() * percentage / FULL_PERCENTAGE;
    }

    @Override
    public String getName() {
        return "Anticipo mínimo";
    }

    private boolean isPenalized(Appointment appointment) throws ValidationException, DataAccessException {
        return appointment.getCustomer().getNoShowCount() >= settings.noShowPenaltyThreshold();
    }
}
