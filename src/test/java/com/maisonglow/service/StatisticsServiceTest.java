package com.maisonglow.service;

import com.maisonglow.dao.AppointmentDao;
import com.maisonglow.dao.SaleDao;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Product;
import com.maisonglow.model.ProductCategory;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Sale;
import com.maisonglow.model.SaleLine;
import com.maisonglow.model.Sellable;
import com.maisonglow.model.Treatment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatisticsService tests")
class StatisticsServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 10, 1);
    private static final LocalDate TO = LocalDate.of(2026, 10, 31);
    private static final LocalDateTime SOLD_AT = LocalDateTime.of(2026, 10, 10, 11, 0);

    @Mock
    private SaleDao saleDao;
    @Mock
    private AppointmentDao appointmentDao;

    private StatisticsService service;

    private final Customer ana = new Customer(1, "1001", "Ana Ruiz", "3001112222", "ana@correo.com");
    private final Customer luis = new Customer(2, "1002", "Luis Mora", "3003334444", "luis@correo.com");
    private final Professional laura = new Professional(1, "2002", "Laura Gómez", "3157778888",
        "laura@correo.com", "Peluquería", LocalTime.of(8, 0), LocalTime.of(17, 0));
    private final ProductCategory hair = new ProductCategory(1, "Cabello");
    private final Product shampoo = new Product(1, "Shampoo", hair, 28000, 20, 2);
    private final Product mask = new Product(2, "Mascarilla", hair, 35000, 20, 2);
    private final Treatment haircut = new Treatment(1, "Corte de cabello", "Corte y secado", 35000, 45);
    private final Treatment manicure = new Treatment(2, "Manicure", "Limado y esmalte", 25000, 40);

    @BeforeEach
    void setUp() {
        service = new StatisticsService(saleDao, appointmentDao);
    }

    private Sale sale(int id, Customer customer, Sellable item, int quantity) throws Exception {
        return Sale.restore(id, customer, SOLD_AT, List.of(SaleLine.restore(item, quantity, item.getPrice())));
    }

    private Appointment appointment(int id, Customer customer, Treatment treatment) {
        return new Appointment(id, customer, laura, treatment, LocalDateTime.of(2026, 10, 20, 9, 0));
    }

    @Test
    @DisplayName("customerStatistics counts purchases, spending, appointments and no-shows")
    void customerStatistics() throws Exception {
        when(saleDao.findByCustomer(1)).thenReturn(List.of(sale(1, ana, shampoo, 2), sale(2, ana, haircut, 1)));
        Appointment completed = appointment(1, ana, haircut);
        completed.complete();
        Appointment missed = appointment(2, ana, haircut);
        missed.markNoShow();
        when(appointmentDao.findAll()).thenReturn(List.of(completed, missed, appointment(3, ana, manicure),
            appointment(4, luis, manicure)));

        CustomerStatistics statistics = service.customerStatistics(1);

        assertThat(statistics).isEqualTo(new CustomerStatistics(2, 91000, 3, 1, 1));
    }

    @Test
    @DisplayName("customerStatistics is all zero for a customer without activity")
    void customerWithoutActivity() throws Exception {
        when(saleDao.findByCustomer(9)).thenReturn(List.of());
        when(appointmentDao.findAll()).thenReturn(List.of());

        assertThat(service.customerStatistics(9)).isEqualTo(new CustomerStatistics(0, 0, 0, 0, 0));
    }

    @Test
    @DisplayName("topProducts adds units per product, skips treatments and keeps the limit")
    void topProducts() throws Exception {
        when(saleDao.findBetween(FROM, TO)).thenReturn(List.of(sale(1, ana, shampoo, 2), sale(2, luis, mask, 5),
            sale(3, luis, shampoo, 1), sale(4, ana, haircut, 7)));

        assertThat(service.topProducts(5, FROM, TO)).containsExactly(entry("Mascarilla", 5), entry("Shampoo", 3));
        assertThat(service.topProducts(1, FROM, TO)).containsExactly(entry("Mascarilla", 5));
    }

    @Test
    @DisplayName("topProducts rejects a reversed range, a missing date and a limit under one")
    void topProductsInvalid() {
        assertThatThrownBy(() -> service.topProducts(3, TO, FROM)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.topProducts(3, null, TO)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.topProducts(0, FROM, TO))
            .isInstanceOf(ValidationException.class)
            .hasMessage("La cantidad a mostrar debe ser al menos 1.");
    }

    @Test
    @DisplayName("topTreatments ignores cancelled appointments and orders ties by name")
    void topTreatments() throws Exception {
        Appointment cancelled = appointment(3, luis, haircut);
        cancelled.cancel();
        when(appointmentDao.findAll()).thenReturn(List.of(appointment(1, ana, haircut), appointment(2, ana, manicure),
            cancelled));

        assertThat(service.topTreatments(5)).containsExactly(entry("Corte de cabello", 1), entry("Manicure", 1));
    }

    @Test
    @DisplayName("frequentCustomers ranks by number of purchases")
    void frequentCustomers() throws Exception {
        when(saleDao.findAll()).thenReturn(List.of(sale(1, ana, shampoo, 1), sale(2, luis, mask, 1),
            sale(3, luis, haircut, 1)));

        assertThat(service.frequentCustomers(2)).containsExactly(entry("Luis Mora", 2), entry("Ana Ruiz", 1));
    }
}
