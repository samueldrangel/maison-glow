package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Professional;

import java.util.List;
import java.util.Optional;

/**
 * Data access for professionals (table {@code professional}) and the
 * treatments each one offers (table {@code professional_treatment}).
 */
public interface ProfessionalDao extends CrudDao<Professional> {

    /**
     * Looks up a professional by identity document.
     *
     * @param document the document to search for
     * @return the matching professional, or an empty {@link Optional}
     * @throws DataAccessException if the lookup fails
     */
    Optional<Professional> findByDocument(String document) throws DataAccessException;

    /**
     * Returns the professionals who offer a treatment, active or not.
     *
     * @param treatmentId the treatment id
     * @return the professionals offering it
     * @throws DataAccessException if the professionals cannot be read
     */
    List<Professional> findByTreatment(int treatmentId) throws DataAccessException;
}
