package com.maisonglow.service;

import com.maisonglow.dao.SettingDao;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Setting;

import java.util.Map;

/**
 * Gives typed access to the business parameters, so that rules and
 * statistics never use hard-coded numbers. Every value is validated when
 * read; changing the settings file changes the behavior without touching code.
 */
public class SettingsService {

    /** Name shown on invoices and screens. */
    public static final String BUSINESS_NAME = "business_name";
    /** Percentage of the service price required as deposit. */
    public static final String DEPOSIT_PERCENTAGE = "deposit_percentage";
    /** Minimum hours between booking and the appointment start. */
    public static final String MIN_BOOKING_NOTICE_HOURS = "min_booking_notice_hours";
    /** Hours before an appointment when the reminder is generated. */
    public static final String REMINDER_HOURS_BEFORE = "reminder_hours_before";
    /** No-shows after which the full price is required in advance. */
    public static final String NO_SHOW_PENALTY_THRESHOLD = "no_show_penalty_threshold";
    /** Days of sales history used to detect low-rotation products. */
    public static final String LOW_ROTATION_WINDOW_DAYS = "low_rotation_window_days";
    /** How many low-rotation products are reported. */
    public static final String LOW_ROTATION_TOP_N = "low_rotation_top_n";
    /** Discount offered for low-rotation products. */
    public static final String LOW_ROTATION_DISCOUNT_PERCENTAGE = "low_rotation_discount_percentage";

    private static final double MAX_PERCENTAGE = 100;

    private final SettingDao settingDao;

    /**
     * Creates the service.
     *
     * @param settingDao the settings storage
     */
    public SettingsService(SettingDao settingDao) {
        this.settingDao = settingDao;
    }

    /**
     * Returns the business name.
     *
     * @return the name shown on invoices
     * @throws ValidationException if the setting is missing or blank
     * @throws DataAccessException if the settings cannot be read
     */
    public String businessName() throws ValidationException, DataAccessException {
        return read(BUSINESS_NAME);
    }

    /**
     * Returns the deposit required when booking, as a percentage of the service price.
     *
     * @return a value between 0 and 100
     * @throws ValidationException if the setting is missing or out of range
     * @throws DataAccessException if the settings cannot be read
     */
    public double depositPercentage() throws ValidationException, DataAccessException {
        return percentage(DEPOSIT_PERCENTAGE);
    }

    /**
     * Returns the minimum hours between booking and the appointment start.
     *
     * @return zero or more hours
     * @throws ValidationException if the setting is missing or negative
     * @throws DataAccessException if the settings cannot be read
     */
    public int minBookingNoticeHours() throws ValidationException, DataAccessException {
        return wholeNumber(MIN_BOOKING_NOTICE_HOURS, 0);
    }

    /**
     * Returns how many hours before an appointment the reminder is generated.
     *
     * @return zero or more hours
     * @throws ValidationException if the setting is missing or negative
     * @throws DataAccessException if the settings cannot be read
     */
    public int reminderHoursBefore() throws ValidationException, DataAccessException {
        return wholeNumber(REMINDER_HOURS_BEFORE, 0);
    }

    /**
     * Returns the number of no-shows after which a customer must pay the full price in advance.
     *
     * @return one or more no-shows
     * @throws ValidationException if the setting is missing or lower than one
     * @throws DataAccessException if the settings cannot be read
     */
    public int noShowPenaltyThreshold() throws ValidationException, DataAccessException {
        return wholeNumber(NO_SHOW_PENALTY_THRESHOLD, 1);
    }

    /**
     * Returns the days of sales history used to detect low-rotation products.
     *
     * @return one or more days
     * @throws ValidationException if the setting is missing or lower than one
     * @throws DataAccessException if the settings cannot be read
     */
    public int lowRotationWindowDays() throws ValidationException, DataAccessException {
        return wholeNumber(LOW_ROTATION_WINDOW_DAYS, 1);
    }

    /**
     * Returns how many low-rotation products are reported to the administrator.
     *
     * @return one or more products
     * @throws ValidationException if the setting is missing or lower than one
     * @throws DataAccessException if the settings cannot be read
     */
    public int lowRotationTopN() throws ValidationException, DataAccessException {
        return wholeNumber(LOW_ROTATION_TOP_N, 1);
    }

    /**
     * Returns the discount offered for low-rotation products.
     *
     * @return a value between 0 and 100
     * @throws ValidationException if the setting is missing or out of range
     * @throws DataAccessException if the settings cannot be read
     */
    public double lowRotationDiscountPercentage() throws ValidationException, DataAccessException {
        return percentage(LOW_ROTATION_DISCOUNT_PERCENTAGE);
    }

    /**
     * Changes the value of an existing setting after checking it.
     *
     * @param key   the setting key
     * @param value the new value, as text
     * @throws ValidationException if the key does not exist or the value is invalid for it
     * @throws DataAccessException if the setting cannot be stored
     */
    public void update(String key, String value) throws ValidationException, DataAccessException {
        Map<String, Setting> settings = settingDao.loadAll();
        if (!settings.containsKey(key)) {
            throw new ValidationException("No existe el parámetro de configuración " + key + ".");
        }
        String previous = settings.get(key).getValue();
        settingDao.save(key, value);
        try {
            checkValue(key);
        } catch (ValidationException e) {
            settingDao.save(key, previous);
            throw e;
        }
    }

    private void checkValue(String key) throws ValidationException, DataAccessException {
        switch (key) {
            case DEPOSIT_PERCENTAGE, LOW_ROTATION_DISCOUNT_PERCENTAGE -> percentage(key);
            case MIN_BOOKING_NOTICE_HOURS, REMINDER_HOURS_BEFORE -> wholeNumber(key, 0);
            case NO_SHOW_PENALTY_THRESHOLD, LOW_ROTATION_WINDOW_DAYS, LOW_ROTATION_TOP_N -> wholeNumber(key, 1);
            default -> read(key);
        }
    }

    private String read(String key) throws ValidationException, DataAccessException {
        Setting setting = settingDao.loadAll().get(key);
        if (setting == null || setting.getValue().isBlank()) {
            throw new ValidationException("Falta el parámetro de configuración " + key + ".");
        }
        return setting.getValue().trim();
    }

    private double percentage(String key) throws ValidationException, DataAccessException {
        double value;
        try {
            value = Double.parseDouble(read(key));
        } catch (NumberFormatException e) {
            throw new ValidationException("El parámetro " + key + " debe ser un número.", e);
        }
        if (value < 0 || value > MAX_PERCENTAGE) {
            throw new ValidationException("El parámetro " + key + " debe estar entre 0 y 100.");
        }
        return value;
    }

    private int wholeNumber(String key, int minimum) throws ValidationException, DataAccessException {
        int value;
        try {
            value = Integer.parseInt(read(key));
        } catch (NumberFormatException e) {
            throw new ValidationException("El parámetro " + key + " debe ser un número entero.", e);
        }
        if (value < minimum) {
            throw new ValidationException("El parámetro " + key + " debe ser al menos " + minimum + ".");
        }
        return value;
    }
}
