package com.maisonglow.model;

/**
 * A business parameter stored as text, such as the deposit percentage or the
 * reminder time (table {@code app_setting}). The key never changes.
 */
public class Setting {

    private final String key;
    private String value;
    private final String description;

    /**
     * Creates a setting.
     *
     * @param key         the unique key, such as {@code deposit_percentage}
     * @param value       the value, as text
     * @param description what the setting controls
     */
    public Setting(String key, String value, String description) {
        this.key = key;
        this.value = value;
        this.description = description;
    }

    /**
     * Returns the unique key.
     *
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the value as text.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Changes the value.
     *
     * @param value the new value, as text
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns what the setting controls.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return key + " = " + value;
    }
}
