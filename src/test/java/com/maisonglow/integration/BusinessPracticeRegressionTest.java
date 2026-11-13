package com.maisonglow.integration;

import com.maisonglow.dao.AppointmentDaoText;
import com.maisonglow.dao.CategoryDaoText;
import com.maisonglow.dao.CustomerDaoText;
import com.maisonglow.dao.ProductDaoText;
import com.maisonglow.dao.ProfessionalDaoText;
import com.maisonglow.dao.SaleDaoText;
import com.maisonglow.dao.TreatmentDaoText;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.AppointmentStatus;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Product;
import com.maisonglow.model.ProductCategory;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Sale;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.AppointmentService;
import com.maisonglow.service.ProductData;
import com.maisonglow.service.ProductService;
import com.maisonglow.service.SaleService;
import com.maisonglow.util.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusinessPracticeRegressionTest {
    @TempDir Path folder;
    private CategoryDaoText categories;
    private CustomerDaoText customers;
    private TreatmentDaoText treatments;
    private ProfessionalDaoText professionals;
    private ProductDaoText products;
    private AppointmentDaoText appointments;
    private SaleDaoText sales;
    private Customer customer;
    private Treatment treatment;
    private Professional professional;
    private Product product;
    private AppointmentService appointmentService;
    private SaleService saleService;
    private static final LocalDateTime START = LocalDateTime.of(2026, 12, 1, 10, 0);

    @BeforeEach void setUp() throws Exception {
        categories = new CategoryDaoText(folder.resolve("categories.txt"));
        ProductCategory category = categories.create(new ProductCategory(0, "Cabello"));
        customers = new CustomerDaoText(folder.resolve("customers.txt"));
        customer = customers.create(new Customer(0, "1001", "Ana", "3001112222", "ana@example.com"));
        treatments = new TreatmentDaoText(folder.resolve("treatments.txt"));
        treatment = treatments.create(new Treatment(0, "Corte", "", 100, 30));
        professionals = new ProfessionalDaoText(folder.resolve("professionals.txt"), folder.resolve("links.txt"), treatments);
        professional = new Professional(0, "2001", "Laura", "3002223333", "laura@example.com", "Cabello", LocalTime.of(8, 0), LocalTime.of(18, 0));
        professional.setTreatments(List.of(treatment));
        professionals.create(professional);
        products = new ProductDaoText(folder.resolve("products.txt"), categories);
        product = products.create(new Product(0, "Champú", category, new BigDecimal("0.10"), 5, 1));
        appointments = new AppointmentDaoText(folder.resolve("appointments.txt"), customers, professionals, treatments);
        sales = new SaleDaoText(folder.resolve("sales.txt"), folder.resolve("sale_lines.txt"), customers, appointments, products, treatments);
        appointmentService = new AppointmentService(appointments, customers, List.of());
        saleService = new SaleService(sales, products);
    }

    @Test void transientEntitiesRemainDistinctAndHashCodesSurviveIdAssignment() {
        Product first = new Product(0, "Primero", product.getCategory(), 1, 1, 0);
        Product second = new Product(0, "Segundo", product.getCategory(), 2, 1, 0);
        assertThat(first).isNotEqualTo(second);
        assertThat(new Treatment(0, "A", "", 1, 1)).isNotEqualTo(new Treatment(0, "B", "", 1, 1));
        assertThat(new Sale(customer)).isNotEqualTo(new Sale(customer));
        assertThat(AppointmentService.newAppointment(0, customer, professional, treatment, START))
            .isNotEqualTo(AppointmentService.newAppointment(0, customer, professional, treatment, START));
        assertThat(new ProductCategory(0, "A")).isNotEqualTo(new ProductCategory(0, "B"));
        HashSet<Product> set = new HashSet<>(); set.add(first); first.setId(42);
        assertThat(set.contains(first)).isTrue();
    }

    @Test void distinctUnsavedProductsAreNotMergedAndDecimalTotalsAreExact() throws Exception {
        Sale sale = new Sale(customer);
        Product first = new Product(0, "A", product.getCategory(), new BigDecimal("0.10"), 1, 0);
        Product second = new Product(0, "B", product.getCategory(), new BigDecimal("0.20"), 1, 0);
        SaleService.addLine(sale, first, 1); SaleService.addLine(sale, second, 1);
        assertThat(sale.getLines()).hasSize(2);
        assertThat(sale.getTotal()).isEqualByComparingTo("0.30");
    }

    @Test void decimalPricesAndTotalsSurvivePersistence() throws Exception {
        Sale sale = new Sale(customer); SaleService.addLine(sale, product, 3); saleService.register(sale);
        Sale loaded = new SaleDaoText(folder.resolve("sales.txt"), folder.resolve("sale_lines.txt"), customers, appointments, products, treatments)
            .findById(sale.getId()).orElseThrow();
        assertThat(loaded.getTotal()).isEqualByComparingTo("0.30");
        assertThat(loaded.getLines().get(0).getUnitPrice()).isEqualByComparingTo("0.10");
    }

    @Test void returnedObjectsCannotMutateDaoStateWithoutSaving() throws Exception {
        Product detached = products.findById(product.getId()).orElseThrow(); detached.setStock(999);
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(5);
        products.create(new Product(0, "Otro", product.getCategory(), 1, 2, 0));
        assertThat(new ProductDaoText(folder.resolve("products.txt"), categories).findById(product.getId()).orElseThrow().getStock()).isEqualTo(5);
    }

    @Test void failedStockWritePreservesMemoryAndFile() throws Exception {
        Files.createDirectory(folder.resolve("products.txt.tmp"));
        assertThatThrownBy(() -> new ProductService(products, categories).adjustStock(product.getId(), -1)).isInstanceOf(DataAccessException.class);
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(5);
        assertThat(new ProductDaoText(folder.resolve("products.txt"), categories).findById(product.getId()).orElseThrow().getStock()).isEqualTo(5);
    }

    @Test void failedCustomerWriteRestoresAppointmentAndAllowsRetry() throws Exception {
        Appointment booked = appointmentService.book(customer, professional, treatment, START, 0);
        Path blocker = Files.createDirectory(folder.resolve("customers.txt.tmp"));
        assertThatThrownBy(() -> appointmentService.markNoShow(booked.getId())).isInstanceOf(DataAccessException.class);
        assertThat(appointments.findById(booked.getId()).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(customers.findById(customer.getId()).orElseThrow().getNoShowCount()).isZero();
        assertThat(new AppointmentDaoText(folder.resolve("appointments.txt"), customers, professionals, treatments)
            .findById(booked.getId()).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.PENDING);
        Files.deleteIfExists(blocker);
        appointmentService.markNoShow(booked.getId());
        assertThat(customers.findById(customer.getId()).orElseThrow().getNoShowCount()).isEqualTo(1);
        assertThatThrownBy(() -> appointmentService.markNoShow(booked.getId())).isInstanceOf(BusinessRuleException.class);
        assertThat(customers.findById(customer.getId()).orElseThrow().getNoShowCount()).isEqualTo(1);
    }

    @Test void failedAppointmentWriteDoesNotIncrementCustomer() throws Exception {
        Appointment booked = appointmentService.book(customer, professional, treatment, START, 0);
        Files.createDirectory(folder.resolve("appointments.txt.tmp"));
        assertThatThrownBy(() -> appointmentService.markNoShow(booked.getId())).isInstanceOf(DataAccessException.class);
        assertThat(customers.findById(customer.getId()).orElseThrow().getNoShowCount()).isZero();
        assertThat(appointments.findById(booked.getId()).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.PENDING);
    }

    @Test void failedSaleLineWriteRestoresStockAndRemovesSaleHeader() throws Exception {
        Sale sale = new Sale(customer); SaleService.addLine(sale, product, 3);
        Files.createDirectory(folder.resolve("sale_lines.txt.tmp"));
        assertThatThrownBy(() -> saleService.register(sale)).isInstanceOf(DataAccessException.class);
        assertThat(sale.getId()).isZero(); assertThat(sales.findAll()).isEmpty();
        assertThat(new ProductDaoText(folder.resolve("products.txt"), categories).findById(product.getId()).orElseThrow().getStock()).isEqualTo(5);
        assertThat(product.getStock()).isEqualTo(5);
    }

    @Test void aStaleProductCannotOverwriteCurrentStock() throws Exception {
        Sale first = new Sale(customer); SaleService.addLine(first, product, 3); saleService.register(first);
        Product stale = new Product(product.getId(), product.getName(), product.getCategory(), product.getPrice(), 5, 1);
        Sale second = new Sale(customer); SaleService.addLine(second, stale, 3);
        assertThatThrownBy(() -> saleService.register(second)).isInstanceOf(BusinessRuleException.class);
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(2);
    }

    @Test void nonFiniteNumbersAreRejectedAtInput() {
        for (double value : new double[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }) {
            assertThatThrownBy(() -> Validator.requirePositive(value, "precio")).isInstanceOf(ValidationException.class);
            assertThatThrownBy(() -> new ProductData("A", 1, value, 0)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
