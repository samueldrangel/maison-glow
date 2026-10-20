package com.maisonglow.service.rule;

import com.maisonglow.dao.AppointmentDao;
import com.maisonglow.dao.CrudDao;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.AppointmentService;
import com.maisonglow.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Appointment rules tests")
class AppointmentRulesTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 20, 8, 0);
    private static final Clock CLOCK = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private SettingsService settings;

    private final Customer ana = new Customer(1, "1001", "Ana Ruiz", "3001112222", "ana@correo.com");
    private final Professional laura = new Professional(1, "2002", "Laura Gómez", "3157778888",
        "laura@correo.com", "Peluquería", LocalTime.of(8, 0), LocalTime.of(17, 0));
    private final Treatment haircut = new Treatment(1, "Corte de cabello", "Corte y secado", 40000, 45);

    @BeforeEach
    void setUp() throws Exception {
        laura.addTreatment(haircut);
        lenient().when(settings.depositPercentage()).thenReturn(30.0);
        lenient().when(settings.noShowPenaltyThreshold()).thenReturn(3);
        lenient().when(settings.minBookingNoticeHours()).thenReturn(2);
        lenient().when(settings.reminderHoursBefore()).thenReturn(24);
    }

    private Appointment appointmentAt(Customer customer, LocalDateTime start, double deposit)
        throws BusinessRuleException {
        Appointment appointment = new Appointment(0, customer, laura, haircut, start);
        appointment.setDeposit(deposit);
        return appointment;
    }

    private Appointment appointmentAt(LocalDateTime start, double deposit) throws BusinessRuleException {
        return appointmentAt(ana, start, deposit);
    }

    @Test
    @DisplayName("DepositRule accepts the configured percentage and rejects less")
    void depositPercentage() throws Exception {
        DepositRule rule = new DepositRule(settings);
        Appointment enough = appointmentAt(NOW.plusDays(1), 12000);
        Appointment tooLittle = appointmentAt(NOW.plusDays(1), 11999);

        assertThatCode(() -> rule.apply(enough)).doesNotThrowAnyException();
        assertThatThrownBy(() -> rule.apply(tooLittle))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("$ 12.000");
    }

    @Test
    @DisplayName("DepositRule requires the full price when the no-shows reach the threshold")
    void depositPenalty() throws Exception {
        Customer missing = new Customer(2, "1002", "Marta Díaz", "3004445555", "marta@correo.com",
            LocalDate.of(2026, 1, 10), 3);
        DepositRule rule = new DepositRule(settings);
        Appointment partial = appointmentAt(missing, NOW.plusDays(1), 30000);
        Appointment full = appointmentAt(missing, NOW.plusDays(1), 40000);

        assertThat(rule.requiredDeposit(partial)).isEqualTo(40000);
        assertThatThrownBy(() -> rule.apply(partial)).isInstanceOf(BusinessRuleException.class);
        assertThatCode(() -> rule.apply(full)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("DepositRule keeps the normal percentage just below the threshold")
    void depositBelowThreshold() throws Exception {
        Customer once = new Customer(2, "1002", "Marta Díaz", "3004445555", "marta@correo.com",
            LocalDate.of(2026, 1, 10), 2);

        assertThat(new DepositRule(settings).requiredDeposit(appointmentAt(once, NOW.plusDays(1), 0)))
            .isEqualTo(12000);
    }

    @Test
    @DisplayName("ConfirmationRule accepts the exact notice and rejects less")
    void confirmationNotice() throws Exception {
        ConfirmationRule rule = new ConfirmationRule(settings, CLOCK);
        Appointment onTime = appointmentAt(NOW.plusHours(2), 0);
        Appointment tooSoon = appointmentAt(NOW.plusHours(1).plusMinutes(59), 0);

        assertThatCode(() -> rule.apply(onTime)).doesNotThrowAnyException();
        assertThatThrownBy(() -> rule.apply(tooSoon))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("Las citas se agendan con al menos 2 horas de anticipación.");
    }

    @Test
    @DisplayName("ReminderRule schedules the reminder the configured hours before the start")
    void reminder() throws Exception {
        Appointment appointment = appointmentAt(NOW.plusDays(2), 0);

        new ReminderRule(settings).apply(appointment);

        assertThat(appointment.getReminderAt()).isEqualTo(NOW.plusDays(1));
    }

    @Test
    @DisplayName("an invalid setting stops the rule with ValidationException")
    void invalidSetting() throws Exception {
        when(settings.reminderHoursBefore()).thenThrow(new ValidationException("valor inválido"));
        Appointment appointment = appointmentAt(NOW.plusDays(1), 0);

        assertThatThrownBy(() -> new ReminderRule(settings).apply(appointment))
            .isInstanceOf(ValidationException.class);
        assertThat(appointment.getReminderAt()).isNull();
    }

    @Test
    @DisplayName("every rule has a name for the messages shown to the user")
    void ruleNames() {
        List<AppointmentRule> rules = List.of(new DepositRule(settings), new ConfirmationRule(settings, CLOCK),
            new ReminderRule(settings));

        assertThat(rules).extracting(AppointmentRule::getName)
            .containsExactly("Anticipo mínimo", "Aviso mínimo", "Recordatorio");
    }

    @Test
    @DisplayName("AppointmentService applies the three rules without changes to the service")
    @SuppressWarnings("unchecked")
    void rulesInsideAppointmentService() throws Exception {
        AppointmentDao appointmentDao = mock(AppointmentDao.class);
        when(appointmentDao.findByProfessional(1)).thenReturn(List.of());
        when(appointmentDao.create(any())).thenAnswer(call -> call.getArgument(0));
        AppointmentService service = new AppointmentService(appointmentDao, mock(CrudDao.class),
            List.of(new DepositRule(settings), new ConfirmationRule(settings, CLOCK), new ReminderRule(settings)));

        Appointment booked = service.book(ana, laura, haircut, NOW.plusDays(2), 12000);

        assertThat(booked.getReminderAt()).isEqualTo(NOW.plusDays(1));
        assertThatThrownBy(() -> service.book(ana, laura, haircut, NOW.plusDays(2).plusHours(1), 5000))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("anticipo mínimo");
    }

    @Test
    @DisplayName("a customer over the no-show threshold cannot book without paying in full")
    @SuppressWarnings("unchecked")
    void penalizedCustomerIsNotStored() throws Exception {
        Customer missing = new Customer(2, "1002", "Marta Díaz", "3004445555", "marta@correo.com",
            LocalDate.of(2026, 1, 10), 4);
        AppointmentDao appointmentDao = mock(AppointmentDao.class);
        when(appointmentDao.findByProfessional(1)).thenReturn(List.of());
        AppointmentService service = new AppointmentService(appointmentDao, mock(CrudDao.class),
            List.of(new DepositRule(settings)));

        assertThatThrownBy(() -> service.book(missing, laura, haircut, NOW.plusDays(2), 12000))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("$ 40.000");
        verify(appointmentDao, never()).create(any());
    }
}
