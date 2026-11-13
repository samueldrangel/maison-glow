package com.maisonglow;

import com.maisonglow.dao.CategoryDao;
import com.maisonglow.dao.CategoryDaoText;
import com.maisonglow.dao.CustomerDao;
import com.maisonglow.dao.CustomerDaoText;
import com.maisonglow.dao.ProductDao;
import com.maisonglow.dao.ProductDaoText;
import com.maisonglow.dao.ProfessionalDao;
import com.maisonglow.dao.ProfessionalDaoText;
import com.maisonglow.dao.SettingDaoText;
import com.maisonglow.dao.TreatmentDao;
import com.maisonglow.dao.TreatmentDaoText;
import com.maisonglow.service.CustomerService;
import com.maisonglow.service.ProductService;
import com.maisonglow.service.ProfessionalService;
import com.maisonglow.service.SettingsService;
import com.maisonglow.service.TreatmentService;

import java.nio.file.Path;
import java.util.Objects;

/**
 * The only place where the text-file DAOs and the services are created
 * (dependency inversion). The console ({@link Main}) and the JavaFX
 * application both take their services from here, so a view never builds or
 * sees a DAO.
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
    private final SettingsService settings;

    /**
     * Wires the DAOs and the catalog services over a data folder.
     *
     * @param data folder that holds the text files
     */
    public AppContext(Path data) {
        this.data = Objects.requireNonNull(data, "data");
        CustomerDao customerDao = new CustomerDaoText(file("customers.txt"));
        TreatmentDao treatmentDao = new TreatmentDaoText(file("treatments.txt"));
        ProfessionalDao professionalDao = new ProfessionalDaoText(file("professionals.txt"),
            file("professional_treatments.txt"), treatmentDao);
        CategoryDao categoryDao = new CategoryDaoText(file("categories.txt"));
        ProductDao productDao = new ProductDaoText(file("products.txt"), categoryDao);

        settings = new SettingsService(new SettingDaoText(file("settings.txt")));
        customers = new CustomerService(customerDao);
        treatments = new TreatmentService(treatmentDao);
        professionals = new ProfessionalService(professionalDao, treatmentDao);
        products = new ProductService(productDao, categoryDao);
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

    /** @return the business settings service */
    public SettingsService settings() {
        return settings;
    }
}
