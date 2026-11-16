package com.maisonglow.view.statistics;

import com.maisonglow.model.Appointment;
import com.maisonglow.model.AppointmentStatus;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Product;
import com.maisonglow.model.ProductCategory;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Sale;
import com.maisonglow.model.SaleLine;
import com.maisonglow.model.Treatment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RangeSummary tests")
class RangeSummaryTest {

    private static final LocalDate FROM = LocalDate.of(2026, 11, 2);
    private static final LocalDate TO = LocalDate.of(2026, 11, 15);

    private final Customer ana = new Customer(1, "1001", "Ana García", "3001112233", "ana@correo.com", FROM.plusDays(1), 0);
    private final Customer laura = new Customer(2, "1002", "Laura Méndez", "3124445566", "l@correo.com", FROM.minusDays(30), 0);
    private final Professional camila = new Professional(1, "2001", "Camila Ruiz", "3001112233", "c@correo.com", "Estilista",
        LocalTime.of(8, 0), LocalTime.of(17, 0));
    private final Treatment cut = new Treatment(1, "Corte", "Corte y peinado", 55000, 60);
    private final Product shampoo = new Product(1, "Champú", new ProductCategory(1, "Cabello"), new BigDecimal("28000"), 10, 5);

    private Appointment appointment(int id, int day, AppointmentStatus status) {
        Appointment appointment = new Appointment(id, ana, camila, cut, LocalDateTime.of(2026, 11, day, 10, 0),
            LocalDateTime.of(2026, 11, day, 11, 0), status, LocalDateTime.of(2026, 10, 1, 9, 0));
        return appointment;
    }

    private Sale sale(int id, int units) {
        return Sale.restore(id, ana, LocalDateTime.of(2026, 11, 5, 11, 0), List.of(SaleLine.restore(shampoo, units, shampoo.getPrice())),
            shampoo.getPrice().multiply(BigDecimal.valueOf(units)));
    }

    @Test
    @DisplayName("adds the sales and counts them")
    void revenue() {
        RangeSummary summary = RangeSummary.of(List.of(sale(1, 2), sale(2, 1)), List.of(), List.of(), FROM, TO);

        assertThat(summary.revenue()).isEqualByComparingTo("84000");
        assertThat(summary.sales()).isEqualTo(2);
    }

    @Test
    @DisplayName("counts the appointments by state and the no-show rate over the ones that took place or were missed")
    void statesAndNoShowRate() {
        RangeSummary summary = RangeSummary.of(List.of(), List.of(
            appointment(1, 3, AppointmentStatus.COMPLETED), appointment(2, 4, AppointmentStatus.COMPLETED),
            appointment(3, 5, AppointmentStatus.COMPLETED), appointment(4, 6, AppointmentStatus.NO_SHOW),
            appointment(5, 7, AppointmentStatus.CANCELLED), appointment(6, 8, AppointmentStatus.PENDING)), List.of(), FROM, TO);

        assertThat(summary.appointments()).isEqualTo(6);
        assertThat(summary.byStatus()).containsEntry(AppointmentStatus.COMPLETED, 3).containsEntry(AppointmentStatus.NO_SHOW, 1)
            .containsEntry(AppointmentStatus.CANCELLED, 1).containsEntry(AppointmentStatus.CONFIRMED, 0);
        assertThat(summary.noShowRate().orElseThrow()).isEqualTo(0.25);
    }

    @Test
    @DisplayName("there is no no-show rate until some appointment has been decided")
    void noRateWithoutDecidedAppointments() {
        assertThat(RangeSummary.of(List.of(), List.of(appointment(1, 3, AppointmentStatus.PENDING)), List.of(), FROM, TO).noShowRate()).isEmpty();
        assertThat(RangeSummary.of(List.of(), List.of(), List.of(), FROM, TO).noShowRate()).isEmpty();
    }

    @Test
    @DisplayName("counts the appointments of each day of the week, leaving the cancelled ones out")
    void byWeekday() {
        RangeSummary summary = RangeSummary.of(List.of(), List.of(
            appointment(1, 2, AppointmentStatus.COMPLETED), appointment(2, 9, AppointmentStatus.PENDING),
            appointment(3, 3, AppointmentStatus.CONFIRMED), appointment(4, 4, AppointmentStatus.CANCELLED)), List.of(), FROM, TO);

        assertThat(summary.byWeekday()).containsEntry(DayOfWeek.MONDAY, 2).containsEntry(DayOfWeek.TUESDAY, 1)
            .containsEntry(DayOfWeek.WEDNESDAY, 0).containsEntry(DayOfWeek.SUNDAY, 0);
    }

    @Test
    @DisplayName("counts as new the customers registered inside the range, both ends included")
    void newCustomers() {
        Customer onTheLimit = new Customer(3, "1003", "Paula Andrés", "3207778899", "p@correo.com", TO, 0);
        Customer tooLate = new Customer(4, "1004", "Valeria Rojas", "3138456721", "v@correo.com", TO.plusDays(1), 0);

        assertThat(RangeSummary.of(List.of(), List.of(), List.of(ana, laura, onTheLimit, tooLate), FROM, TO).newCustomers()).isEqualTo(2);
    }
}
