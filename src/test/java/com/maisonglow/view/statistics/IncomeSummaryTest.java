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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IncomeSummary tests")
class IncomeSummaryTest {

    private final Customer ana = new Customer(1, "1001", "Ana García", "3001112233", "ana@correo.com");
    private final Professional camila = new Professional(1, "2001", "Camila Ruiz", "3001112233", "c@correo.com", "Estilista",
        LocalTime.of(8, 0), LocalTime.of(17, 0));
    private final Professional daniela = new Professional(2, "2002", "Daniela Soto", "3124445566", "d@correo.com", "Manicurista",
        LocalTime.of(9, 0), LocalTime.of(18, 0));
    private final Treatment cut = new Treatment(1, "Corte", "Corte y peinado", 55000, 60);
    private final Treatment manicure = new Treatment(2, "Manicura", "Manicura gel", 45000, 50);
    private final Product shampoo = new Product(1, "Champú", new ProductCategory(1, "Cabello"), new BigDecimal("28000"), 10, 5);

    private Appointment appointment(int id, Professional professional, Treatment treatment, AppointmentStatus status, int deposit) {
        Appointment appointment = new Appointment(id, ana, professional, treatment, LocalDateTime.of(2026, 11, 10, 10, 0),
            LocalDateTime.of(2026, 11, 10, 11, 0), status, LocalDateTime.of(2026, 10, 1, 9, 0));
        appointment.setDeposit(BigDecimal.valueOf(deposit));
        return appointment;
    }

    private Sale sale(int id, int units) {
        return Sale.restore(id, ana, LocalDateTime.of(2026, 11, 5, 11, 0), List.of(SaleLine.restore(shampoo, units, shampoo.getPrice())),
            shampoo.getPrice().multiply(BigDecimal.valueOf(units)));
    }

    @Test
    @DisplayName("adds the sales and the price of the completed appointments, and keeps them apart")
    void totalIncome() {
        IncomeSummary income = IncomeSummary.of(List.of(sale(1, 2)), List.of(
            appointment(1, camila, cut, AppointmentStatus.COMPLETED, 16500), appointment(2, daniela, manicure, AppointmentStatus.COMPLETED, 13500)));

        assertThat(income.salesIncome()).isEqualByComparingTo("56000");
        assertThat(income.treatmentIncome()).isEqualByComparingTo("100000");
        assertThat(income.totalIncome()).isEqualByComparingTo("156000");
    }

    @Test
    @DisplayName("deposits count only for appointments still to be attended, and missed or cancelled ones are lost income")
    void depositsAndLostIncome() {
        IncomeSummary income = IncomeSummary.of(List.of(), List.of(
            appointment(1, camila, cut, AppointmentStatus.PENDING, 16500), appointment(2, camila, cut, AppointmentStatus.CONFIRMED, 16500),
            appointment(3, daniela, manicure, AppointmentStatus.NO_SHOW, 13500), appointment(4, daniela, manicure, AppointmentStatus.CANCELLED, 13500),
            appointment(5, camila, cut, AppointmentStatus.COMPLETED, 16500)));

        assertThat(income.pendingDeposits()).isEqualByComparingTo("33000");
        assertThat(income.lostIncome()).isEqualByComparingTo("90000");
        assertThat(income.lostAppointments()).isEqualTo(2);
        assertThat(income.treatmentIncome()).isEqualByComparingTo("55000");
    }

    @Test
    @DisplayName("ranks the income of the completed appointments by professional and by treatment, highest first")
    void rankings() {
        IncomeSummary income = IncomeSummary.of(List.of(), List.of(
            appointment(1, camila, cut, AppointmentStatus.COMPLETED, 0), appointment(2, camila, cut, AppointmentStatus.COMPLETED, 0),
            appointment(3, daniela, manicure, AppointmentStatus.COMPLETED, 0), appointment(4, daniela, cut, AppointmentStatus.PENDING, 0)));

        assertThat(income.byProfessional().keySet()).containsExactly("Camila Ruiz", "Daniela Soto");
        assertThat(income.byProfessional().get("Camila Ruiz")).isEqualByComparingTo("110000");
        assertThat(income.byTreatment().keySet()).containsExactly("Corte", "Manicura");
    }

    @Test
    @DisplayName("an empty range has zero income and empty rankings")
    void emptyRange() {
        IncomeSummary income = IncomeSummary.of(List.of(), List.of());

        assertThat(income.totalIncome()).isEqualByComparingTo("0");
        assertThat(income.pendingDeposits()).isEqualByComparingTo("0");
        assertThat(income.byProfessional()).isEmpty();
        assertThat(income.byTreatment()).isEmpty();
    }
}
