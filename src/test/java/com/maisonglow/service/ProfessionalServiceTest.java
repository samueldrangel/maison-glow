package com.maisonglow.service;

import com.maisonglow.dao.ProfessionalDao;
import com.maisonglow.dao.ProfessionalDaoText;
import com.maisonglow.dao.TreatmentDao;
import com.maisonglow.dao.TreatmentDaoText;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProfessionalService tests")
class ProfessionalServiceTest {

    private static final ContactData LAURA = new ContactData("49777888", " Laura Gómez ", "3157778888",
        "laura@correo.com");
    private static final WorkSchedule DAY = new WorkSchedule(LocalTime.of(8, 0), LocalTime.of(17, 0));

    @TempDir
    Path folder;

    private TreatmentDao treatmentDao;
    private ProfessionalService service;
    private Treatment haircut;

    @BeforeEach
    void setUp() throws Exception {
        treatmentDao = new TreatmentDaoText(folder.resolve("treatments.txt"));
        haircut = treatmentDao.create(new Treatment(0, "Corte de cabello", "Corte y secado", 35000, 45));
        service = new ProfessionalService(newProfessionalDao(), treatmentDao);
    }

    private ProfessionalDao newProfessionalDao() {
        return new ProfessionalDaoText(folder.resolve("professionals.txt"),
            folder.resolve("professional_treatments.txt"), treatmentDao);
    }

    @Test
    @DisplayName("register stores an active professional without treatments")
    void register() throws Exception {
        Professional laura = service.register(LAURA, "Peluquería", DAY);

        Professional reloaded = newProfessionalDao().findById(laura.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Laura Gómez");
        assertThat(reloaded.isActive()).isTrue();
        assertThat(reloaded.getTreatments()).isEmpty();
    }

    @Test
    @DisplayName("register rejects a repeated document")
    void registerRepeatedDocument() throws Exception {
        service.register(LAURA, "Peluquería", DAY);

        assertThatThrownBy(() -> service.register(LAURA, "Uñas", DAY))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("Ya existe un profesional con el documento 49777888.");
    }

    @Test
    @DisplayName("register rejects a working day that ends before or when it starts")
    void registerReversedHours() {
        WorkSchedule reversed = new WorkSchedule(LocalTime.of(17, 0), LocalTime.of(8, 0));
        WorkSchedule empty = new WorkSchedule(LocalTime.of(8, 0), LocalTime.of(8, 0));

        assertThatThrownBy(() -> service.register(LAURA, "Peluquería", reversed))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.register(LAURA, "Peluquería", empty))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.register(LAURA, "Peluquería", new WorkSchedule(null, LocalTime.NOON)))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("register rejects a blank specialty or a bad email")
    void registerInvalidData() {
        ContactData badEmail = new ContactData("49777888", "Laura Gómez", "3157778888", "laura");

        assertThatThrownBy(() -> service.register(LAURA, " ", DAY)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.register(badEmail, "Peluquería", DAY))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("update changes the contact data but never the document")
    void update() throws Exception {
        Professional laura = service.register(LAURA, "Peluquería", DAY);

        service.update(laura.getId(), new ContactData("49777888", "Laura G.", "3150000000", "lg@correo.com"),
            "Colorimetría");

        Professional reloaded = newProfessionalDao().findById(laura.getId()).orElseThrow();
        assertThat(reloaded.getPhone()).isEqualTo("3150000000");
        assertThat(reloaded.getSpecialty()).isEqualTo("Colorimetría");
        assertThatThrownBy(() -> service.update(laura.getId(),
            new ContactData("1111", "Laura G.", "3150000000", "lg@correo.com"), "Colorimetría"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("documento");
    }

    @Test
    @DisplayName("changeWorkingHours saves the new day and rejects reversed hours")
    void changeWorkingHours() throws Exception {
        Professional laura = service.register(LAURA, "Peluquería", DAY);

        service.changeWorkingHours(laura.getId(), new WorkSchedule(LocalTime.of(10, 0), LocalTime.of(19, 0)));

        assertThat(service.findById(laura.getId()).getWorkEnd()).isEqualTo(LocalTime.of(19, 0));
        assertThatThrownBy(() -> service.changeWorkingHours(laura.getId(),
            new WorkSchedule(LocalTime.of(19, 0), LocalTime.of(10, 0))))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("assignTreatment links the treatment and the link survives a reload")
    void assignTreatment() throws Exception {
        Professional laura = service.register(LAURA, "Peluquería", DAY);

        service.assignTreatment(laura.getId(), haircut.getId());

        assertThat(newProfessionalDao().findById(laura.getId()).orElseThrow().offers(haircut)).isTrue();
    }

    @Test
    @DisplayName("assignTreatment rejects an inactive, missing or repeated treatment")
    void assignTreatmentRejected() throws Exception {
        Professional laura = service.register(LAURA, "Peluquería", DAY);
        Treatment dye = treatmentDao.create(new Treatment(0, "Tinte", "Color completo", 90000, 120));
        dye.deactivate();
        treatmentDao.update(dye);
        service.assignTreatment(laura.getId(), haircut.getId());

        assertThatThrownBy(() -> service.assignTreatment(laura.getId(), dye.getId()))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("El tratamiento Tinte no está activo.");
        assertThatThrownBy(() -> service.assignTreatment(laura.getId(), 99))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("No existe el tratamiento");
        assertThatThrownBy(() -> service.assignTreatment(laura.getId(), haircut.getId()))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("Laura Gómez ya ofrece Corte de cabello.");
    }

    @Test
    @DisplayName("removeTreatment unlinks it and rejects a treatment not offered")
    void removeTreatment() throws Exception {
        Professional laura = service.register(LAURA, "Peluquería", DAY);
        service.assignTreatment(laura.getId(), haircut.getId());

        service.removeTreatment(laura.getId(), haircut.getId());

        assertThat(service.findById(laura.getId()).getTreatments()).isEmpty();
        assertThatThrownBy(() -> service.removeTreatment(laura.getId(), haircut.getId()))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("no ofrece");
    }

    @Test
    @DisplayName("listByTreatment returns only the active professionals who offer it")
    void listByTreatment() throws Exception {
        Professional laura = service.register(LAURA, "Peluquería", DAY);
        Professional sofia = service.register(new ContactData("52111222", "Sofía Pérez", "3159990000",
            "sofia@correo.com"), "Peluquería", DAY);
        service.register(new ContactData("53000111", "Marta Ruiz", "3151112222", "marta@correo.com"), "Uñas", DAY);
        service.assignTreatment(laura.getId(), haircut.getId());
        service.assignTreatment(sofia.getId(), haircut.getId());
        service.deactivate(sofia.getId());

        assertThat(service.listByTreatment(haircut.getId())).containsExactly(laura);
        service.activate(sofia.getId());
        assertThat(service.listByTreatment(haircut.getId())).hasSize(2);
        assertThat(service.listAll()).hasSize(3);
    }

    @Test
    @DisplayName("findById rejects an unknown id")
    void findUnknown() {
        assertThatThrownBy(() -> service.findById(42))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("No existe el profesional con id 42.");
    }
}
