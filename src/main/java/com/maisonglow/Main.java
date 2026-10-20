package com.maisonglow;

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
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Product;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Sale;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.AppointmentService;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.CustomerService;
import com.maisonglow.service.CustomerStatistics;
import com.maisonglow.service.LowRotationService;
import com.maisonglow.service.ProductData;
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
import com.maisonglow.service.rule.ConfirmationRule;
import com.maisonglow.service.rule.DepositRule;
import com.maisonglow.service.rule.ReminderRule;
import com.maisonglow.util.InvoiceFormatter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

/**
 * Console demo of Delivery 1 (N-11). Wires the text-file DAOs and the
 * services (the only place where concrete classes are created) and runs the
 * complete business scenario over a fresh copy of the data folder, so it can
 * be executed any number of times.
 */
public final class Main {

    private static final Path SOURCE_DATA = Path.of("data");
    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");
    private static final WorkSchedule EIGHT_TO_SIX = new WorkSchedule(LocalTime.of(8, 0), LocalTime.of(18, 0));

    private final Path data;
    private final Clock clock = Clock.systemDefaultZone();
    private final CustomerService customers;
    private final TreatmentService treatments;
    private final ProfessionalService professionals;
    private final ProductService products;
    private final AppointmentService appointments;
    private final SaleService sales;
    private final SettingsService settings;
    private final StatisticsService statistics;
    private final LowRotationService lowRotation;
    private final PromotionService promotions;

    private Main(Path data) {
        this.data = data;
        CustomerDao customerDao = new CustomerDaoText(file("customers.txt"));
        TreatmentDao treatmentDao = new TreatmentDaoText(file("treatments.txt"));
        ProfessionalDao professionalDao = new ProfessionalDaoText(file("professionals.txt"),
            file("professional_treatments.txt"), treatmentDao);
        CategoryDao categoryDao = new CategoryDaoText(file("categories.txt"));
        ProductDao productDao = new ProductDaoText(file("products.txt"), categoryDao);
        AppointmentDao appointmentDao = new AppointmentDaoText(file("appointments.txt"), customerDao,
            professionalDao, treatmentDao);
        SaleDao saleDao = new SaleDaoText(file("sales.txt"), file("sale_lines.txt"), customerDao, appointmentDao,
            productDao, treatmentDao);

        settings = new SettingsService(new SettingDaoText(file("settings.txt")));
        customers = new CustomerService(customerDao);
        treatments = new TreatmentService(treatmentDao);
        professionals = new ProfessionalService(professionalDao, treatmentDao);
        products = new ProductService(productDao, categoryDao);
        appointments = new AppointmentService(appointmentDao, customerDao, List.of(new DepositRule(settings),
            new ConfirmationRule(settings, clock), new ReminderRule(settings)));
        sales = new SaleService(saleDao, productDao);
        statistics = new StatisticsService(saleDao, appointmentDao);
        lowRotation = new LowRotationService(saleDao, productDao, settings);
        promotions = new PromotionService(lowRotation, saleDao, settings);
    }

    /**
     * Runs the console demo.
     *
     * @param args the command-line arguments; currently unused
     */
    public static void main(String[] args) {
        System.out.println("Maison Glow - Sistema de gestión para negocios de belleza");
        try {
            new Main(prepareDemoFolder()).run();
        } catch (IOException | DataAccessException e) {
            System.out.println("Error de datos: " + e.getMessage());
        } catch (ValidationException | BusinessRuleException e) {
            System.out.println("Error en la demostración: " + e.getMessage());
        }
    }

    private static Path prepareDemoFolder() throws IOException {
        Path folder = Files.createTempDirectory("maison-glow-demo");
        Files.copy(SOURCE_DATA.resolve("settings.txt"), folder.resolve("settings.txt"));
        Files.copy(SOURCE_DATA.resolve("categories.txt"), folder.resolve("categories.txt"));
        return folder;
    }

