package com.maisonglow.service;

import com.maisonglow.dao.AppointmentDao;
import com.maisonglow.dao.CrudDao;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.AppointmentStatus;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.rule.AppointmentRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("AppointmentService tests")
class AppointmentServiceTest {

    private static final LocalDateTime NINE = LocalDateTime.of(2026, 10, 20, 9, 0);

    private final Customer ana = new Customer(1, "1001", "Ana Ruiz", "3001112222", "ana@correo.com");
    private final Professional laura = new Professional(1, "2002", "Laura Gómez", "3157778888",
        "laura@correo.com", "Peluquería", LocalTime.of(8, 0), LocalTime.of(17, 0));
    private final Professional sofia = new Professional(2, "2003", "Sofía Pérez", "3159990000",
        "sofia@correo.com", "Peluquería", LocalTime.of(8, 0), LocalTime.of(17, 0));
    private final Treatment haircut = new Treatment(1, "Corte de cabello", "Corte y secado", 35000, 45);

    private InMemoryAppointmentDao appointmentDao;
    private CrudDao<Customer> customerDao;
    private AppointmentService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        laura.addTreatment(haircut);
        sofia.addTreatment(haircut);
        appointmentDao = new InMemoryAppointmentDao();
        customerDao = mock(CrudDao.class);
        service = new AppointmentService(appointmentDao, customerDao, List.of());
    }

    private Appointment bookWithLauraAt(LocalDateTime start) throws Exception {
        return service.book(ana, laura, haircut, start, 0);
    }

    @Test
    @DisplayName("book stores a pending appointment with its deposit")
    void bookStoresAppointment() throws Exception {
        Appointment booked = service.book(ana, laura, haircut, NINE, 10000);

        assertThat(booked.getId()).isPositive();
        assertThat(booked.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(booked.getDeposit()).isEqualTo(10000);
        assertThat(appointmentDao.findById(booked.getId())).contains(booked);
    }

    @Test
    @DisplayName("a second appointment that overlaps the first one is rejected and not stored")
    void overlappingBookingIsRejected() throws Exception {
        bookWithLauraAt(NINE);

        assertThatThrownBy(() -> bookWithLauraAt(NINE.plusMinutes(30)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("Laura Gómez ya tiene una cita en ese horario.");
        assertThat(appointmentDao.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("a slot that starts when the previous one ends is accepted")
    void touchingSlotIsAccepted() throws Exception {
        bookWithLauraAt(NINE);

        bookWithLauraAt(NINE.plusMinutes(45));

        assertThat(appointmentDao.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("a cancelled appointment no longer blocks the slot")
    void cancelledAppointmentFreesSlot() throws Exception {
        Appointment first = bookWithLauraAt(NINE);
        service.cancel(first.getId());

        bookWithLauraAt(NINE);

        assertThat(appointmentDao.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("another professional can take the same slot")
    void otherProfessionalSameSlot() throws Exception {
        bookWithLauraAt(NINE);

        service.book(ana, sofia, haircut, NINE, 0);

        assertThat(appointmentDao.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("an inactive professional or treatment is rejected")
    void inactiveProfessionalOrTreatment() {
        sofia.deactivate();
        Treatment dye = new Treatment(2, "Tinte", "Color completo", 90000, 120);
        laura.addTreatment(dye);
        dye.deactivate();

        assertThatThrownBy(() -> service.book(ana, sofia, haircut, NINE, 0))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("no está activo");
        assertThatThrownBy(() -> service.book(ana, laura, dye, NINE, 0))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("no está disponible");
    }

    @Test
    @DisplayName("a treatment the professional does not offer is rejected")
    void treatmentNotOffered() {
        Treatment manicure = new Treatment(3, "Manicura", "Manicura clásica", 25000, 30);

        assertThatThrownBy(() -> service.book(ana, laura, manicure, NINE, 0))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("Laura Gómez no ofrece Manicura.");
    }

    @Test
    @DisplayName("a slot outside the working hours is rejected")
    void outsideWorkingHours() {
        assertThatThrownBy(() -> bookWithLauraAt(NINE.withHour(16).withMinute(30)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("Laura Gómez solo atiende de 08:00 a 17:00.");
    }

    @Test
    @DisplayName("missing data is rejected before anything is checked")
    void missingData() {
        assertThatThrownBy(() -> service.book(null, laura, haircut, NINE, 0))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("cliente");
        assertThatThrownBy(() -> service.book(ana, laura, haircut, null, 0))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("fecha y hora");
    }

    @Test
    @DisplayName("a negative deposit is rejected")
    void negativeDeposit() {
        assertThatThrownBy(() -> service.book(ana, laura, haircut, NINE, -1))
            .isInstanceOf(BusinessRuleException.class);
        assertThat(appointmentDao.findAll()).isEmpty();
    }

    @Test
    @DisplayName("a new rule is added without changing the service, and runs in order")
    void rulesAreApplied() throws Exception {
        service = new AppointmentService(appointmentDao, customerDao, List.of(new ReminderTestRule()));

        Appointment booked = bookWithLauraAt(NINE);

        assertThat(booked.getReminderAt()).isEqualTo(NINE.minusHours(24));
    }

    @Test
    @DisplayName("an appointment that breaks a rule is not stored")
    void brokenRuleIsNotStored() {
        service = new AppointmentService(appointmentDao, customerDao, List.of(new RejectingTestRule()));

        assertThatThrownBy(() -> bookWithLauraAt(NINE))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("Regla de prueba incumplida.");
        assertThat(appointmentDao.findAll()).isEmpty();
    }

    @Test
    @DisplayName("reschedule moves the appointment and may overlap its own old slot")
    void rescheduleMovesAppointment() throws Exception {
        Appointment booked = bookWithLauraAt(NINE);

        Appointment moved = service.reschedule(booked.getId(), NINE.plusMinutes(15));

        assertThat(moved.getId()).isEqualTo(booked.getId());
        assertThat(moved.getEnd()).isEqualTo(NINE.plusMinutes(60));
        assertThat(appointmentDao.findById(booked.getId())).contains(moved);
        assertThat(appointmentDao.findById(booked.getId()).orElseThrow().getStart())
            .isEqualTo(NINE.plusMinutes(15));
    }

    @Test
    @DisplayName("reschedule into another appointment's slot is rejected and keeps the old time")
    void rescheduleIntoConflict() throws Exception {
        Appointment nine = bookWithLauraAt(NINE);
        bookWithLauraAt(NINE.plusHours(2));

        assertThatThrownBy(() -> service.reschedule(nine.getId(), NINE.plusHours(2).plusMinutes(15)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("ya tiene una cita");
        assertThat(appointmentDao.findById(nine.getId()).orElseThrow().getStart()).isEqualTo(NINE);
    }

    @Test
    @DisplayName("reschedule re-applies the rules for the new start")
    void rescheduleAppliesRules() throws Exception {
        service = new AppointmentService(appointmentDao, customerDao, List.of(new ReminderTestRule()));
        Appointment booked = bookWithLauraAt(NINE);

        Appointment moved = service.reschedule(booked.getId(), NINE.plusDays(1));

        assertThat(moved.getReminderAt()).isEqualTo(NINE.plusDays(1).minusHours(24));
    }

    @Test
    @DisplayName("a closed or unknown appointment cannot be rescheduled")
    void rescheduleClosedOrUnknown() throws Exception {
        Appointment booked = bookWithLauraAt(NINE);
        service.complete(booked.getId());

        assertThatThrownBy(() -> service.reschedule(booked.getId(), NINE.plusDays(1)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("No se puede reprogramar una cita completada.");
        assertThatThrownBy(() -> service.reschedule(99, NINE.plusDays(1)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("No existe la cita con id 99.");
    }

    @Test
    @DisplayName("confirm, cancel and complete store the new status")
    void statusChangesAreStored() throws Exception {
        Appointment first = bookWithLauraAt(NINE);
        Appointment second = bookWithLauraAt(NINE.plusHours(1));

        service.confirm(first.getId());
        service.complete(first.getId());
        service.cancel(second.getId());

        assertThat(appointmentDao.updates).isEqualTo(3);
        assertThat(appointmentDao.findById(first.getId()).orElseThrow().getStatus())
            .isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(appointmentDao.findById(second.getId()).orElseThrow().getStatus())
            .isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("an invalid status change is rejected and not stored")
    void invalidStatusChange() throws Exception {
        Appointment booked = bookWithLauraAt(NINE);
        service.cancel(booked.getId());
        int updatesBefore = appointmentDao.updates;

        assertThatThrownBy(() -> service.confirm(booked.getId())).isInstanceOf(BusinessRuleException.class);
        assertThat(appointmentDao.updates).isEqualTo(updatesBefore);
    }

    @Test
    @DisplayName("markNoShow stores the appointment and the customer's new no-show count")
    void markNoShowSavesCustomer() throws Exception {
        Appointment booked = bookWithLauraAt(NINE);

        service.markNoShow(booked.getId());

        assertThat(booked.getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);
        assertThat(ana.getNoShowCount()).isEqualTo(1);
        verify(customerDao).update(ana);
    }

    @Test
    @DisplayName("listByDate returns the schedule of that day only")
    void listByDate() throws Exception {
        Appointment today = bookWithLauraAt(NINE);
        bookWithLauraAt(NINE.plusDays(1));

        assertThat(service.listByDate(NINE.toLocalDate())).containsExactly(today);
        assertThatThrownBy(() -> service.listByDate(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("checkAvailability considers status, working hours and other appointments")
    void checkAvailability() throws Exception {
        bookWithLauraAt(NINE);

        assertThat(service.checkAvailability(laura, NINE.plusMinutes(30), NINE.plusMinutes(60))).isFalse();
        assertThat(service.checkAvailability(laura, NINE.plusMinutes(45), NINE.plusMinutes(90))).isTrue();
        assertThat(service.checkAvailability(laura, NINE.withHour(17), NINE.withHour(18))).isFalse();
        laura.deactivate();
        assertThat(service.checkAvailability(laura, NINE.plusHours(3), NINE.plusHours(4))).isFalse();
    }

    private static final class ReminderTestRule implements AppointmentRule {

        @Override
        public void apply(Appointment appointment) {
            appointment.setReminderAt(appointment.getStart().minusHours(24));
        }

        @Override
        public String getName() {
            return "Recordatorio de prueba";
        }
    }

    private static final class RejectingTestRule implements AppointmentRule {

        @Override
        public void apply(Appointment appointment) throws BusinessRuleException {
            throw new BusinessRuleException("Regla de prueba incumplida.");
        }

        @Override
        public String getName() {
            return "Regla de prueba";
        }
    }

    private static final class InMemoryAppointmentDao implements AppointmentDao {

        private final Map<Integer, Appointment> appointments = new LinkedHashMap<>();
        private int nextId = 1;
        private int updates;

        @Override
        public Appointment create(Appointment appointment) {
            appointment.setId(nextId++);
            appointments.put(appointment.getId(), appointment);
            return appointment;
        }

        @Override
        public Optional<Appointment> findById(int id) {
            return Optional.ofNullable(appointments.get(id));
        }

        @Override
        public List<Appointment> findAll() {
            return new ArrayList<>(appointments.values());
        }

        @Override
        public void update(Appointment appointment) {
            appointments.put(appointment.getId(), appointment);
            updates++;
        }

        @Override
        public void delete(int id) {
            appointments.remove(id);
        }

        @Override
        public List<Appointment> findByProfessional(int professionalId) {
            return appointments.values().stream()
                .filter(appointment -> appointment.getProfessional().getId() == professionalId)
                .toList();
        }

        @Override
        public List<Appointment> findByDate(LocalDate date) {
            return appointments.values().stream()
                .filter(appointment -> appointment.getStart().toLocalDate().equals(date))
                .toList();
        }
    }
}
