package com.maisonglow;

import com.maisonglow.ai.AiConfig;
import com.maisonglow.ai.AiModule;
import com.maisonglow.ai.AiProviderFactory;
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
import com.maisonglow.service.AppointmentService;
import com.maisonglow.service.CustomerService;
import com.maisonglow.service.LowRotationService;
import com.maisonglow.service.ProductService;
import com.maisonglow.service.ProfessionalService;
import com.maisonglow.service.PromotionService;
import com.maisonglow.service.SaleService;
import com.maisonglow.service.SettingsService;
import com.maisonglow.service.StatisticsService;
import com.maisonglow.service.TreatmentService;
import com.maisonglow.service.rule.ConfirmationRule;
import com.maisonglow.service.rule.DepositRule;
import com.maisonglow.service.rule.ReminderRule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

/**
 * The only place where the text-file DAOs, the services, the appointment rules
 * and the AI module are created (dependency inversion). The console
 * ({@link Main}) and the JavaFX application ({@link MaisonGlowApp}) both take
 * their services from here, so a view never builds or sees a DAO.
 *
 * <p>The data folder must already hold the preloaded {@code settings.txt} and
 * {@code categories.txt}; {@link #seed(Path)} copies them.
 */
public final class AppContext {

    private static final Path SOURCE_DATA = Path.of("data");
    private static final Path SESSION_DATA = SOURCE_DATA.resolve("session");
    private static final String SETTINGS_FILE = "settings.txt";
    private static final String CATEGORIES_FILE = "categories.txt";

    private final Path data;
    private final Clock clock;
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
    private final AiModule ai;

    /**
     * Wires everything over a data folder with the system clock and the AI
     * provider chosen by {@code config.properties} (simulated when absent).
     *
     * @param data folder that holds the text files
     */
    public AppContext(Path data) {
        this(data, Clock.systemDefaultZone(), AiConfig.load());
    }

    /**
     * Wires everything with an explicit clock and AI configuration, which
     * keeps tests independent from the real time and the real provider.
     *
     * @param data folder that holds the text files
     * @param clock clock used by the appointment rules
     * @param aiConfig AI provider selection
     */
    public AppContext(Path data, Clock clock, AiConfig aiConfig) {
        this.data = Objects.requireNonNull(data, "data");
        this.clock = Objects.requireNonNull(clock, "clock");
        CustomerDao customerDao = new CustomerDaoText(file("customers.txt"));
        TreatmentDao treatmentDao = new TreatmentDaoText(file("treatments.txt"));
        ProfessionalDao professionalDao = new ProfessionalDaoText(file("professionals.txt"),
            file("professional_treatments.txt"), treatmentDao);
        CategoryDao categoryDao = new CategoryDaoText(file(CATEGORIES_FILE));
        ProductDao productDao = new ProductDaoText(file("products.txt"), categoryDao);
        AppointmentDao appointmentDao = new AppointmentDaoText(file("appointments.txt"), customerDao,
            professionalDao, treatmentDao);
        SaleDao saleDao = new SaleDaoText(file("sales.txt"), file("sale_lines.txt"), customerDao, appointmentDao,
            productDao, treatmentDao);

        settings = new SettingsService(new SettingDaoText(file(SETTINGS_FILE)));
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
        ai = new AiProviderFactory(aiConfig).create(customers, products, appointments, professionals, treatments);
    }

    /**
     * Prepares the folder where the interactive console and the JavaFX
     * application keep their data between runs ({@code data/session}).
     *
     * @return the session folder, created and seeded when it did not exist
     * @throws IOException if the folder or the preloaded files cannot be created
     */
    public static Path sessionFolder() throws IOException {
        Files.createDirectories(SESSION_DATA);
        if (Files.notExists(SESSION_DATA.resolve(SETTINGS_FILE))) {
            seed(SESSION_DATA);
        }
        return SESSION_DATA;
    }

    /**
     * Copies the preloaded settings and categories into a data folder.
     *
     * @param folder an existing folder that does not hold them yet
     * @throws IOException if a file cannot be copied
     */
    public static void seed(Path folder) throws IOException {
        Files.copy(SOURCE_DATA.resolve(SETTINGS_FILE), folder.resolve(SETTINGS_FILE));
        Files.copy(SOURCE_DATA.resolve(CATEGORIES_FILE), folder.resolve(CATEGORIES_FILE));
    }

    private Path file(String name) {
        return data.resolve(name);
    }

    /** @return the folder that holds the text files */
    public Path dataFolder() {
        return data;
    }

    /** @return the clock shared by the rules and the views */
    public Clock clock() {
        return clock;
    }

    /** @return the customer service */
    public CustomerService customers() {
        return customers;
    }

    /** @return the professional service */
    public ProfessionalService professionals() {
        return professionals;
    }

    /** @return the treatment catalog service */
    public TreatmentService treatments() {
        return treatments;
    }

    /** @return the product and inventory service */
    public ProductService products() {
        return products;
    }

    /** @return the appointment service */
    public AppointmentService appointments() {
        return appointments;
    }

    /** @return the sales service */
    public SaleService sales() {
        return sales;
    }

    /** @return the business settings service */
    public SettingsService settings() {
        return settings;
    }

    /** @return the statistics service */
    public StatisticsService statistics() {
        return statistics;
    }

    /** @return the low-rotation detection service */
    public LowRotationService lowRotation() {
        return lowRotation;
    }

    /** @return the promotion suggestion service */
    public PromotionService promotions() {
        return promotions;
    }

    /** @return the AI module (assistant and virtual try-on), simulated unless an API is configured */
    public AiModule ai() {
        return ai;
    }
}
