package com.maisonglow.integration;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import com.maisonglow.dao.*;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.*;
import com.maisonglow.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import static org.assertj.core.api.Assertions.*;

class SqliteIntegrationTest {
    @TempDir Path folder;
    private AppContext open() { return new AppContext(folder,Clock.systemDefaultZone(),AiConfig.simulated()); }
    private Customer customer(AppContext app) throws Exception {
        return app.customers().register(new ContactData("1001","Ana García","3001112233","ana@example.com"));
    }
    private long count(AppContext app,String table) throws Exception {
        return app.database().transaction(c -> {
            try(var s=c.createStatement();var row=s.executeQuery("SELECT COUNT(*) FROM "+table)) { row.next();return row.getLong(1); }
        });
    }
    @Test void createsDatabaseAndReadsNewCustomersAcrossContexts() throws Exception {
        AppContext first=open(),second=open();
        Customer ana=customer(first);
        assertThat(folder.resolve("maison-glow.db")).exists();
        assertThat(count(second,"customer")).isEqualTo(1);
        assertThat(second.customers().findById(ana.getId()).getName()).isEqualTo("Ana García");
        first.customers().update(ana.getId(),new ContactData("1001","Ana Gómez","3001112233","ana@example.com"));
        assertThat(second.customers().findById(ana.getId()).getName()).isEqualTo("Ana Gómez");
    }
    @Test void migratesLegacyFilesOnceAndPreservesIdsAndSettings() throws Exception {
        Files.writeString(folder.resolve("customers.txt"),"#nextId=20\n7|1001|Ana García|3001112233|ana@example.com|2|2026-01-02\n");
        Files.writeString(folder.resolve("settings.txt"),"deposit_percentage|45|Personalizado\n");
        AppContext app=open();
        assertThat(app.customers().findById(7).getNoShowCount()).isEqualTo(2);
        assertThat(new SettingDaoSqlite(app.database()).loadAll().get("deposit_percentage").getValue()).isEqualTo("45");
        Customer added=app.customers().register(new ContactData("1002","Laura","3002223344","laura@example.com"));
        assertThat(added.getId()).isEqualTo(20);
        assertThat(open().customers().listAll()).hasSize(2);
        assertThat(Files.readString(folder.resolve("customers.txt"))).contains("#nextId=20");
    }
    @Test void failedImportRollsBackAndCanBeRetried() throws Exception {
        Files.writeString(folder.resolve("customers.txt"),"1|1001|Ana|3001112233|ana@example.com|0|2026-01-02\n");
        Files.writeString(folder.resolve("products.txt"),"1|Producto|999|10|2|1|1\n");
        assertThatThrownBy(this::open).isInstanceOf(IllegalStateException.class);
        SqliteDatabase database=new SqliteDatabase(folder.resolve("maison-glow.db"));
        int customers = database.transaction(c -> { try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM customer")) {r.next();return r.getInt(1);} });
        assertThat(customers).isZero();
        Files.writeString(folder.resolve("products.txt"),"");
        assertThat(open().customers().listAll()).hasSize(1);
    }
    @Test void demoSeedIsRepeatableAndDoesNotResetModifiedData() throws Exception {
        AppContext app=open();
        app.database().runScript("demo-seed.sql");
        app.settings().update("business_name","Mi salón");
        app.database().runScript("demo-seed.sql");
        assertThat(count(app,"customer")).isEqualTo(3);
        assertThat(count(app,"product")).isEqualTo(3);
        assertThat(count(app,"professional_treatment")).isEqualTo(2);
        assertThat(new SettingDaoSqlite(app.database()).loadAll().get("business_name").getValue()).isEqualTo("Mi salón");
    }
    @Test void appointmentAndMixedSaleSurviveRestartWithHistoricalPricesAndStock() throws Exception {
        AppContext app=open(); Customer ana=customer(app);
        Treatment cut=app.treatments().register(new TreatmentData("Corte","",55000,45));
        Professional pro=app.professionals().register(new ContactData("2001","Camila","3002223344","camila@example.com"),"Cabello",new WorkSchedule(LocalTime.of(8,0),LocalTime.of(18,0)));
        app.professionals().assignTreatment(pro.getId(),cut.getId());
        var appointment=app.appointments().book(ana,app.professionals().findById(pro.getId()),cut,LocalDate.now().plusDays(1).atTime(10,0),16500);
        Product product=app.products().register(new ProductData("Champú",1,28000,2),5);
        Sale sale=new Sale(ana); SaleService.addLine(sale,product,2); SaleService.addLine(sale,cut,1); app.sales().register(sale);
        AppContext reopened=open();
        assertThat(reopened.appointments().listByDate(appointment.getStart().toLocalDate())).hasSize(1);
        assertThat(reopened.professionals().findById(pro.getId()).getTreatments()).hasSize(1);
        assertThat(reopened.products().findById(product.getId()).getStock()).isEqualTo(3);
        Sale restored=reopened.sales().listByCustomer(ana.getId()).getFirst();
        assertThat(restored.getTotal()).isEqualByComparingTo("111000");
        assertThat(restored.getLines()).hasSize(2);
        assertThatThrownBy(() -> reopened.customers().delete(ana.getId())).isInstanceOf(DataAccessException.class);
    }
    @Test void rejectedSaleRollsBackStockSaleAndLines() throws Exception {
        AppContext app=open(); Customer ana=customer(app);
        Product product=app.products().register(new ProductData("Champú",1,28000,2),5);
        Customer missing=new Customer(999,"9999","Inexistente","3002223344","missing@example.com");
        Sale sale=new Sale(missing); SaleService.addLine(sale,product,2);
        assertThatThrownBy(() -> app.sales().register(sale)).isInstanceOf(DataAccessException.class);
        assertThat(open().products().findById(product.getId()).getStock()).isEqualTo(5);
        assertThat(count(app,"sale")).isZero(); assertThat(count(app,"sale_line")).isZero(); assertThat(sale.getId()).isZero();
    }

    @Test void exactMoneySurvivesImportSaleAndRestart() throws Exception {
        Files.writeString(folder.resolve("treatments.txt"),"1|Coloración||123456789012345.67|90|1\n");
        AppContext app=open(); Customer ana=customer(app);
        Treatment treatment=app.treatments().findById(1);
        assertThat(treatment.getPrice()).isEqualByComparingTo("123456789012345.67");
        Sale sale=new Sale(ana); SaleService.addLine(sale,treatment,2);app.sales().register(sale);
        Sale loaded=open().sales().listByCustomer(ana.getId()).getFirst();
        assertThat(loaded.getTotal()).isEqualByComparingTo("246913578024691.34");
        assertThat(loaded.getLines().getFirst().getUnitPrice()).isEqualByComparingTo("123456789012345.67");
    }

    @Test void commitFailureRestoresCallerStateAndDatabase() throws Exception {
        AppContext app=open();Customer ana=customer(app);
        Product product=app.products().register(new ProductData("Champú",1,28000,2),5);
        var database=app.database();
        TransactionRunner failAtCommit = new TransactionRunner() {
            public <T> T execute(Action<T> action) throws com.maisonglow.exception.ValidationException,com.maisonglow.exception.BusinessRuleException,DataAccessException {
                return database.transaction(c -> {
                    T value;
                    try { value=action.run(); }
                    catch(com.maisonglow.exception.ValidationException | com.maisonglow.exception.BusinessRuleException e) { throw new DataAccessException("Rechazada",e); }
                    try(var statement=c.createStatement()) {
                        statement.execute("PRAGMA defer_foreign_keys=ON");
                        statement.executeUpdate("INSERT INTO professional_treatment VALUES (99999,99999)");
                    }
                    return value;
                });
            }
        };
        var customerDao=new CustomerDaoSqlite(database);var treatmentDao=new TreatmentDaoSqlite(database);
        var products=new ProductDaoSqlite(database,new CategoryDaoSqlite(database));
        var appointments=new AppointmentDaoSqlite(database,customerDao,new ProfessionalDaoSqlite(database,treatmentDao),treatmentDao);
        var sales=new SaleService(new SaleDaoSqlite(database,customerDao,appointments,products,treatmentDao),products,failAtCommit);
        Sale sale=new Sale(ana);SaleService.addLine(sale,product,2);
        assertThatThrownBy(() -> sales.register(sale)).isInstanceOf(DataAccessException.class);
        assertThat(sale.getId()).isZero();assertThat(product.getStock()).isEqualTo(5);
        assertThat(open().products().findById(product.getId()).getStock()).isEqualTo(5);
        assertThat(count(app,"sale")).isZero();assertThat(count(app,"sale_line")).isZero();
    }
}
