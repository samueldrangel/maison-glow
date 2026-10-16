package com.maisonglow.service.rule;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Appointment;

/**
 * A configurable business rule that every appointment must pass before it is
 * stored, such as a minimum deposit or a minimum booking notice (ADR-0007).
 * New rules are added as new classes, without changing the service that
 * applies them.
 *
 * <p>The schedule conflict is not a rule: it is an invariant checked by the
 * appointment service itself.</p>
 */
public interface AppointmentRule {

    /**
     * Checks the appointment or completes it, for example by setting its
     * reminder time.
     *
     * @param appointment the appointment about to be stored
     * @throws BusinessRuleException if the appointment breaks the rule
     * @throws ValidationException   if a business parameter the rule needs is invalid
     * @throws DataAccessException   if a business parameter cannot be read
     */
    void apply(Appointment appointment) throws BusinessRuleException, ValidationException, DataAccessException;

    /**
     * Returns the name of the rule, for messages shown to the user.
     *
     * @return the rule name, such as "Anticipo mínimo"
     */
    String getName();
}
