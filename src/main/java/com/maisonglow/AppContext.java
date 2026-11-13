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

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

/**
 * The only place where the text-file DAOs, the services and the appointment
 * rules are created (dependency inversion). The console ({@link Main}) and the
 * JavaFX application both take their services from here, so a view never
 * builds or sees a DAO.
 *
 * <p>The data folder must already hold the preloaded {@code settings.txt} and
 * {@code categories.txt}.
 */
public final class AppContext {

    private final Path data;
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

    /**
     * Wires everything over a data folder with the system clock.
     *
     * @param data folder that holds the text files
     */
    public AppContext(Path data) {
        this(data, Clock.systemDefaultZone());
    }

    /**
     * Wires the DAOs, the appointment rules and the services with an explicit
     * clock, which keeps tests independent from the real time.
     *
     * @param data folder that holds the text files
     * @param clock clock used by the appointment rules
     */
    public AppContext(Path data, Clock clock) {
        this.data = Objects.requireNonNull(data, "data");
        Objects.requireNonNull(clock, "clock");
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

    private Path file(String name) {
        return data.resolve(name);
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
}
