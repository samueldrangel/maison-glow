package com.maisonglow.dao;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
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
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SaleDaoText tests")
class SaleDaoTextTest {

    private static final LocalDateTime OCT_1_AT_15 = LocalDateTime.of(2026, 10, 1, 15, 0);

    private final Customer ana = new Customer(1, "1001", "Ana Ruiz", "3001112222", "ana@correo.com");
    private final Customer luis = new Customer(2, "1002", "Luis Mora", "3004445555", "luis@correo.com");
    private final Product shampoo = new Product(1, "Champú", new ProductCategory(1, "Cuidado capilar"), 28000, 10, 2);
    private final Treatment haircut = new Treatment(1, "Corte de cabello", "Corte y secado", 35000, 45);
    private final Professional laura = new Professional(1, "2002", "Laura Gómez", "3157778888",
        "laura@correo.com", "Peluquería", LocalTime.of(8, 0), LocalTime.of(17, 0));
    private final Appointment anaHaircut = new Appointment(1, ana, laura, haircut, LocalDateTime.of(2026, 10, 20, 9, 0));

    @TempDir
    Path folder;

    private Path salesFile;
    private Path linesFile;
    private InMemoryDao<Product> products;
    private SaleDaoText dao;

    private SaleDaoText openDao() {
        return new SaleDaoText(salesFile, linesFile,
            new InMemoryDao<>(Customer::getId, ana, luis),
            new InMemoryDao<>(Appointment::getId, anaHaircut),
            products,
            new InMemoryDao<>(Treatment::getId, haircut));
    }

    @BeforeEach
    void setUp() {
        salesFile = folder.resolve("sales.txt");
        linesFile = folder.resolve("sale_lines.txt");
        products = new InMemoryDao<>(Product::getId, shampoo);
        dao = openDao();
    }

    private Sale mixedSale(Customer customer) throws BusinessRuleException {
        Sale sale = new Sale(customer);
        sale.addLine(shampoo, 2);
        sale.addLine(haircut, 1);
        return sale;
    }

    private Sale storedSale(Customer customer, LocalDateTime soldAt) throws Exception {
        return dao.create(Sale.restore(0, customer, soldAt, List.of(SaleLine.restore(shampoo, 1, 28000))));
    }

    @Test
    @DisplayName("a mixed sale reloads with the same lines, prices and total")
    void mixedSaleRoundTrip() throws Exception {
        Sale sale = mixedSale(ana);
        dao.create(sale);
        shampoo.setPrice(40000);

        Sale loaded = openDao().findById(sale.getId()).orElseThrow();

        assertThat(loaded.getCustomer()).isSameAs(ana);
        assertThat(loaded.getSoldAt()).isEqualTo(sale.getSoldAt());
        assertThat(loaded.getLines()).extracting(SaleLine::getItem).containsExactly(shampoo, haircut);
        assertThat(loaded.getLines()).extracting(SaleLine::getQuantity).containsExactly(2, 1);
        assertThat(loaded.getLines()).extracting(SaleLine::getUnitPrice).containsExactly(28000.0, 35000.0);
        assertThat(loaded.calculateTotal()).isEqualTo(2 * 28000 + 35000);
        assertThat(loaded.getAppointment()).isEmpty();
    }

    @Test
    @DisplayName("the link to the appointment survives a reload")
    void appointmentLinkRoundTrip() throws Exception {
        Sale sale = new Sale(ana);
        sale.addLine(haircut, 1);
        sale.linkAppointment(anaHaircut);
        dao.create(sale);

        assertThat(openDao().findById(sale.getId()).orElseThrow().getAppointment()).contains(anaHaircut);
    }

    @Test
    @DisplayName("both files follow the column order of the sale and sale_line tables")
    void filesFollowSchemaColumns() throws Exception {
        Sale sale = mixedSale(ana);
        sale.linkAppointment(anaHaircut);
        dao.create(sale);

        assertThat(Files.readAllLines(salesFile).get(1))
            .isEqualTo("1|1|1|" + sale.getSoldAt() + "|91000.0");
        assertThat(Files.readAllLines(linesFile)).containsExactly(
            "#nextId=3",
            "1|1|1|||2|28000.0",
            "2|1||1||1|35000.0");
    }