    private void run() throws ValidationException, BusinessRuleException, DataAccessException {
        System.out.println("Datos de la demostración en: " + data.toAbsolutePath());

        title("1. Registro de clientes, tratamientos, profesionales y productos");
        Customer ana = customers.register(new ContactData("1065123456", "Ana Pérez", "3001234567",
            "ana@correo.com"));
        Customer luis = customers.register(new ContactData("77111222", "Luis Díaz", "3012223344",
            "luis@correo.com"));
        Treatment haircut = treatments.register(new TreatmentData("Corte de cabello", "Corte y secado", 40000, 45));
        Treatment manicure = treatments.register(new TreatmentData("Manicure", "Limpieza y esmaltado", 25000, 40));
        Professional laura = professionals.register(
            new ContactData("49777888", "Laura Gómez", "3157778888", "laura@correo.com"), "Peluquería",
            EIGHT_TO_SIX);
        professionals.assignTreatment(laura.getId(), haircut.getId());
        professionals.assignTreatment(laura.getId(), manicure.getId());
        Product shampoo = products.register(new ProductData("Shampoo hidratante", 1, 28000, 2), 5);
        products.register(new ProductData("Esmalte rojo", 3, 12000, 3), 15);
        products.register(new ProductData("Laca fijadora", 1, 18000, 2), 8);
        printList("Clientes", customers.listAll());
        printList("Tratamientos", treatments.listActive());
        printList("Profesionales", professionals.listAll());
        printList("Productos", products.listAll());

        title("2. Agendamiento con reglas y control de cruces");
        LocalDateTime start = LocalDate.now(clock).plusDays(1).atTime(10, 0);
        Professional lauraWithTreatments = professionals.findById(laura.getId());
        double deposit = new DepositRule(settings).requiredDeposit(new Appointment(0, ana, lauraWithTreatments,
            haircut, start));
        Appointment booked = appointments.book(ana, lauraWithTreatments, haircut, start, deposit);
        System.out.println("Cita agendada para " + ana.getName() + " el " + booked.getStart()
            + " (anticipo " + money(booked.getDeposit()) + ", recordatorio " + booked.getReminderAt() + ")");
        attempt("Cita cruzada con Laura", () ->
            appointments.book(luis, lauraWithTreatments, manicure, start.plusMinutes(20), 25000));
        attempt("Cita sin el anticipo mínimo", () ->
            appointments.book(luis, lauraWithTreatments, manicure, start.plusHours(2), 0));

        title("3. Venta mixta con descuento de stock y factura simulada");
        Sale sale = new Sale(ana);
        sale.addLine(products.findById(shampoo.getId()), 3);
        sale.addLine(haircut, 1);
        sale.linkAppointment(booked);
        Sale registered = sales.register(sale);
        new SimulatedEmailNotifier(new InvoiceFormatter(settings.businessName()), System.out)
            .sendInvoice(ana, registered);
        attempt("Venta con más unidades que el stock", () -> {
            Sale tooBig = new Sale(luis);
            tooBig.addLine(products.findById(shampoo.getId()), 10);
            sales.register(tooBig);
        });

        title("4. Inventario, estadísticas y promociones");
        LocalDate today = LocalDate.now(clock);
        printList("Productos con stock bajo", products.listLowStock());
        CustomerStatistics anaStatistics = statistics.customerStatistics(ana.getId());
        System.out.println("Estadísticas de " + ana.getName() + ": " + anaStatistics.purchases() + " compra(s) por "
            + money(anaStatistics.totalSpent()) + ", " + anaStatistics.appointments() + " cita(s), "
            + anaStatistics.noShows() + " inasistencia(s)");
        System.out.println("Productos más vendidos hoy: " + statistics.topProducts(3, today, today));
        System.out.println("Tratamientos más solicitados: " + statistics.topTreatments(3));
        System.out.println("Clientes frecuentes: " + statistics.frequentCustomers(3));
        lowRotation.notifyAdministrator(new SimulatedAdminNotifier(System.out), today);
        System.out.println("Promociones sugeridas para " + luis.getName() + ":");
        for (PromotionSuggestion suggestion : promotions.suggestPromotions(luis.getId(), today)) {
            System.out.println("  - " + suggestion.productName() + ": " + money(suggestion.regularPrice()) + " -> "
                + money(suggestion.promotionalPrice()) + String.format(COLOMBIA, " (%.0f %% de descuento)",
                suggestion.discountPercentage()));
        }
    }

    private Path file(String name) {
        return data.resolve(name);
    }

    private static String money(double amount) {
        return String.format(COLOMBIA, "$ %,.0f", amount);
    }

    private static void title(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }

    private static void printList(String label, List<?> items) {
        System.out.println(label + ":");
        items.forEach(item -> System.out.println("  - " + item));
    }

    private static void attempt(String description, DemoStep step) throws DataAccessException {
        try {
            step.run();
            System.out.println(description + ": aceptada (no se esperaba)");
        } catch (ValidationException | BusinessRuleException e) {
            System.out.println(description + ": rechazada -> " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface DemoStep {
        void run() throws ValidationException, BusinessRuleException, DataAccessException;
    }
}
