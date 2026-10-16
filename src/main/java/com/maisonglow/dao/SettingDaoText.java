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
 * {@code key|value|description}. Reads and writes the file with
 * {@link TextFileStore} and encodes each line with {@link TextLineCodec}.
 */
public class SettingDaoText implements SettingDao {

    private static final int FIELD_COUNT = 3;

    private final TextFileStore store;
    private Map<String, Setting> settings;

    /**
     * Creates the DAO over the given file.
     *
     * @param file the settings file, usually {@code data/settings.txt}
     */
    public SettingDaoText(Path file) {
        this.store = new TextFileStore(file);
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
        for (String line : store.readLines()) {
            String[] fields = TextLineCodec.decode(line);
            if (fields.length != FIELD_COUNT) {
                throw new DataAccessException("Línea de configuración inválida: " + line);
            }
            settings.put(fields[0], new Setting(fields[0], fields[1], fields[2]));
        }
    }

    private void write() throws DataAccessException {
        List<String> lines = new ArrayList<>();
        for (Setting setting : settings.values()) {
            lines.add(TextLineCodec.encode(setting.getKey(), setting.getValue(), setting.getDescription()));
        }
        store.writeLines(lines);
    }
}
