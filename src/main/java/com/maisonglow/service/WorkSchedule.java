package com.maisonglow.service;

import com.maisonglow.exception.ValidationException;

import java.time.LocalTime;

/**
 * The working day of a professional, as entered in the registration form.
 *
 * @param start when the working day starts
 * @param end   when it ends; must be after the start
 */
public record WorkSchedule(LocalTime start, LocalTime end) {

    /**
     * Checks that both hours are present and in order.
     *
     * @throws ValidationException if an hour is missing or the start is not before the end
     */
    public void validate() throws ValidationException {
        if (start == null || end == null) {
            throw new ValidationException("La jornada debe tener hora de inicio y de fin.");
        }
        if (!start.isBefore(end)) {
            throw new ValidationException("La hora de inicio de la jornada debe ser anterior a la de fin.");
        }
    }
}