    @Test
    @DisplayName("findByCustomer returns only that customer's sales, by sale time")
    void findByCustomer() throws Exception {
        storedSale(ana, OCT_1_AT_15.plusDays(2));
        storedSale(luis, OCT_1_AT_15);
        storedSale(ana, OCT_1_AT_15);

        assertThat(dao.findByCustomer(ana.getId()))
            .extracting(Sale::getSoldAt)
            .containsExactly(OCT_1_AT_15, OCT_1_AT_15.plusDays(2));
    }

    @Test
    @DisplayName("findBetween includes both days and leaves out the rest")
    void findBetween() throws Exception {
        storedSale(ana, OCT_1_AT_15.minusDays(1));
        storedSale(ana, OCT_1_AT_15.plusDays(3));
        storedSale(luis, OCT_1_AT_15);
        storedSale(ana, OCT_1_AT_15.plusDays(4));

        assertThat(dao.findBetween(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 4)))
            .extracting(Sale::getSoldAt)
            .containsExactly(OCT_1_AT_15, OCT_1_AT_15.plusDays(3));
    }

    @Test
    @DisplayName("update replaces the stored lines of the sale")
    void updateReplacesLines() throws Exception {
        Sale sale = dao.create(mixedSale(ana));
        sale.addLine(shampoo, 1);

        dao.update(sale);

        Sale loaded = openDao().findById(sale.getId()).orElseThrow();
        assertThat(loaded.getLines()).extracting(SaleLine::getQuantity).containsExactly(3, 1);
    }

    @Test
    @DisplayName("delete removes the sale together with its lines")
    void deleteRemovesLines() throws Exception {
        Sale kept = dao.create(mixedSale(luis));
        Sale removed = dao.create(mixedSale(ana));

        dao.delete(removed.getId());

        SaleDaoText reopened = openDao();
        assertThat(reopened.findAll()).containsExactly(kept);
        assertThat(reopened.findById(kept.getId()).orElseThrow().getLines()).hasSize(2);
        assertThat(Files.readAllLines(linesFile)).hasSize(3);
    }

    @Test
    @DisplayName("if the lines cannot be saved, the sale is not stored either")
    void failedLinesUndoTheSale() throws Exception {
        Sale sale = new Sale(ana);
        sale.addLine(shampoo, 1);
        sale.addLine(new GiftCard(), 1);

        assertThatThrownBy(() -> dao.create(sale)).isInstanceOf(DataAccessException.class);

        assertThat(sale.getId()).isZero();
        assertThat(dao.findAll()).isEmpty();
        assertThat(openDao().findAll()).isEmpty();
    }

    @Test
    @DisplayName("a line whose product no longer exists is reported")
    void missingProductFails() throws Exception {
        dao.create(mixedSale(ana));
        products.delete(shampoo.getId());

        assertThatThrownBy(() -> openDao().findAll())
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("producto con id 1");
    }

    @Test
    @DisplayName("a line with both a product and a treatment is reported")
    void lineWithBothItemsFails() throws Exception {
        Files.writeString(salesFile, "1|1||2026-10-01T15:00|63000.0\n");
        Files.writeString(linesFile, "1|1|1|1||1|28000.0\n");

        assertThatThrownBy(() -> dao.findAll())
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("no ambos ni ninguno");
    }

    @Test
    @DisplayName("a sale line with missing fields is reported")
    void incompleteSaleFails() throws Exception {
        Files.writeString(salesFile, "1|1|\n");

        assertThatThrownBy(() -> dao.findAll())
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("Se esperaban 5 campos");
    }

    private static final class GiftCard implements Sellable {

        @Override
        public int getId() {
            return 9;
        }

        @Override
        public String getName() {
            return "Tarjeta de regalo";
        }

        @Override
        public double getPrice() {
            return 50000;
        }
    }
}
