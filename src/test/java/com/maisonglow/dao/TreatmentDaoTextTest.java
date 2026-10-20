package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Treatment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TreatmentDaoText tests")
class TreatmentDaoTextTest {

    @TempDir
    Path folder;

    @Test
    @DisplayName("all the fields, including an inactive state, survive a reload")
    void createAndReload() throws DataAccessException {
        Path file = folder.resolve("treatments.txt");
        TreatmentDao dao = new TreatmentDaoText(file);
        Treatment created = dao.create(new Treatment(0, "Keratina", "Alisado | brillo", 120000, 120));
        created.deactivate();
        dao.update(created);

        Treatment reloaded = new TreatmentDaoText(file).findById(created.getId()).orElseThrow();

        assertThat(reloaded.getDescription()).isEqualTo("Alisado | brillo");
        assertThat(reloaded.getPrice()).isEqualTo(120000);
        assertThat(reloaded.getDurationMinutes()).isEqualTo(120);
        assertThat(reloaded.isActive()).isFalse();
    }

    @Test
    @DisplayName("findByName ignores case")
    void findByName() throws DataAccessException {
        TreatmentDao dao = new TreatmentDaoText(folder.resolve("treatments.txt"));
        dao.create(new Treatment(0, "Manicure", "", 25000, 40));

        assertThat(dao.findByName("manicure")).isPresent();
        assertThat(dao.findByName("Pedicure")).isEmpty();
    }

    @Test
    @DisplayName("a line with missing fields is reported with its line number")
    void malformedLine() throws Exception {
        Path file = folder.resolve("treatments.txt");
        Files.writeString(file, "#nextId=2\n1|Manicure|25000\n");

        assertThatThrownBy(() -> new TreatmentDaoText(file).findAll())
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("línea 2");
    }
}
