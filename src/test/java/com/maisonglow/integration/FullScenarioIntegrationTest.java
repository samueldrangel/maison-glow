package com.maisonglow.integration;

import com.maisonglow.dao.AppointmentDao;
import com.maisonglow.dao.AppointmentDaoText;
import com.maisonglow.dao.CategoryDao;
import com.maisonglow.dao.CategoryDaoText;
import com.maisonglow.dao.CustomerDao;
import com.maisonglow.dao.CustomerDaoText;
import com.maisonglow.dao.ProductDao;
import com.maisonglow.dao.ProductDaoText;
import com.maisonglow.dao.ProfessionalDao;
import com.maisonglow.dao.ProfessionalDaoText;
import com.maisonglow.dao.SaleDao;
import com.maisonglow.dao.SaleDaoText;
import com.maisonglow.dao.SettingDaoText;
import com.maisonglow.dao.TreatmentDao;
import com.maisonglow.dao.TreatmentDaoText;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.AppointmentStatus;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Product;
import com.maisonglow.model.ProductCategory;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Sale;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.AppointmentService;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.CustomerService;
import com.maisonglow.service.CustomerStatistics;
import com.maisonglow.service.LowRotationService;
import com.maisonglow.service.ProductData;
import com.maisonglow.service.ProductSales;
import com.maisonglow.service.ProductService;
import com.maisonglow.service.ProfessionalService;
import com.maisonglow.service.PromotionService;
import com.maisonglow.service.PromotionSuggestion;
import com.maisonglow.service.SaleService;
import com.maisonglow.service.SettingsService;
import com.maisonglow.service.SimulatedAdminNotifier;
import com.maisonglow.service.SimulatedEmailNotifier;
import com.maisonglow.service.StatisticsService;
import com.maisonglow.service.TreatmentData;
import com.maisonglow.service.TreatmentService;
import com.maisonglow.service.WorkSchedule;
import com.maisonglow.service.rule.AppointmentRule;
import com.maisonglow.service.rule.ConfirmationRule;
import com.maisonglow.service.rule.DepositRule;
import com.maisonglow.service.rule.ReminderRule;
import com.maisonglow.util.InvoiceFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the complete Delivery 1 scenario over text files in a temporary
 * folder: registration, booking with rules, sale with stock deduction,
 * invoice, statistics, low rotation and promotions. Every check after a
 * write rebuilds the DAOs, so the data really comes back from the files.
 */
@DisplayName("Full scenario integration test")
class FullScenarioIntegrationTest {

    private static final PrintStream SILENT = new PrintStream(OutputStream.nullOutputStream());
    private static final WorkSchedule EIGHT_TO_SIX = new WorkSchedule(LocalTime.of(8, 0), LocalTime.of(18, 0));

    @TempDir
    Path data;

    private CustomerDao customerDao;
    private TreatmentDao treatmentDao;
    private ProfessionalDao professionalDao;
    private CategoryDao categoryDao;
    private ProductDao productDao;
    private AppointmentDao appointmentDao;
    private SaleDao saleDao;
    private SettingsService settings;
    private List<AppointmentRule> rules;

    private CustomerService customers;
    private TreatmentService treatments;
    private ProfessionalService professionals;
    private ProductService products;
    private AppointmentService appointments;
    private SaleService sales;
    private LocalDate today;
    private LocalDateTime tomorrowAtTen;

    @BeforeEach
    void setUp() throws IOException {
        Files.copy(Path.of("data", "settings.txt"), data.resolve("settings.txt"));
        Files.copy(Path.of("data", "categories.txt"), data.resolve("categories.txt"));
        wire();
        today = LocalDate.now();
        tomorrowAtTen = today.plusDays(1).atTime(10, 0);
    }

    private void wire() {
        customerDao = new CustomerDaoText(data.resolve("customers.txt"));
        treatmentDao = new TreatmentDaoText(data.resolve("treatments.txt"));
        professionalDao = new ProfessionalDaoText(data.resolve("professionals.txt"),
            data.resolve("professional_treatments.txt"), treatmentDao);
        categoryDao = new CategoryDaoText(data.resolve("categories.txt"));
        productDao = new ProductDaoText(data.resolve("products.txt"), categoryDao);
        appointmentDao = new AppointmentDaoText(data.resolve("appointments.txt"), customerDao, professionalDao,
            treatmentDao);
        saleDao = new SaleDaoText(data.resolve("sales.txt"), data.resolve("sale_lines.txt"), customerDao,
            appointmentDao, productDao, treatmentDao);
        settings = new SettingsService(new SettingDaoText(data.resolve("settings.txt")));
        rules = new ArrayList<>(List.of(new DepositRule(settings),
            new ConfirmationRule(settings, Clock.systemDefaultZone()), new ReminderRule(settings)));

        customers = new CustomerService(customerDao);
        treatments = new TreatmentService(treatmentDao);
        professionals = new ProfessionalService(professionalDao, treatmentDao);
        products = new ProductService(productDao, categoryDao);
        appointments = new AppointmentService(appointmentDao, customerDao, rules);
        sales = new SaleService(saleDao, productDao);
    }

    private Professional professionalOffering(ContactData contact, String specialty, Treatment treatment)
        throws Exception {
        Professional professional = professionals.register(contact, specialty, EIGHT_TO_SIX);
        professionals.assignTreatment(professional.getId(), treatment.getId());
        return professionals.findById(professional.getId());
    }

