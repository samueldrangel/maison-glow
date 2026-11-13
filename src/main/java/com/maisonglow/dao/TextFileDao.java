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
 * <p>The first line of the file stores the next id to assign
 * ({@code #nextId=N}), so the id of a deleted record is never given to a
 * new one, as with {@code AUTOINCREMENT} in {@code sql/schema.sql}.</p>
 *
 * <p>Create only one instance per file, at application start-up: each
 * instance keeps serialized snapshots, never caller-owned entities. Reads
 * rebuild detached objects, so editing one cannot change stored data until
 * {@link #update(Object)} succeeds.</p>
 *
 * @param <T> the type of entity stored in the file
 */
public abstract class TextFileDao<T> implements CrudDao<T> {

    private static final int UNSAVED_ID = 0;
    private static final int FIRST_ID = 1;
    private static final String NEXT_ID_HEADER = "#nextId=";

    private final TextFileStore store;
    private final NavigableMap<Integer, String[]> entities = new TreeMap<>();
    private int nextId = FIRST_ID;
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

    /**
     * Finds the entity that a record references by id, for subclasses that
     * rebuild related entities with other DAOs (ADR-0012).
     *
     * @param dao        where the related entity is stored
     * @param idField    the id as written in the record
     * @param entityName the Spanish name of the related entity, for the error message
     * @param <E>        the type of the related entity
     * @return the related entity
     * @throws DataAccessException if no entity has that id or it cannot be read
     */
    protected static <E> E findRelated(CrudDao<E> dao, String idField, String entityName)
        throws DataAccessException {
        int id = Integer.parseInt(idField);
        return dao.findById(id).orElseThrow(() ->
            new DataAccessException("No existe el registro de " + entityName + " con id " + id
                + " al que se hace referencia."));
    }

    /**
     * {@inheritDoc}
     *
     * @throws DataAccessException also if the entity already has an id, that
     *                             is, if it was stored before
     */
    @Override
    public T create(T entity) throws DataAccessException {
        ensureLoaded();
        requireUnsaved(entity);
        int id = nextId;
        setId(entity, id);
        try {
            entities.put(id, toFields(entity).clone());
            nextId++;
            save();
        } catch (DataAccessException | RuntimeException e) {
            entities.remove(id); setId(entity, UNSAVED_ID); nextId = id;
            if (e instanceof DataAccessException dataError) throw dataError;
            throw new DataAccessException("No se pudo convertir el registro.", e);
        }
        return entity;
    }

    @Override
    public Optional<T> findById(int id) throws DataAccessException {
        ensureLoaded();
        String[] fields = entities.get(id);
        return fields == null ? Optional.empty() : Optional.of(fromFields(fields.clone()));
    }

    @Override
    public List<T> findAll() throws DataAccessException {
        ensureLoaded();
        List<T> result = new ArrayList<>();
        for (String[] fields : entities.values()) result.add(fromFields(fields.clone()));
        return result;
    }

    @Override
    public void update(T entity) throws DataAccessException {
        ensureLoaded();
        int id = getId(entity);
        String[] previous = requireExisting(id);
        try { entities.put(id, toFields(entity).clone()); }
        catch (RuntimeException e) { throw new DataAccessException("No se pudo convertir el registro.", e); }
        saveOrUndo(() -> entities.put(id, previous));
    }

    @Override
    public void delete(int id) throws DataAccessException {
        ensureLoaded();
        String[] removed = requireExisting(id);
        entities.remove(id);
        saveOrUndo(() -> entities.put(id, removed));
    }

    private void requireUnsaved(T entity) throws DataAccessException {
        int id = getId(entity);
        if (id != UNSAVED_ID) {
            throw new DataAccessException("El registro ya tiene el id " + id + "; use update para modificarlo.");
        }
    }

    private String[] requireExisting(int id) throws DataAccessException {
        String[] entity = entities.get(id);
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
        nextId = FIRST_ID;
        List<String> lines = store.readLines();
        for (int i = 0; i < lines.size(); i++) {
            loadLine(lines.get(i), i + 1);
        }
        loaded = true;
    }

    private void loadLine(String line, int lineNumber) throws DataAccessException {
        if (line.isBlank()) {
            return;
        }
        if (line.startsWith(NEXT_ID_HEADER)) {
            nextId = Math.max(nextId, parseNextId(line, lineNumber));
            return;
        }
        T entity = parseRecord(line, lineNumber);
        int id = getId(entity);
        if (id < FIRST_ID || entities.containsKey(id)) {
            throw invalidLine(lineNumber, "el id " + id + " está repetido o no es válido", null);
        }
        entities.put(id, TextLineCodec.decode(line));
        nextId = Math.max(nextId, id + 1);
    }

    private int parseNextId(String line, int lineNumber) throws DataAccessException {
        try {
            return Integer.parseInt(line.substring(NEXT_ID_HEADER.length()).trim());
        } catch (NumberFormatException e) {
            throw invalidLine(lineNumber, e.getMessage(), e);
        }
    }

    private T parseRecord(String line, int lineNumber) throws DataAccessException {
        try {
            return fromFields(TextLineCodec.decode(line));
        } catch (DataAccessException | RuntimeException e) {
            throw invalidLine(lineNumber, e.getMessage(), e);
        }
    }

    private DataAccessException invalidLine(int lineNumber, String reason, Exception cause) {
        return new DataAccessException(
            "La línea " + lineNumber + " de " + store.getFile() + " no es válida: " + reason, cause);
    }

    private void saveOrUndo(Runnable undo) throws DataAccessException {
        try {
            save();
        } catch (DataAccessException e) {
            undo.run();
            throw e;
        } catch (RuntimeException e) {
            undo.run();
            throw new DataAccessException("No se pudo convertir un registro para guardarlo en " + store.getFile(), e);
        }
    }

    private void save() throws DataAccessException {
        List<String> lines = new ArrayList<>(entities.size() + 1);
        lines.add(NEXT_ID_HEADER + nextId);
        for (String[] fields : entities.values()) {
            lines.add(TextLineCodec.encode(fields));
        }
        store.writeLines(lines);
    }
}
