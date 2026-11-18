package com.maisonglow;

import com.maisonglow.ai.AiConfig;
import com.maisonglow.ai.AiHistory;
import com.maisonglow.ai.AiModule;
import com.maisonglow.ai.AiProviderFactory;
import com.maisonglow.dao.*;
import com.maisonglow.service.AppointmentService;
import com.maisonglow.service.AuthenticationService;
import com.maisonglow.service.auth.PasswordHasher;
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
import com.maisonglow.view.access.AccessGateway;
import com.maisonglow.view.access.AuthenticationAccessGateway;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

/**
 * The composition root for SQLite DAOs, services, appointment rules
 * and the AI module are created (dependency inversion). The console
 * ({@link Main}) and the JavaFX application ({@link MaisonGlowApp}) both take
 * their services from here, so a view never builds or sees a DAO.
 *
 * <p>Missing SQLite tables and defaults are created automatically. Legacy text files
 * are imported once without removing or rewriting them.
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
    private final AiHistory aiHistory;
    private final SqliteDatabase database;
    private final AuthenticationService auth;
    private AccessGateway access;

    /**
     * Wires everything over a data folder with the system clock and the AI
     * provider chosen by {@code config.properties} (simulated when absent).
     *
     * @param data folder containing the SQLite database and optional legacy files
     */
    public AppContext(Path data) {
        this(data, Clock.systemDefaultZone(), AiConfig.load());
    }

    /**
     * Wires everything with an explicit clock and AI configuration, which
     * keeps tests independent from the real time and the real provider.
     *
     * @param data folder containing the SQLite database and optional legacy files
     * @param clock clock used by the appointment rules
     * @param aiConfig AI provider selection
     */
    public AppContext(Path data, Clock clock, AiConfig aiConfig) {
        this.data = Objects.requireNonNull(data, "data");
        this.clock = Objects.requireNonNull(clock, "clock");
        try { database = SqliteBootstrap.open(data); }
        catch (com.maisonglow.exception.DataAccessException e) {
            throw new IllegalStateException("No se pudo preparar SQLite: " + e.getMessage(), e);
        }
        CustomerDao customerDao = new CustomerDaoSqlite(database);
        TreatmentDao treatmentDao = new TreatmentDaoSqlite(database);
        ProfessionalDao professionalDao = new ProfessionalDaoSqlite(database, treatmentDao);
        CategoryDao categoryDao = new CategoryDaoSqlite(database);
        ProductDao productDao = new ProductDaoSqlite(database, categoryDao);
        AppointmentDao appointmentDao = new AppointmentDaoSqlite(database, customerDao, professionalDao, treatmentDao);
        SaleDao saleDao = new SaleDaoSqlite(database, customerDao, appointmentDao, productDao, treatmentDao);

        settings = new SettingsService(new SettingDaoSqlite(database));
        customers = new CustomerService(customerDao);
        auth = new AuthenticationService(new AccountDaoSqlite(database), customerDao, customers,
            new SqliteTransactions(database), new PasswordHasher(), clock);
        access = new AuthenticationAccessGateway(auth, customers);
        treatments = new TreatmentService(treatmentDao);
        professionals = new ProfessionalService(professionalDao, treatmentDao);
        products = new ProductService(productDao, categoryDao);
        appointments = new AppointmentService(appointmentDao, customerDao, List.of(new DepositRule(settings),
            new ConfirmationRule(settings, clock), new ReminderRule(settings)));
        sales = new SaleService(saleDao, productDao, new SqliteTransactions(database));
        statistics = new StatisticsService(saleDao, appointmentDao);
        lowRotation = new LowRotationService(saleDao, productDao, settings);
        promotions = new PromotionService(lowRotation, saleDao, settings);
        ai = new AiProviderFactory(aiConfig).create(customers, products, appointments, professionals, treatments);
        aiHistory = new AiHistory(data, clock);
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
        try {
            SqliteDatabase database = SqliteBootstrap.open(SESSION_DATA);
            database.transaction(c -> {
                try (var statement = c.createStatement(); var rows = statement.executeQuery("SELECT 1 FROM app_metadata WHERE key='demo_seed_v1'")) {
                    if (rows.next()) return null;
                }
                database.runScript("demo-seed.sql");
                try (var statement = c.createStatement()) { statement.executeUpdate("INSERT INTO app_metadata VALUES ('demo_seed_v1','complete')"); }
                return null;
            });
        } catch (com.maisonglow.exception.DataAccessException e) { throw new IOException("No se pudo preparar SQLite: " + e.getMessage(), e); }
        return SESSION_DATA;
    }

    /**
     * @return the accounts and the session as the access screens see them
     */
    public AccessGateway access() { return access; }

    /**
     * Replaces the account service, which the real service (S-R02) and the tests use.
     *
     * @param gateway the service to use from now on
     */
    public void useAccess(AccessGateway gateway) { this.access = Objects.requireNonNull(gateway, "gateway"); }

    /** @return the SQLite database shared by all business services */
    public SqliteDatabase database() { return database; }

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

    /** @return the folder containing the SQLite database and optional legacy files */
    public Path dataFolder() {
        return data;
    }

    /** @return the clock shared by the rules and the views */
    public Clock clock() {
        return clock;
    }

    /** @return account lifecycle and the current desktop session; initially unauthenticated */
    public AuthenticationService auth() { return auth; }

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

    /** @return persistent AI conversations, tool audit and virtual try-on sessions */
    public AiHistory aiHistory() {
        return aiHistory;
    }
}
