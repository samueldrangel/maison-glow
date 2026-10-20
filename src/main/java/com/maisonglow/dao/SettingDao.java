package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Setting;

import java.util.Map;

/**
 * Data access for the business settings (table {@code app_setting}). It does
 * not extend {@link CrudDao} because settings are identified by a text key.
 */
public interface SettingDao {

    /**
     * Returns every stored setting.
     *
     * @return the settings by key
     * @throws DataAccessException if the settings cannot be read
     */
    Map<String, Setting> loadAll() throws DataAccessException;

    /**
     * Stores a new value for a setting, creating it if it does not exist.
     *
     * @param key   the setting key
     * @param value the new value, as text
     * @throws DataAccessException if the setting cannot be stored
     */
    void save(String key, String value) throws DataAccessException;
}
