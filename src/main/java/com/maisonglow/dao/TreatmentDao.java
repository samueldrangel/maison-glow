package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Treatment;

import java.util.Optional;

/**
 * Data access for the treatment catalog (table {@code treatment}).
 */
public interface TreatmentDao extends CrudDao<Treatment> {

    /**
     * Looks up a treatment by name, ignoring case.
     *
     * @param name the name to search for
     * @return the matching treatment, or an empty {@link Optional}
     * @throws DataAccessException if the lookup fails
     */
    Optional<Treatment> findByName(String name) throws DataAccessException;
}
