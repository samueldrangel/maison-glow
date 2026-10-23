package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProfessionalDaoText tests")
class ProfessionalDaoTextTest {

    @TempDir
    Path folder;

    private TreatmentDao treatments;
    private Treatment haircut;
    private Treatment dye;

    @BeforeEach
    void setUp() throws DataAccessException {
        treatments = new TreatmentDaoText(folder.resolve("treatments.txt"));
        haircut = treatments.create(new Treatment(0, "Corte de cabello", "Corte y secado", 35000, 45));
        dye = treatments.create(new Treatment(0, "Tinte", "Color completo", 90000, 120));
    }

    private ProfessionalDao newDao() {
        return new ProfessionalDaoText(folder.resolve("professionals.txt"),
            folder.resolve("professional_treatments.txt"), treatments);
    }

    private Professional laura() {
        return new Professional(0, "49777888", "Laura Gómez", "3157778888", "laura@correo.com",
            "Peluquería", LocalTime.of(8, 0), LocalTime.of(17, 30));
    }

    @Test
    @DisplayName("all the fields and the treatments survive a reload")
    void createAndReload() throws Exception {
        Professional laura = laura();
        laura.addTreatment(haircut);
        laura.addTreatment(dye);
        laura.deactivate();
        newDao().create(laura);

        Professional reloaded = newDao().findById(laura.getId()).orElseThrow();

        assertThat(reloaded.getDocument()).isEqualTo("49777888");
        assertThat(reloaded.getSpecialty()).isEqualTo("Peluquería");
        assertThat(reloaded.getWorkStart()).isEqualTo(LocalTime.of(8, 0));
        assertThat(reloaded.getWorkEnd()).isEqualTo(LocalTime.of(17, 30));
        assertThat(reloaded.isActive()).isFalse();
        assertThat(reloaded.getTreatments()).containsExactly(haircut, dye);
    }

    @Test
    @DisplayName("removing a treatment is saved in the links file")
    void updateLinks() throws Exception {
        ProfessionalDao dao = newDao();
        Professional laura = laura();
        laura.addTreatment(haircut);
        laura.addTreatment(dye);
        dao.create(laura);
        laura.removeTreatment(dye);
        dao.update(laura);

        assertThat(Files.readAllLines(folder.resolve("professional_treatments.txt"))).containsExactly("1|1");
        assertThat(newDao().findById(laura.getId()).orElseThrow().getTreatments()).containsExactly(haircut);
    }

    @Test
    @DisplayName("deleting a professional also removes their links")
    void deleteRemovesLinks() throws Exception {
        ProfessionalDao dao = newDao();
        Professional laura = laura();
        laura.addTreatment(haircut);
        dao.create(laura);

        dao.delete(laura.getId());

        assertThat(Files.readAllLines(folder.resolve("professional_treatments.txt"))).isEmpty();
        assertThat(newDao().findAll()).isEmpty();
    }

    @Test
    @DisplayName("findByDocument and findByTreatment find the right professionals")
    void queries() throws Exception {
        ProfessionalDao dao = newDao();
        Professional laura = laura();
        laura.addTreatment(haircut);
        dao.create(laura);
        dao.create(new Professional(0, "52111222", "Sofía Pérez", "3159990000", "sofia@correo.com",
            "Uñas", LocalTime.of(9, 0), LocalTime.of(18, 0)));

        assertThat(dao.findByDocument("49777888")).contains(laura);
        assertThat(dao.findByDocument("0000")).isEmpty();
        assertThat(dao.findByTreatment(haircut.getId())).containsExactly(laura);
        assertThat(dao.findByTreatment(dye.getId())).isEmpty();
    }

    @Test
    @DisplayName("a link to a treatment that does not exist is reported with its line number")
    void missingTreatment() throws Exception {
        newDao().create(laura());
        Files.writeString(folder.resolve("professional_treatments.txt"), "1|99\n");

        assertThatThrownBy(() -> newDao().findAll())
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("línea 2")
            .hasMessageContaining("tratamiento con id 99");
    }
}
