package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Setting;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SettingDaoText tests")
class SettingDaoTextTest {

    @TempDir
    Path folder;

    @Test
    @DisplayName("loads key, value and description from the file")
    void loadsSettings() throws IOException, DataAccessException {
        Path file = folder.resolve("settings.txt");
        Files.write(file, List.of("deposit_percentage|30|Deposit when booking"));

        Setting setting = new SettingDaoText(file).loadAll().get("deposit_percentage");

        assertThat(setting.getValue()).isEqualTo("30");
        assertThat(setting.getDescription()).isEqualTo("Deposit when booking");
    }

    @Test
    @DisplayName("saved values survive a reload and keep the description")
    void saveAndReload() throws IOException, DataAccessException {
        Path file = folder.resolve("settings.txt");
        Files.write(file, List.of("business_name|Maison Glow|Name on invoices"));

        SettingDao dao = new SettingDaoText(file);
        dao.save("business_name", "Maison | Glow");
        dao.save("new_key", "7");

        SettingDao reloaded = new SettingDaoText(file);
        assertThat(reloaded.loadAll().get("business_name").getValue()).isEqualTo("Maison | Glow");
        assertThat(reloaded.loadAll().get("business_name").getDescription()).isEqualTo("Name on invoices");
        assertThat(reloaded.loadAll().get("new_key").getValue()).isEqualTo("7");
    }

    @Test
    @DisplayName("a missing file means no settings")
    void missingFile() throws DataAccessException {
        assertThat(new SettingDaoText(folder.resolve("none.txt")).loadAll()).isEmpty();
    }

    @Test
    @DisplayName("a malformed line is reported")
    void malformedLine() throws IOException {
        Path file = folder.resolve("settings.txt");
        Files.write(file, List.of("only_key"));

        assertThatThrownBy(() -> new SettingDaoText(file).loadAll()).isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("the preloaded settings file has the seed values")
    void preloadedFile() throws DataAccessException {
        assertThat(new SettingDaoText(Path.of("data", "settings.txt")).loadAll())
            .containsKeys("business_name", "deposit_percentage", "low_rotation_discount_percentage");
    }
}
