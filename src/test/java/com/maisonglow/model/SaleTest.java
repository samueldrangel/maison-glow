package com.maisonglow.model;

import com.maisonglow.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Sale tests")
class SaleTest {

    private final Customer ana = new Customer(1, "1001", "Ana Ruiz", "3001112222", "ana@correo.com");
    private final Customer luis = new Customer(2, "1002", "Luis Mora", "3004445555", "luis@correo.com");
    private final ProductCategory hairCare = new ProductCategory(1, "Cuidado capilar");
    private final Product shampoo = new Product(1, "Champú", hairCare, 28000, 10, 2);
    private final Treatment haircut = new Treatment(1, "Corte de cabello", "Corte y secado", 35000, 45);
    private final Professional laura = new Professional(1, "2002", "Laura Gómez", "3157778888",
        "laura@correo.com", "Peluquería", LocalTime.of(8, 0), LocalTime.of(17, 0));

    @Test
    @DisplayName("a new sale has no lines, no appointment and is sold now")
    void newSaleDefaults() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        Sale sale = new Sale(ana);

        assertThat(sale.getLines()).isEmpty();
        assertThat(sale.calculateTotal()).isZero();
        assertThat(sale.getAppointment()).isEmpty();
        assertThat(sale.getSoldAt()).isAfter(before);
    }

    @Test
    @DisplayName("a mixed sale of a product and a treatment adds up both lines")
    void mixedSaleTotal() throws BusinessRuleException {
        Sale sale = new Sale(ana);

        sale.addLine(shampoo, 2);
        sale.addLine(haircut, 1);

        assertThat(sale.getLines()).hasSize(2);
        assertThat(sale.calculateTotal()).isEqualTo(2 * 28000 + 35000);
    }

    @Test
    @DisplayName("addLine copies the current price, so a later price change does not alter the sale")
    void lineKeepsPriceOfTheMoment() throws BusinessRuleException {
        Sale sale = new Sale(ana);
        SaleLine line = sale.addLine(shampoo, 1);

        shampoo.setPrice(40000);

        assertThat(line.getUnitPrice()).isEqualTo(28000);
        assertThat(sale.calculateTotal()).isEqualTo(28000);
    }

    @Test
    @DisplayName("adding an item that is already in the sale adds units to its line")
    void sameItemAddsUnits() throws BusinessRuleException {
        Sale sale = new Sale(ana);
        SaleLine first = sale.addLine(shampoo, 1);

        SaleLine second = sale.addLine(shampoo, 2);

        assertThat(second).isSameAs(first);
        assertThat(sale.getLines()).hasSize(1);
        assertThat(first.getQuantity()).isEqualTo(3);
        assertThat(first.calculateSubtotal()).isEqualTo(3 * 28000);
    }

    @Test
    @DisplayName("a product and a treatment with the same id are different lines")
    void sameIdDifferentKind() throws BusinessRuleException {
        Sale sale = new Sale(ana);

        sale.addLine(shampoo, 1);
        sale.addLine(haircut, 1);

        assertThat(sale.getLines()).extracting(SaleLine::getItem).containsExactly(shampoo, haircut);
    }

    @Test
    @DisplayName("a quantity that is not positive is rejected, also when adding to an existing line")
    void nonPositiveQuantity() throws BusinessRuleException {
        Sale sale = new Sale(ana);
        sale.addLine(shampoo, 1);

        assertThatThrownBy(() -> sale.addLine(haircut, 0))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("La cantidad debe ser mayor que cero.");
        assertThatThrownBy(() -> sale.addLine(shampoo, -1)).isInstanceOf(BusinessRuleException.class);
        assertThat(sale.getLines()).hasSize(1);
        assertThat(sale.getLines().get(0).getQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("an item without a positive price cannot be sold")
    void nonPositivePrice() {
        Treatment free = new Treatment(2, "Valoración", "Sin costo", 0, 15);

        assertThatThrownBy(() -> new Sale(ana).addLine(free, 1))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("El precio de Valoración debe ser mayor que cero.");
    }

    @Test
    @DisplayName("the lines cannot be changed from outside the sale")
    void linesAreReadOnly() throws BusinessRuleException {
        Sale sale = new Sale(ana);
        sale.addLine(shampoo, 1);

        assertThatThrownBy(() -> sale.getLines().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("a sale is linked to one appointment of the same customer")
    void linkAppointment() throws BusinessRuleException {
        Sale sale = new Sale(ana);
        Appointment anaAppointment = new Appointment(1, ana, laura, haircut, LocalDateTime.of(2026, 10, 20, 9, 0));
        Appointment luisAppointment = new Appointment(2, luis, laura, haircut, LocalDateTime.of(2026, 10, 20, 10, 0));

        assertThatThrownBy(() -> sale.linkAppointment(luisAppointment))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("La cita es de otro cliente.");
        sale.linkAppointment(anaAppointment);

        assertThat(sale.getAppointment()).contains(anaAppointment);
        assertThatThrownBy(() -> sale.linkAppointment(anaAppointment))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessage("La venta ya está enlazada a una cita.");
    }

    @Test
    @DisplayName("restore keeps the stored id, time and the price each line was sold at")
    void restoreKeepsStoredData() throws BusinessRuleException {
        LocalDateTime soldAt = LocalDateTime.of(2026, 10, 1, 15, 30);
        SaleLine stored = SaleLine.restore(shampoo, 2, 25000);

        Sale sale = Sale.restore(5, ana, soldAt, List.of(stored));

        assertThat(sale.getId()).isEqualTo(5);
        assertThat(sale.getSoldAt()).isEqualTo(soldAt);
        assertThat(sale.calculateTotal()).isEqualTo(50000);
    }

    @Test
    @DisplayName("a stored line without a positive price is rejected")
    void restoreRejectsInvalidLine() {
        assertThatThrownBy(() -> SaleLine.restore(shampoo, 1, -5)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> SaleLine.restore(shampoo, 0, 28000)).isInstanceOf(BusinessRuleException.class);
    }
}
