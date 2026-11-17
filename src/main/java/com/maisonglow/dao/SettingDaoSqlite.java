package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Setting;
import java.util.LinkedHashMap;
import java.util.Map;

/** Settings read from SQLite without an in-memory snapshot. */
public final class SettingDaoSqlite implements SettingDao {
    private final SqliteDatabase database;
    public SettingDaoSqlite(SqliteDatabase database) { this.database = database; }
    @Override public Map<String, Setting> loadAll() throws DataAccessException {
        return database.transaction(c -> {
            Map<String, Setting> settings = new LinkedHashMap<>();
            try (var statement = c.createStatement(); var rows = statement.executeQuery("SELECT * FROM app_setting ORDER BY setting_key")) {
                while (rows.next()) settings.put(rows.getString("setting_key"), new Setting(rows.getString("setting_key"),
                    rows.getString("setting_value"), rows.getString("description")));
            }
            return settings;
        });
    }
    @Override public void save(String key, String value) throws DataAccessException {
        database.transaction(c -> {
            try (var statement = c.prepareStatement("INSERT INTO app_setting(setting_key,setting_value,description) VALUES (?,?,'') ON CONFLICT(setting_key) DO UPDATE SET setting_value=excluded.setting_value")) {
                statement.setString(1,key); statement.setString(2,value); statement.executeUpdate();
            }
            return null;
        });
    }
}
