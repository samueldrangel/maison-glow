package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Appointment;

import java.time.LocalDate;
import java.util.List;

/**
 * Data access for appointments (table {@code appointment}), with the
 * queries the schedule needs on top of the basic operations.
 */
public interface AppointmentDao extends CrudDao<Appointment> {

    /**
     * Returns the appointments of a professional, in every status.
     *
     * @param professionalId the professional's identifier
     * @return the appointments ordered by start time; empty if there are none
     * @throws DataAccessException if the appointments cannot be read
     */
    List<Appointment> findByProfessional(int professionalId) throws DataAccessException;

    /**
     * Returns the appointments that start on the given day, in every status.
     *
     * @param date the day
     * @return the appointments ordered by start time; empty if there are none
     * @throws DataAccessException if the appointments cannot be read
     */
    List<Appointment> findByDate(LocalDate date) throws DataAccessException;
}
