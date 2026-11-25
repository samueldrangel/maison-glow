package com.maisonglow;

import com.maisonglow.ai.AiConfig;
import com.maisonglow.ai.AiHistory;
import com.maisonglow.ai.AiModule;
import com.maisonglow.ai.AiProviderFactory;
import com.maisonglow.ai.SessionAi;
import com.maisonglow.ai.ToolRegistry;
import com.maisonglow.ai.CustomerTools;
import com.maisonglow.service.CatalogService;
import com.maisonglow.service.OrderService;
import com.maisonglow.service.CustomerPortalService;
import com.maisonglow.service.ChangeHistoryService;
import com.maisonglow.service.PhotoStore;
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
import com.maisonglow.view.orders.OrdersGateway;
import com.maisonglow.view.orders.OrderServiceGateway;
import com.maisonglow.view.shop.Cart;

import java.io.IOException;
import java.net.URISyntaxException;
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
    private final CatalogService catalog;
    private final OrderService orders;
    private final CustomerPortalService portal;
    private final ChangeHistoryService changes;
    private OrdersGateway ordersGateway;
    private final Cart cart = new Cart();

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

        var transactions=new SqliteTransactions(database);
        var rawSettings = new SettingsService(new SettingDaoSqlite(database));
        var rawCustomers = new CustomerService(customerDao);
        auth = new AuthenticationService(new AccountDaoSqlite(database), customerDao, rawCustomers,
            new SqliteTransactions(database), new PasswordHasher(), clock);
        access=new AuthenticationAccessGateway(auth,rawCustomers);
        var rawTreatments=new TreatmentService(treatmentDao);
        var rawProfessionals=new ProfessionalService(professionalDao,treatmentDao);
        var rawProducts=new ProductService(productDao,categoryDao);
        var rawSales=new SaleService(saleDao,productDao,transactions);
        var rawStatistics=new StatisticsService(saleDao,appointmentDao);
        var selfAppointments=new AppointmentService(appointmentDao,customerDao,List.of(new ConfirmationRule(rawSettings,clock),new ReminderRule(rawSettings)));
        var commerce=new CommerceDao(database);
        var photos=new PhotoStore(data.resolve("media"));
        photos.cleanPending(commerce);
        try {commerce.expire(java.time.LocalDateTime.now(clock));}
        catch(com.maisonglow.exception.DataAccessException e) {throw new IllegalStateException("No se pudieron actualizar los pedidos vencidos.",e);}
        catalog=new CatalogService(auth,rawProducts,commerce,photos,transactions,clock);
        orders=new OrderService(auth,commerce,rawProducts,rawCustomers,rawSales,saleDao,transactions,clock);
        portal=new CustomerPortalService(auth,rawCustomers,rawTreatments,rawProfessionals,selfAppointments,appointmentDao,saleDao,rawStatistics,rawSettings,transactions,commerce,clock);
        ordersGateway = new OrderServiceGateway(orders, rawCustomers, rawSettings);
        changes=new ChangeHistoryService(auth,commerce,appointmentDao,photos,transactions,clock);

        CustomerDao adminCustomers=AdminDaoGuard.protect(CustomerDao.class,customerDao,auth);
        TreatmentDao adminTreatments=AdminDaoGuard.protect(TreatmentDao.class,treatmentDao,auth);
        ProfessionalDao adminProfessionals=AdminDaoGuard.protect(ProfessionalDao.class,professionalDao,auth);
        ProductDao adminProducts=AdminDaoGuard.protect(ProductDao.class,productDao,auth,() -> commerce.expire(java.time.LocalDateTime.now(clock)));
        CategoryDao adminCategories=AdminDaoGuard.protect(CategoryDao.class,categoryDao,auth);
        AppointmentDao adminAppointments=AdminDaoGuard.protect(AppointmentDao.class,appointmentDao,auth);
        SaleDao adminSales=AdminDaoGuard.protect(SaleDao.class,saleDao,auth);
        settings=new SettingsService(AdminDaoGuard.protect(SettingDao.class,new SettingDaoSqlite(database),auth));
        customers=new CustomerService(adminCustomers);
        treatments=new TreatmentService(adminTreatments);
        professionals=new ProfessionalService(adminProfessionals,adminTreatments);
        products=new ProductService(adminProducts,adminCategories);
        appointments=new AppointmentService(adminAppointments,adminCustomers,List.of(new DepositRule(rawSettings),new ConfirmationRule(rawSettings,clock),new ReminderRule(rawSettings)));
        sales=new SaleService(adminSales,adminProducts,transactions);
        statistics=new StatisticsService(adminSales,adminAppointments);
        lowRotation=new LowRotationService(adminSales,adminProducts,settings);
        promotions=new PromotionService(lowRotation,adminSales,settings);
        var aiFactory=new AiProviderFactory(aiConfig);
        ai=new SessionAi(auth,() -> aiFactory.create(customers,products,appointments,professionals,treatments,t -> SessionAi.bind(t,auth)),
            () -> aiFactory.create(rawCustomers,new ToolRegistry(CustomerTools.create(portal,catalog).stream().map(t -> SessionAi.bind(t,auth)).toList())),
            () -> aiConfig.provider() != AiConfig.Provider.API).module();
        // Old unowned text histories remain untouched and are not attributed to newly created account IDs.
        aiHistory = new AiHistory(data.resolve("account-ai"),clock,auth,commerce);
    }

    /**
     * Prepares the folder where the interactive console and the JavaFX
     * application keep their data between runs ({@code data/session}).
     *
     * @return the session folder, created and seeded when it did not exist
     * @throws IOException if the folder or the preloaded files cannot be created
     */
    public static Path sessionFolder() throws IOException {
        return sessionFolder(true);
    }

    /** When preparing is disabled, recovery requires an existing database. */
    static Path sessionFolder(boolean prepare) throws IOException {
        String demo=System.getenv("MAISON_GLOW_DEMO_FOLDER");
        if(demo!=null&&!demo.isBlank()) {
            try{return com.maisonglow.service.LocalSeedService.openDemoFolder(Path.of(demo));}
            catch(com.maisonglow.exception.ValidationException|com.maisonglow.exception.DataAccessException e){throw new IOException("No se pudo abrir la demo local: "+e.getMessage(),e);}
        }
        Path sessionData = defaultSessionFolder();
        if (!prepare) {
            Path database = sessionData.resolve("maison-glow.db");
            if (!Files.isRegularFile(database)) {
                throw new IOException("No se encontró la base local en " + database + ". Ejecuta Main para configurar la instalación.");
            }
            return sessionData;
        }
        Files.createDirectories(sessionData);
        try {
            SqliteDatabase database = SqliteBootstrap.open(sessionData);
            database.transaction(c -> {
                try (var statement = c.createStatement(); var rows = statement.executeQuery("SELECT 1 FROM app_metadata WHERE key='demo_seed_v1'")) {
                    if (rows.next()) return null;
                }
                database.runScript("demo-seed.sql");
                try (var statement = c.createStatement()) { statement.executeUpdate("INSERT INTO app_metadata VALUES ('demo_seed_v1','complete')"); }
                return null;
            });
        } catch (com.maisonglow.exception.DataAccessException e) { throw new IOException("No se pudo preparar SQLite: " + e.getMessage(), e); }
        return sessionData;
    }

    /** IDE and Maven launches from the same checkout share the same database. */
    private static Path defaultSessionFolder() throws IOException {
        var source = AppContext.class.getProtectionDomain().getCodeSource();
        if (source != null && "file".equals(source.getLocation().getProtocol())) {
            try {
                Path location = Path.of(source.getLocation().toURI());
                for (Path folder = location; folder != null; folder = folder.getParent()) {
                    if (Files.isRegularFile(folder.resolve("pom.xml"))
                        && Files.isRegularFile(folder.resolve("src/main/java/com/maisonglow/AppContext.java"))) {
                        return folder.resolve("data/session");
                    }
                }
            } catch (URISyntaxException e) {
                throw new IOException("No se pudo ubicar la carpeta del proyecto.", e);
            }
        }
        return SOURCE_DATA.resolve("session").toAbsolutePath();
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

    /** @return the orders of the shop as the administrator's screen sees them, over {@link #orders()} */
    public OrdersGateway ordersGateway() { return ordersGateway; }

    /**
     * Replaces the orders gateway of the screen, which the tests use to answer with their own orders.
     *
     * @param gateway the gateway to use from now on
     */
    public void useOrdersGateway(OrdersGateway gateway) { this.ordersGateway = Objects.requireNonNull(gateway, "gateway"); }

    /** @return the cart of the customer who is using the application; the window empties it when the session closes */
    public Cart cart() { return cart; }

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
    public CatalogService catalog() {return catalog;}
    public OrderService orders() {return orders;}
    public CustomerPortalService portal() {return portal;}
    public ChangeHistoryService changes() {return changes;}

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
