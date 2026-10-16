package com.maisonglow.service;

import com.maisonglow.dao.SettingDao;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SettingsService tests")
class SettingsServiceTest {

    private InMemorySettingDao dao;
    private SettingsService service;

    @BeforeEach
    void setUp() throws DataAccessException {
        dao = new InMemorySettingDao();
        dao.save(SettingsService.BUSINESS_NAME, "Maison Glow");
        dao.save(SettingsService.DEPOSIT_PERCENTAGE, "30");
        dao.save(SettingsService.MIN_BOOKING_NOTICE_HOURS, "2");
        dao.save(SettingsService.REMINDER_HOURS_BEFORE, "24");
        dao.save(SettingsService.NO_SHOW_PENALTY_THRESHOLD, "3");
        dao.save(SettingsService.LOW_ROTATION_WINDOW_DAYS, "30");
        dao.save(SettingsService.LOW_ROTATION_TOP_N, "5");
        dao.save(SettingsService.LOW_ROTATION_DISCOUNT_PERCENTAGE, "15");
        service = new SettingsService(dao);
    }

    @Test
    @DisplayName("typed getters return the stored values")
    void typedGetters() throws Exception {
        assertThat(service.businessName()).isEqualTo("Maison Glow");
        assertThat(service.depositPercentage()).isEqualTo(30);
        assertThat(service.minBookingNoticeHours()).isEqualTo(2);
        assertThat(service.reminderHoursBefore()).isEqualTo(24);
        assertThat(service.noShowPenaltyThreshold()).isEqualTo(3);
        assertThat(service.lowRotationWindowDays()).isEqualTo(30);
        assertThat(service.lowRotationTopN()).isEqualTo(5);
        assertThat(service.lowRotationDiscountPercentage()).isEqualTo(15);
    }

    @Test
    @DisplayName("a changed value is reflected without touching code")
    void changedValueIsReflected() throws Exception {
        dao.save(SettingsService.DEPOSIT_PERCENTAGE, "50");

        assertThat(service.depositPercentage()).isEqualTo(50);
    }

    @Test
    @DisplayName("an out-of-range percentage in the file is rejected")
    void percentageOutOfRange() throws DataAccessException {
        dao.save(SettingsService.DEPOSIT_PERCENTAGE, "120");

        assertThatThrownBy(() -> service.depositPercentage())
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("entre 0 y 100");
    }

    @Test
    @DisplayName("negative hours and non-numeric values are rejected")
    void invalidNumbers() throws DataAccessException {
        dao.save(SettingsService.REMINDER_HOURS_BEFORE, "-1");
        dao.save(SettingsService.LOW_ROTATION_TOP_N, "cinco");

        assertThatThrownBy(() -> service.reminderHoursBefore()).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.lowRotationTopN())
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("entero");
    }

    @Test
    @DisplayName("a missing or blank setting is rejected")
    void missingSetting() throws DataAccessException {
        dao.save(SettingsService.BUSINESS_NAME, " ");
        SettingsService empty = new SettingsService(new InMemorySettingDao());

        assertThatThrownBy(() -> service.businessName()).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> empty.noShowPenaltyThreshold())
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining(SettingsService.NO_SHOW_PENALTY_THRESHOLD);
    }

    @Test
    @DisplayName("update stores a valid value")
    void updateValidValue() throws Exception {
        service.update(SettingsService.LOW_ROTATION_WINDOW_DAYS, "60");

        assertThat(service.lowRotationWindowDays()).isEqualTo(60);
    }

    @Test
    @DisplayName("update rejects an invalid value and keeps the previous one")
    void updateInvalidValueKeepsPrevious() throws Exception {
        assertThatThrownBy(() -> service.update(SettingsService.LOW_ROTATION_DISCOUNT_PERCENTAGE, "-5"))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.update(SettingsService.NO_SHOW_PENALTY_THRESHOLD, "0"))
            .isInstanceOf(ValidationException.class);

        assertThat(service.lowRotationDiscountPercentage()).isEqualTo(15);
        assertThat(service.noShowPenaltyThreshold()).isEqualTo(3);
    }

    @Test
    @DisplayName("update validates before saving, so an invalid value is never written")
    void updateInvalidValueIsNeverSaved() {
        int savesBefore = dao.saves;

        assertThatThrownBy(() -> service.update(SettingsService.DEPOSIT_PERCENTAGE, "120"))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.update(SettingsService.REMINDER_HOURS_BEFORE, "dos"))
            .isInstanceOf(ValidationException.class);

        assertThat(dao.saves).isEqualTo(savesBefore);
    }

    @Test
    @DisplayName("a valid update is written once")
    void updateValidValueIsSavedOnce() throws Exception {
        int savesBefore = dao.saves;

        service.update(SettingsService.DEPOSIT_PERCENTAGE, " 40 ");

        assertThat(dao.saves).isEqualTo(savesBefore + 1);
        assertThat(service.depositPercentage()).isEqualTo(40);
    }

    @Test
    @DisplayName("update rejects unknown keys")
    void updateUnknownKey() {
        assertThatThrownBy(() -> service.update("unknown_key", "1"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("unknown_key");
    }

    @Test
    @DisplayName("update accepts any non-blank business name and hour values")
    void updateOtherKeys() throws Exception {
        service.update(SettingsService.BUSINESS_NAME, "Maison Glow Valledupar");
        service.update(SettingsService.MIN_BOOKING_NOTICE_HOURS, "0");

        assertThat(service.businessName()).isEqualTo("Maison Glow Valledupar");
        assertThat(service.minBookingNoticeHours()).isZero();
    }

    private static final class InMemorySettingDao implements SettingDao {

        private final Map<String, Setting> settings = new LinkedHashMap<>();
        private int saves;

        @Override
        public Map<String, Setting> loadAll() {
            return settings;
        }

        @Override
        public void save(String key, String value) {
            settings.put(key, new Setting(key, value, ""));
            saves++;
        }
    }
}
