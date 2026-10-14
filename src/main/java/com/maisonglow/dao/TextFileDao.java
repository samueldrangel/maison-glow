package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Base class for the data access objects that keep their entities in a text
 * file, one record per line. It keeps the entities in memory ordered by id,
 * assigns the ids of new entities and saves the whole file after every
 * change; if saving fails, the change is undone in memory as well.
 *
 * <p>Each subclass only says how to turn its entity into fields and back,
 * and how to read and assign its id. The file is loaded on the first
 * operation instead of in the constructor, so a subclass can use its own
 * fields (for example, other DAOs it receives) while converting records.</p>
 *
 * @param <T> the type of entity stored in the file
 */
public abstract class TextFileDao<T> implements CrudDao<T> {

    private static final int UNSAVED_ID = 0;

    private final TextFileStore store;
    private final NavigableMap<Integer, T> entities = new TreeMap<>();
    private boolean loaded;

    /**
     * Creates a DAO backed by the given file. The file does not need to exist.
     *
     * @param file the path of the data file
     */
    protected TextFileDao(Path file) {
        this.store = new TextFileStore(file);
    }

    /**
     * Converts an entity into the field values of its record, in the same
     * order as the columns of its table in {@code sql/schema.sql}.
     *
     * @param entity the entity to convert
     * @return the field values
     */
    protected abstract String[] toFields(T entity);

    /**
     * Rebuilds an entity from the field values of its record.
     *
     * @param fields the field values, in the order written by {@link #toFields(Object)}
     * @return the rebuilt entity
     * @throws DataAccessException if a related entity cannot be loaded
     */
    protected abstract T fromFields(String[] fields) throws DataAccessException;

    /**
     * Returns the identifier of an entity.
     *
     * @param entity the entity
     * @return its identifier
     */
    protected abstract int getId(T entity);

    /**
     * Assigns the identifier of an entity.
     *
     * @param entity the entity
     * @param id     the identifier to assign
     */
    protected abstract void setId(T entity, int id);

    @Override
    public T create(T entity) throws DataAccessException {
        ensureLoaded();
        int id = nextId();
        setId(entity, id);
        entities.put(id, entity);
        saveOrUndo(() -> {
            entities.remove(id);
            setId(entity, UNSAVED_ID);
        });
        return entity;
    }

    @Override
    public Optional<T> findById(int id) throws DataAccessException {
        ensureLoaded();
        return Optional.ofNullable(entities.get(id));
    }

    @Override
    public List<T> findAll() throws DataAccessException {
        ensureLoaded();
        return new ArrayList<>(entities.values());
    }

    @Override
    public void update(T entity) throws DataAccessException {
        ensureLoaded();
        int id = getId(entity);
        T previous = requireExisting(id);
        entities.put(id, entity);
        saveOrUndo(() -> entities.put(id, previous));
    }

    @Override
    public void delete(int id) throws DataAccessException {
        ensureLoaded();
        T removed = requireExisting(id);
        entities.remove(id);
        saveOrUndo(() -> entities.put(id, removed));
    }

    private int nextId() {
        return entities.isEmpty() ? UNSAVED_ID + 1 : entities.lastKey() + 1;
    }

    private T requireExisting(int id) throws DataAccessException {
        T entity = entities.get(id);
        if (entity == null) {
            throw new DataAccessException("No existe un registro con id " + id + " en " + store.getFile());
        }
        return entity;
    }

    private void ensureLoaded() throws DataAccessException {
        if (loaded) {
            return;
        }
        entities.clear();
        List<String> lines = store.readLines();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.isBlank()) {
                T entity = parseLine(line, i + 1);
                entities.put(getId(entity), entity);
            }
        }
        loaded = true;
    }

    private T parseLine(String line, int lineNumber) throws DataAccessException {
        try {
            return fromFields(TextLineCodec.decode(line));
        } catch (RuntimeException e) {
            throw new DataAccessException(
                "La línea " + lineNumber + " de " + store.getFile() + " no es válida: " + e.getMessage(), e);
        }
    }

    private void saveOrUndo(Runnable undo) throws DataAccessException {
        try {
            save();
        } catch (DataAccessException e) {
            undo.run();
            throw e;
        }
    }

    private void save() throws DataAccessException {
        List<String> lines = new ArrayList<>(entities.size());
        for (T entity : entities.values()) {
            lines.add(TextLineCodec.encode(toFields(entity)));
        }
        store.writeLines(lines);
    }
}
