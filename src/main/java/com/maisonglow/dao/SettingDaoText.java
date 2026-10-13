package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Setting;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores the business settings in a text file, one per line with the fields
 * {@code key|value|description}. Reuses the escaping rules of {@link TextFileDao}.
 */
public class SettingDaoText implements SettingDao {

    private static final int FIELD_COUNT = 3;

    private final Path file;
    private Map<String, Setting> settings;

    /**
     * Creates the DAO over the given file.
     *
     * @param file the settings file, usually {@code data/settings.txt}
     */
    public SettingDaoText(Path file) {
        this.file = file;
    }

    @Override
    public Map<String, Setting> loadAll() throws DataAccessException {
        ensureLoaded();
        return Collections.unmodifiableMap(settings);
    }

    @Override
    public void save(String key, String value) throws DataAccessException {
        ensureLoaded();
        Setting setting = settings.get(key);
        if (setting == null) {
            settings.put(key, new Setting(key, value, ""));
        } else {
            setting.setValue(value);
        }
        write();
    }

    private void ensureLoaded() throws DataAccessException {
        if (settings != null) {
            return;
        }
        settings = new LinkedHashMap<>();
        for (String line : TextFileDao.readLines(file)) {
            String[] fields = TextFileDao.splitFields(line);
            if (fields.length != FIELD_COUNT) {
                throw new DataAccessException("Línea de configuración inválida: " + line);
            }
            settings.put(fields[0], new Setting(fields[0], fields[1], fields[2]));
        }
    }

    private void write() throws DataAccessException {
        List<String> lines = new ArrayList<>();
        for (Setting setting : settings.values()) {
            lines.add(TextFileDao.joinFields(setting.getKey(), setting.getValue(), setting.getDescription()));
        }
        TextFileDao.writeLines(file, lines);
    }
}
