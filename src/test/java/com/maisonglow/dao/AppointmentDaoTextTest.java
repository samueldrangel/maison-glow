package com.maisonglow.dao;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.AppointmentStatus;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AppointmentDaoText tests")
class AppointmentDaoTextTest {

    private static final LocalDateTime OCT_20_AT_9 = LocalDateTime.of(2026, 10, 20, 9, 0);

    private final Customer ana = new Customer(1, "1001", "Ana Ruiz", "3001112222", "ana@correo.com");
    private final Professional laura = professional(1, "2002", "Laura Gómez");
    private final Professional sofia = professional(2, "2003", "Sofía Díaz");
    private final Treatment haircut = new Treatment(1, "Corte de cabello", "Corte y secado", 35000, 45);

    @TempDir
    Path folder;

    private Path file;
    private InMemoryDao<Customer> customers;
    private AppointmentDaoText dao;

    private static Professional professional(int id, String document, String name) {
        return new Professional(id, document, name, "3157778888", "pro@correo.com", "Peluquería",
            LocalTime.of(8, 0), LocalTime.of(17, 0));
    }

    private AppointmentDaoText openDao() {
        return new AppointmentDaoText(file, customers,
            new InMemoryDao<>(Professional::getId, laura, sofia),
            new InMemoryDao<>(Treatment::getId, haircut));
    }

    @BeforeEach
    void setUp() {
        file = folder.resolve("appointments.txt");
        customers = new InMemoryDao<>(Customer::getId, ana);
        dao = openDao();
    }

    private Appointment book(Professional professional, LocalDateTime start) throws DataAccessException {
        return dao.create(new Appointment(0, ana, professional, haircut, start));
    }

    @Test
    @DisplayName("every field survives a save and a reload")
    void roundTripKeepsAllFields() throws Exception {
        Appointment booked = new Appointment(0, ana, laura, haircut, OCT_20_AT_9);
        booked.confirm();
        booked.setDeposit(10500);
        booked.setReminderAt(OCT_20_AT_9.minusHours(24));
        dao.create(booked);

        Appointment loaded = openDao().findById(booked.getId()).orElseThrow();

        assertThat(loaded.getCustomer()).isSameAs(ana);
        assertThat(loaded.getProfessional()).isSameAs(laura);
        assertThat(loaded.getTreatment()).isSameAs(haircut);
        assertThat(loaded.getStart()).isEqualTo(OCT_20_AT_9);
        assertThat(loaded.getEnd()).isEqualTo(OCT_20_AT_9.plusMinutes(45));
        assertThat(loaded.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(loaded.getDeposit()).isEqualTo(10500);
        assertThat(loaded.getReminderAt()).isEqualTo(OCT_20_AT_9.minusHours(24));
        assertThat(loaded.getCreatedAt()).isEqualTo(booked.getCreatedAt());
    }

    @Test
    @DisplayName("an appointment without reminder reloads with a null reminder")
    void missingReminderReloadsAsNull() throws DataAccessException {
        Appointment booked = book(laura, OCT_20_AT_9);

        assertThat(openDao().findById(booked.getId()).orElseThrow().getReminderAt()).isNull();
    }

    @Test
    @DisplayName("the file follows the column order of the appointment table")
    void fileFollowsSchemaColumns() throws Exception {
        Appointment booked = book(laura, OCT_20_AT_9);

        assertThat(Files.readAllLines(file).get(1)).isEqualTo(
            "1|1|1|1|2026-10-20T09:00|2026-10-20T09:45|PENDING|0.0||" + booked.getCreatedAt());
    }

    @Test
    @DisplayName("findByProfessional returns only that professional's appointments, by start time")
    void findByProfessional() throws DataAccessException {
        book(laura, OCT_20_AT_9.plusHours(3));
        book(sofia, OCT_20_AT_9);
        book(laura, OCT_20_AT_9);

        assertThat(dao.findByProfessional(laura.getId()))
            .extracting(Appointment::getStart)
            .containsExactly(OCT_20_AT_9, OCT_20_AT_9.plusHours(3));
    }

    @Test
    @DisplayName("findByDate returns the appointments of that day, by start time, in every status")
    void findByDate() throws DataAccessException, BusinessRuleException {
        book(laura, OCT_20_AT_9.plusHours(2));
        book(laura, OCT_20_AT_9.plusDays(1));
        Appointment cancelled = book(sofia, OCT_20_AT_9);
        cancelled.cancel();
        dao.update(cancelled);

        assertThat(dao.findByDate(LocalDate.of(2026, 10, 20)))
            .extracting(Appointment::getStart)
            .containsExactly(OCT_20_AT_9, OCT_20_AT_9.plusHours(2));
    }

    @Test
    @DisplayName("an appointment whose customer no longer exists is reported")
    void missingCustomerFails() throws DataAccessException {
        book(laura, OCT_20_AT_9);
        customers.delete(ana.getId());

        assertThatThrownBy(() -> openDao().findAll())
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("cliente con id 1")
            .hasMessageContaining("línea 2");
    }

    @Test
    @DisplayName("a line with missing fields is reported")
    void incompleteLineFails() throws Exception {
        Files.writeString(file, "1|1|1|1|2026-10-20T09:00\n");

        assertThatThrownBy(() -> dao.findAll())
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("Se esperaban 10 campos");
    }
}