    @Test
    @DisplayName("register, book, sell and query statistics end to end")
    void fullScenario() throws Exception {
        Customer ana = customers.register(new ContactData("1065123456", "Ana Pérez", "3001234567",
            "ana@correo.com"));
        Treatment haircut = treatments.register(new TreatmentData("Corte de cabello", "Corte y secado", 40000, 45));
        Professional laura = professionalOffering(new ContactData("49777888", "Laura Gómez", "3157778888",
            "laura@correo.com"), "Peluquería", haircut);
        ProductCategory hair = products.listCategories().get(0);
        Product shampoo = products.register(new ProductData("Shampoo hidratante", hair.getId(), 28000, 2), 5);

        Appointment booked = appointments.book(ana, laura, haircut, tomorrowAtTen, 12000);

        assertThat(booked.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(booked.getReminderAt()).isEqualTo(tomorrowAtTen.minusHours(24));
        assertThatThrownBy(() -> appointments.book(ana, laura, haircut, tomorrowAtTen.plusMinutes(30), 12000))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("ya tiene una cita");

        Sale sale = new Sale(ana);
        sale.addLine(products.findById(shampoo.getId()), 3);
        sale.addLine(haircut, 1);
        sale.linkAppointment(booked);
        Sale registered = sales.register(sale);

        SimulatedEmailNotifier email = new SimulatedEmailNotifier(new InvoiceFormatter(settings.businessName()),
            SILENT);
        email.sendInvoice(ana, registered);

        wire();
        assertThat(products.findById(shampoo.getId()).getStock()).isEqualTo(2);
        assertThat(products.listLowStock()).extracting(Product::getName).containsExactly("Shampoo hidratante");
        assertThat(sales.listByCustomer(ana.getId())).singleElement()
            .satisfies(stored -> assertThat(stored.getAppointment()).contains(booked));
        assertThat(email.getSentMessages().get(0)).contains("Maison Glow", "124.000");

        StatisticsService statistics = new StatisticsService(saleDao, appointmentDao);
        assertThat(statistics.customerStatistics(ana.getId()))
            .isEqualTo(new CustomerStatistics(1, 124000, 1, 0, 0));
        assertThat(statistics.topProducts(3, today, today)).containsEntry("Shampoo hidratante", 3);
        assertThat(statistics.topTreatments(3)).containsEntry("Corte de cabello", 1);
    }

    @Test
    @DisplayName("a sale with more units than the stock fails and changes nothing")
    void saleWithoutStockChangesNothing() throws Exception {
        Customer ana = customers.register(new ContactData("1065123456", "Ana Pérez", "3001234567",
            "ana@correo.com"));
        Product shampoo = products.register(new ProductData("Shampoo", 1, 28000, 1), 2);
        Product gel = products.register(new ProductData("Gel", 1, 15000, 1), 10);

        Sale sale = new Sale(ana);
        sale.addLine(products.findById(gel.getId()), 4);
        sale.addLine(products.findById(shampoo.getId()), 3);

        assertThatThrownBy(() -> sales.register(sale))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Stock insuficiente de Shampoo");
        wire();
        assertThat(products.findById(gel.getId()).getStock()).isEqualTo(10);
        assertThat(saleDao.findAll()).isEmpty();
    }

    @Test
    @DisplayName("a repeated no-show customer must pay the full price and a new rule plugs in without changes")
    void penaltyAndNewRule() throws Exception {
        Customer luis = customers.register(new ContactData("77111222", "Luis Díaz", "3012223344",
            "luis@correo.com"));
        Treatment manicure = treatments.register(new TreatmentData("Manicure", "Limpieza y esmalte", 25000, 40));
        Professional sara = professionalOffering(new ContactData("1003", "Sara Ruiz", "3204445566",
            "sara@correo.com"), "Uñas", manicure);
        for (int i = 0; i < 3; i++) {
            customers.registerNoShow(luis.getId());
        }
        Customer penalized = customers.findById(luis.getId());

        assertThatThrownBy(() -> appointments.book(penalized, sara, manicure, tomorrowAtTen, 7500))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("25.000");
        assertThat(appointmentDao.findAll()).isEmpty();

        rules.add(new AppointmentRule() {
            @Override
            public void apply(Appointment appointment) throws BusinessRuleException {
                if (appointment.getStart().getHour() < 9) {
                    throw new BusinessRuleException("No se agenda antes de las 9.");
                }
            }

            @Override
            public String getName() {
                return "Horario de apertura";
            }
        });
        AppointmentService withNewRule = new AppointmentService(appointmentDao, customerDao, rules);
        assertThatThrownBy(() -> withNewRule.book(penalized, sara, manicure, tomorrowAtTen.withHour(8), 25000))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("antes de las 9");
    }

    @Test
    @DisplayName("low-rotation products are reported and suggested as promotions")
    void lowRotation() throws Exception {
        Customer ana = customers.register(new ContactData("1065123456", "Ana Pérez", "3001234567",
            "ana@correo.com"));
        Product shampoo = products.register(new ProductData("Shampoo", 1, 28000, 1), 20);
        products.register(new ProductData("Laca", 1, 18000, 1), 20);
        Sale sale = new Sale(ana);
        sale.addLine(products.findById(shampoo.getId()), 2);
        sales.register(sale);

        wire();
        LowRotationService lowRotation = new LowRotationService(saleDao, productDao, settings);
        SimulatedAdminNotifier admin = new SimulatedAdminNotifier(SILENT);

        assertThat(lowRotation.notifyAdministrator(admin, today))
            .extracting(ProductSales::unitsSold).containsExactly(0, 2);
        assertThat(admin.getNotifications()).hasSize(1);
        assertThat(new PromotionService(lowRotation, saleDao, settings).suggestPromotions(ana.getId(), today))
            .containsExactly(new PromotionSuggestion("Laca", 18000, 15, 15300));
    }
}
