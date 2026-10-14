package com.maisonglow.dao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * Test double for {@link CrudDao}: keeps the entities in a map, keyed by the
 * id they already have. Useful to test a DAO that depends on other DAOs.
 *
 * @param <T> the type of entity
 */
class InMemoryDao<T> implements CrudDao<T> {

    private final Map<Integer, T> entities = new LinkedHashMap<>();
    private final ToIntFunction<T> idOf;

    @SafeVarargs
    InMemoryDao(ToIntFunction<T> idOf, T... initial) {
        this.idOf = idOf;
        for (T entity : initial) {
            create(entity);
        }
    }

    @Override
    public T create(T entity) {
        entities.put(idOf.applyAsInt(entity), entity);
        return entity;
    }

    @Override
    public Optional<T> findById(int id) {
        return Optional.ofNullable(entities.get(id));
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(entities.values());
    }

    @Override
    public void update(T entity) {
        create(entity);
    }

    @Override
    public void delete(int id) {
        entities.remove(id);
    }
}
