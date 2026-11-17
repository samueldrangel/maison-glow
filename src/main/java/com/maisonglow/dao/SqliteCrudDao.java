package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Prepared-statement CRUD; reads reconstruct detached objects from current database rows. */
abstract class SqliteCrudDao<T> implements CrudDao<T> {
    protected final SqliteDatabase database;
    private final String table;
    private final String[] columns;

    SqliteCrudDao(SqliteDatabase database, String table, String... columns) {
        this.database = database; this.table = table; this.columns = columns;
    }
    protected abstract String[] toFields(T entity);
    protected abstract T fromFields(String[] fields) throws DataAccessException;
    protected abstract int getId(T entity);
    protected abstract void setId(T entity, int id);
    protected void saveRelations(Connection c, T entity) throws SQLException, DataAccessException { }

    @Override public T create(T entity) throws DataAccessException {
        if (getId(entity) != 0) throw new DataAccessException("El registro ya tiene un identificador.");
        try {
            return database.transaction(c -> {
                String names = String.join(",", Arrays.copyOfRange(columns, 1, columns.length));
                String slots = String.join(",", java.util.Collections.nCopies(columns.length - 1, "?"));
                try (var statement = c.prepareStatement("INSERT INTO " + table + " (" + names + ") VALUES (" + slots + ")")) {
                    String[] fields = toFields(entity);
                    for (int i = 1; i < fields.length; i++) statement.setObject(i, nullable(columns[i], fields[i]));
                    statement.executeUpdate();
                }
                try (var statement = c.createStatement(); var row = statement.executeQuery("SELECT last_insert_rowid()")) {
                    row.next(); setId(entity, row.getInt(1));
                }
                saveRelations(c, entity);
                return entity;
            });
        } catch (DataAccessException | RuntimeException e) { setId(entity, 0); throw e; }
    }

    @Override public Optional<T> findById(int id) throws DataAccessException {
        return query(" WHERE id=?", id).stream().findFirst();
    }
    @Override public List<T> findAll() throws DataAccessException { return query(" ORDER BY id"); }

    protected List<T> query(String suffix, Object... values) throws DataAccessException {
        return database.transaction(c -> {
            List<T> result = new ArrayList<>();
            try (var statement = c.prepareStatement("SELECT " + String.join(",", columns) + " FROM " + table + suffix)) {
                for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
                try (var rows = statement.executeQuery()) {
                    while (rows.next()) {
                        String[] fields = new String[columns.length];
                        for (int i = 0; i < fields.length; i++) {
                            fields[i] = rows.getString(i + 1);
                            if (fields[i] == null) fields[i] = "";
                        }
                        result.add(fromFields(fields));
                    }
                }
            }
            return result;
        });
    }

    @Override public void update(T entity) throws DataAccessException {
        database.transaction(c -> {
            String assignments = Arrays.stream(columns).skip(1).map(name -> name + "=?").collect(Collectors.joining(","));
            try (var statement = c.prepareStatement("UPDATE " + table + " SET " + assignments + " WHERE id=?")) {
                String[] fields = toFields(entity);
                for (int i = 1; i < fields.length; i++) statement.setObject(i, nullable(columns[i], fields[i]));
                statement.setInt(fields.length, getId(entity));
                if (statement.executeUpdate() != 1) throw new DataAccessException("No existe el registro que intentas modificar.");
            }
            saveRelations(c, entity);
            return null;
        });
    }
    @Override public void delete(int id) throws DataAccessException {
        database.transaction(c -> {
            try (var statement = c.prepareStatement("DELETE FROM " + table + " WHERE id=?")) {
                statement.setInt(1, id);
                if (statement.executeUpdate() != 1) throw new DataAccessException("No existe el registro que intentas eliminar.");
            }
            return null;
        });
    }
    private Object nullable(String column, String value) {
        return value.isEmpty() && (column.equals("reminder_at") || column.equals("appointment_id")) ? null : value;
    }
    protected static <E> E findRelated(CrudDao<E> dao, String id, String name) throws DataAccessException {
        return dao.findById(Integer.parseInt(id)).orElseThrow(() -> new DataAccessException("No existe el registro de " + name + "."));
    }
}
