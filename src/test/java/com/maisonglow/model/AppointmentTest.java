package com.maisonglow.model;

import com.maisonglow.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Appointment tests")
class AppointmentTest {

    private static final LocalDateTime NINE = LocalDateTime.of(2026, 10, 20, 9, 0);

    private final Customer ana = new Customer(1, "1001", "Ana Ruiz", "3001112222", "ana@correo.com");
    private final Professional laura = new Professional(1, "2002", "Laura Gómez", "3157778888",
        "laura@correo.com", "Peluquería", LocalTime.of(8, 0), LocalTime.of(17, 0));
    private final Treatment haircut = new Treatment(1, "Corte de cabello", "Corte y secado", 35000, 45);

    private Appointment bookAt(LocalDateTime start) {
        return new Appointment(0, ana, laura, haircut, start);
    }

    @Test
    @DisplayName("a new appointment is pending, blocks the schedule and ends after the treatment")
    void newAppointmentDefaults() {
        Appointment appointment = bookAt(NINE);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(appointment.blocksSchedule()).isTrue();
        assertThat(appointment.getEnd()).isEqualTo(NINE.plusMinutes(45));
        assertThat(appointment.getDeposit()).isZero();
        assertThat(appointment.getReminderAt()).isNull();
    }

    @Test
    @DisplayName("the end does not move if the treatment duration changes later")
    void endIsFixedWhenBooked() {
        Appointment appointment = bookAt(NINE);

        haircut.setDurationMinutes(90);

        assertThat(appointment.getEnd()).isEqualTo(NINE.plusMinutes(45));
    }

    @Test
    @DisplayName("overlapsWith detects shared time and ignores touching slots")
    void overlapsWith() {
        Appointment nine = bookAt(NINE);

        assertThat(nine.overlapsWith(bookAt(NINE.plusMinutes(30)))).isTrue();
        assertThat(nine.overlapsWith(bookAt(NINE.minusMinutes(30)))).isTrue();
        assertThat(nine.overlapsWith(bookAt(NINE.plusMinutes(45)))).isFalse();
        assertThat(nine.overlapsWith(bookAt(NINE.minusMinutes(45)))).isFalse();
    }

    @Test
    @DisplayName("a pending appointment can be confirmed and then completed")
    void confirmThenComplete() throws BusinessRuleException {
        Appointment appointment = bookAt(NINE);

        appointment.confirm();
        appointment.complete();

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(appointment.blocksSchedule()).isFalse();
    }

    @Test
    @DisplayName("cancel frees the schedule")
    void cancelFreesSchedule() throws BusinessRuleException {
        Appointment appointment = bookAt(NINE);

        appointment.cancel();

        assertThat(appointment.blocksSchedule()).isFalse();
    }

    @Test
    @DisplayName("markNoShow adds a no-show to the customer")
    void markNoShowCountsForCustomer() throws BusinessRuleException {
        Appointment appointment = bookAt(NINE);

        appointment.markNoShow();

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);
        assertThat(ana.getNoShowCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("confirming twice fails with a readable message")
    void confirmTwiceFails() throws BusinessRuleException {
        Appointment appointment = bookAt(NINE);
        appointment.confirm();

        assertThatThrownBy(appointment::confirm)
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("No se puede confirmar una cita confirmada.");
    }

    @Test
    @DisplayName("a closed appointment cannot change again")
    void closedAppointmentCannotChange() throws BusinessRuleException {
        Appointment appointment = bookAt(NINE);
        appointment.cancel();

        assertThatThrownBy(appointment::confirm).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(appointment::complete).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(appointment::cancel).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(appointment::markNoShow).isInstanceOf(BusinessRuleException.class);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("a rejected no-show does not add to the customer")
    void rejectedNoShowDoesNotCount() throws BusinessRuleException {
        Appointment appointment = bookAt(NINE);
        appointment.complete();

        assertThatThrownBy(appointment::markNoShow).isInstanceOf(BusinessRuleException.class);
        assertThat(ana.getNoShowCount()).isZero();
    }

    @Test
    @DisplayName("a negative deposit is rejected")
    void negativeDepositFails() {
        Appointment appointment = bookAt(NINE);

        assertThatThrownBy(() -> appointment.setDeposit(-1)).isInstanceOf(BusinessRuleException.class);
        assertThat(appointment.getDeposit()).isZero();
    }

    @Test
    @DisplayName("restore keeps the stored end, status and creation time")
    void restoreKeepsStoredData() {
        LocalDateTime created = NINE.minusDays(2);

        Appointment restored = Appointment.restore(7, ana, laura, haircut, NINE, NINE.plusMinutes(60),
            AppointmentStatus.CONFIRMED, created);

        assertThat(restored.getId()).isEqualTo(7);
        assertThat(restored.getEnd()).isEqualTo(NINE.plusMinutes(60));
        assertThat(restored.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(restored.getCreatedAt()).isEqualTo(created);
    }
}
