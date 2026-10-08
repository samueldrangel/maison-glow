package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;

import java.util.List;
import java.util.Optional;

/**
 * Defines the basic persistence operations shared by every data access class.
 * Services depend on this contract rather than on a concrete storage
 * technology, so the text-file implementations can be replaced by JDBC ones
 * without changing business logic.
 *
 * @param <T> the type of entity managed by the implementation
 */
public interface CrudDao<T> {

    /**
     * Stores a new entity and returns it with its identifier assigned.
     *
     * @param entity the entity to store
     * @return the stored entity, with its generated id
     * @throws DataAccessException if the entity cannot be stored
     */
    T create(T entity) throws DataAccessException;

    /**
     * Looks up an entity by its identifier.
     *
     * @param id the identifier to search for
     * @return the matching entity, or an empty {@link Optional} if none exists
     * @throws DataAccessException if the lookup fails
     */
    Optional<T> findById(int id) throws DataAccessException;

    /**
     * Returns every stored entity.
     *
     * @return the list of all entities; empty if there are none
     * @throws DataAccessException if the entities cannot be read
     */
    List<T> findAll() throws DataAccessException;

    /**
     * Replaces the stored data of an existing entity.
     *
     * @param entity the entity carrying the updated data
     * @throws DataAccessException if the entity does not exist or cannot be updated
     */
    void update(T entity) throws DataAccessException;

    /**
     * Removes the entity with the given identifier.
     *
     * @param id the identifier of the entity to remove
     * @throws DataAccessException if the entity does not exist or cannot be removed
     */
    void delete(int id) throws DataAccessException;
}
