package com.maisonglow.service;

import com.maisonglow.dao.TreatmentDao;
import com.maisonglow.dao.TreatmentDaoText;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Treatment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TreatmentService tests")
class TreatmentServiceTest {

    @TempDir
    Path folder;

    private TreatmentDao dao;
    private TreatmentService service;

    @BeforeEach
    void setUp() {
        dao = new TreatmentDaoText(folder.resolve("treatments.txt"));
        service = new TreatmentService(dao);
    }

    @Test
    @DisplayName("register stores a valid treatment that survives a reload")
    void registerAndReload() throws Exception {
        Treatment manicure = service.register(new TreatmentData(" Manicure ", null, 25000, 40));

        Treatment reloaded = new TreatmentDaoText(folder.resolve("treatments.txt"))
            .findById(manicure.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Manicure");
        assertThat(reloaded.getDescription()).isEmpty();
        assertThat(reloaded.getDurationMinutes()).isEqualTo(40);
    }

    @Test
    @DisplayName("register rejects a repeated name ignoring case")
    void registerRepeatedName() throws Exception {
        service.register(new TreatmentData("Manicure", "Básico", 25000, 40));

        assertThatThrownBy(() -> service.register(new TreatmentData("MANICURE", "", 30000, 45)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Ya existe");
    }

    @Test
    @DisplayName("register rejects zero price, zero duration and a blank name")
    void registerInvalidData() {
        assertThatThrownBy(() -> service.register(new TreatmentData("Pedicure", "", 0, 40)))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.register(new TreatmentData("Pedicure", "", 25000, 0)))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.register(new TreatmentData("", "", 25000, 30)))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("update changes the data and keeps the name check")
    void updateTreatment() throws Exception {
        Treatment manicure = service.register(new TreatmentData("Manicure", "", 25000, 40));
        service.register(new TreatmentData("Pedicure", "", 30000, 50));

        service.update(manicure.getId(), new TreatmentData("Manicure", "Con esmaltado", 28000, 45));

        Treatment updated = service.findById(manicure.getId());
        assertThat(updated.getPrice()).isEqualTo(28000);
        assertThat(updated.getDescription()).isEqualTo("Con esmaltado");
        assertThatThrownBy(() -> service.update(manicure.getId(), new TreatmentData("Pedicure", "", 1000, 10)))
            .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("deactivate hides the treatment from the active list and activate restores it")
    void activation() throws Exception {
        Treatment manicure = service.register(new TreatmentData("Manicure", "", 25000, 40));
        service.register(new TreatmentData("Pedicure", "", 30000, 50));

        service.deactivate(manicure.getId());
        assertThat(service.listActive()).extracting(Treatment::getName).containsExactly("Pedicure");
        assertThat(service.listAll()).hasSize(2);

        service.activate(manicure.getId());
        assertThat(service.listActive()).hasSize(2);
    }

    @Test
    @DisplayName("findById fails for an unknown treatment")
    void findUnknown() {
        assertThatThrownBy(() -> service.findById(3))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("3");
    }
}
