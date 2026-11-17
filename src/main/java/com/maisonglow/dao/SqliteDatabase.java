package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** File-backed SQLite connections, schema bootstrap and nested transaction boundary. */
public final class SqliteDatabase {
    private final Path file;
    private final ThreadLocal<Connection> current = new ThreadLocal<>();

    public SqliteDatabase(Path file) { this.file = file.toAbsolutePath(); }
    public Path file() { return file; }

    @FunctionalInterface
    public interface Work<T> { T run(Connection connection) throws SQLException, DataAccessException; }

    /** Runs all operations on this thread in one transaction; rollback preserves the original failure. */
    public <T> T transaction(Work<T> work) throws DataAccessException {
        if (current.get() != null) {
            try { return work.run(current.get()); }
            catch (SQLException e) { throw failure(e); }
        }
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file)) {
            try (var statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys=ON");
                statement.execute("PRAGMA busy_timeout=5000");
            }
            connection.setAutoCommit(false);
            current.set(connection);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | DataAccessException | RuntimeException e) {
                try { connection.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                if (e instanceof DataAccessException dataError) throw dataError;
                if (e instanceof SQLException sqlError) throw failure(sqlError);
                throw e;
            } finally { current.remove(); }
        } catch (SQLException e) { throw failure(e); }
    }

    /** Creates missing folders and tables. Scripts are bundled in the application JAR. */
    public void initialize() throws DataAccessException {
        try { Files.createDirectories(file.getParent()); }
        catch (IOException e) { throw new DataAccessException("No se pudo crear la carpeta de la base de datos.", e); }
        transaction(c -> {
            script(c, "schema.sql");
            migrateAccounts(c);
            return null;
        });
    }

    /** Additive versioned migration; a failed upgrade rolls back without removing existing data. */
    private void migrateAccounts(Connection connection) throws SQLException, DataAccessException {
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS schema_migration (version INTEGER PRIMARY KEY, applied_at TEXT NOT NULL)");
            try (var rows = statement.executeQuery("SELECT COALESCE(MAX(version),0) FROM schema_migration")) {
                rows.next();
                int version = rows.getInt(1);
                if (version > 1) throw new DataAccessException("La base requiere una versión más reciente de Maison Glow.");
                if (version == 1) return;
            }
            boolean hasVersion = false;
            try (var rows = statement.executeQuery("PRAGMA table_info(app_user)")) {
                while (rows.next()) if ("auth_version".equals(rows.getString("name"))) hasVersion = true;
            }
            if (!hasVersion) statement.execute("ALTER TABLE app_user ADD COLUMN auth_version INTEGER NOT NULL DEFAULT 0 CHECK (auth_version >= 0)");
            statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_app_user_normalized_username ON app_user(lower(trim(username)))");
            statement.execute("INSERT INTO schema_migration VALUES(1, strftime('%Y-%m-%dT%H:%M:%S','now'))");
        }
    }

    public void runScript(String resource) throws DataAccessException {
        transaction(c -> { script(c, resource); return null; });
    }

    private void script(Connection c, String name) throws DataAccessException, SQLException {
        String source;
        try (InputStream input = SqliteDatabase.class.getResourceAsStream("/sql/" + name)) {
            if (input == null) throw new IOException("Falta el recurso SQL: " + name);
            source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) { throw new DataAccessException("No se pudo cargar el esquema de SQLite.", e); }
        StringBuilder sql = new StringBuilder();
        boolean trigger = false;
        try (var statement = c.createStatement()) {
            for (String line : source.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;
                if (trimmed.startsWith("CREATE TRIGGER")) trigger = true;
                sql.append(line).append('\n');
                if (trimmed.endsWith(";") && (!trigger || trimmed.equals("END;"))) {
                    statement.execute(sql.toString());
                    sql.setLength(0);
                    trigger = false;
                }
            }
            if (!sql.toString().isBlank()) throw new DataAccessException("Script SQL incompleto: " + name);
        }
    }

    private DataAccessException failure(SQLException e) {
        String detail = String.valueOf(e.getMessage());
        String message = detail.contains("professional_schedule_conflict")
            ? "El profesional ya tiene una cita en ese horario."
            : detail.contains("FOREIGN KEY") ? "No se puede guardar o eliminar el registro porque tiene datos relacionados."
            : detail.contains("UNIQUE") ? "Ya existe un registro con esos datos."
            : "No se pudo consultar o guardar la información en la base de datos. Comprueba que esté disponible.";
        return new DataAccessException(message, e);
    }
}
