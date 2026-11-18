package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.UserAccount;
import com.maisonglow.model.UserRole;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Optional;

/** SQLite account DAO sharing the business transaction connection. */
public final class AccountDaoSqlite implements AccountDao {
    private final SqliteDatabase database;

    public AccountDaoSqlite(SqliteDatabase database) { this.database = database; }

    @Override public Optional<Credentials> findByUsername(String username) throws DataAccessException {
        return find("SELECT * FROM app_user WHERE lower(trim(username)) = ?", username);
    }

    @Override public Optional<Credentials> findById(int id) throws DataAccessException {
        return find("SELECT * FROM app_user WHERE id = ?", id);
    }

    private Optional<Credentials> find(String sql, Object value) throws DataAccessException {
        return database.transaction(c -> {
            try (var statement = c.prepareStatement(sql)) {
                statement.setObject(1, value);
                try (var rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(read(rows)) : Optional.empty();
                }
            }
        });
    }

    @Override public UserAccount create(String username, String hash, UserRole role, Integer customerId)
        throws DataAccessException {
        return database.transaction(c -> {
            try (var statement = c.prepareStatement(
                "INSERT INTO app_user(username,password_hash,role,customer_id) VALUES(?,?,?,?) RETURNING *")) {
                statement.setString(1, username);
                statement.setString(2, hash);
                statement.setString(3, role.name());
                if (customerId == null) statement.setNull(4, Types.INTEGER);
                else statement.setInt(4, customerId);
                try (var rows = statement.executeQuery()) { rows.next(); return read(rows).account(); }
            }
        });
    }

    @Override public long countAdministrators(boolean activeOnly) throws DataAccessException {
        return database.transaction(c -> {
            try (var statement = c.prepareStatement(
                "SELECT count(*) FROM app_user WHERE role='ADMIN' AND (? = 0 OR active=1)")) {
                statement.setBoolean(1, activeOnly);
                try (var rows = statement.executeQuery()) { rows.next(); return rows.getLong(1); }
            }
        });
    }

    @Override public void changePassword(int id, String hash) throws DataAccessException {
        update("UPDATE app_user SET password_hash=?,auth_version=auth_version+1 WHERE id=?", hash, id);
    }

    @Override public void setActive(int id, boolean active) throws DataAccessException {
        update("UPDATE app_user SET active=?,auth_version=auth_version+1 WHERE id=?", active ? 1 : 0, id);
    }

    private void update(String sql, Object value, int id) throws DataAccessException {
        database.transaction(c -> {
            try (var statement = c.prepareStatement(sql)) {
                statement.setObject(1, value);
                statement.setInt(2, id);
                if (statement.executeUpdate() != 1) throw new DataAccessException("La cuenta ya no existe.");
            }
            return null;
        });
    }

    private static Credentials read(ResultSet rows) throws SQLException {
        int customerId = rows.getInt("customer_id");
        Integer linkedCustomer = rows.wasNull() ? null : customerId;
        return new Credentials(new UserAccount(rows.getInt("id"), rows.getString("username"),
            UserRole.valueOf(rows.getString("role")), linkedCustomer, rows.getBoolean("active")),
            rows.getString("password_hash"), rows.getInt("auth_version"));
    }
}
