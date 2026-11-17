package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Sale;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.ProductData;
import com.maisonglow.service.SaleService;
import com.maisonglow.service.TreatmentData;
import com.maisonglow.service.WorkSchedule;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Home view tests")
class HomeViewTest {

    private static boolean toolkit;

    @TempDir
    Path data;

    private AppContext context;
    private Customer ana;
    private Professional camila;
    private Treatment cut;
    private LocalDate today;
    private Clock morning;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @BeforeEach
    void prepareData() throws Exception {
        AppContext.seed(data);
        today = LocalDate.now();
        morning = Clock.fixed(today.atTime(8, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        context = new AppContext(data, morning, AiConfig.simulated());
        ana = context.customers().register(new ContactData("1001", "Ana García", "3001112233", "ana@correo.com"));
        cut = context.treatments().register(new TreatmentData("Corte y peinado", "", 55000, 60));
        camila = context.professionals().register(new ContactData("2001", "Camila Ruiz", "3001112233", "c@correo.com"),
            "Estilista", new WorkSchedule(LocalTime.of(8, 0), LocalTime.of(17, 0)));
        context.professionals().assignTreatment(camila.getId(), cut.getId());
        camila = context.professionals().findById(camila.getId());
    }

    private Parent open() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        context = new AppContext(data, morning, AiConfig.simulated());
        return FxTest.onFxThread(() -> {
            Parent root = ViewLoader.load(HomeViewTest.class.getResource("/com/maisonglow/view/HomeView.fxml"), context);
            new Scene(root, 1200, 760);
            root.applyCss();
            root.layout();
            return root;
        });
    }

    private static String text(Parent root, String id) throws Exception {
        return FxTest.onFxThread(() -> ((Label) root.lookup("#" + id)).getText());
    }

    private static boolean visible(Parent root, String id) throws Exception {
        return FxTest.onFxThread(() -> root.lookup("#" + id).isVisible());
    }

    private static List<String> rows(Parent root, String id) throws Exception {
        return FxTest.onFxThread(() -> ((Pane) root.lookup("#" + id)).getChildren().stream()
            .map(row -> row instanceof Label label ? label.getText() : String.join(" ", row.lookupAll(".label").stream()
                .map(node -> ((Label) node).getText()).toList())).toList());
    }

    @Test
    @DisplayName("greets by the hour and writes today's date")
    void greetsAndDates() throws Exception {
        Parent root = open();

        assertThat(text(root, "greetingLabel")).isEqualTo("Buenos días");
        assertThat(text(root, "dateLabel")).isNotBlank().matches("^\\p{Lu}.*");
        assertThat(HomeController.greeting(15)).isEqualTo("Buenas tardes");
        assertThat(HomeController.greeting(21)).isEqualTo("Buenas noches");
    }

    @Test
    @DisplayName("with no data every indicator is at zero and the lists say there is nothing")
    void emptyDay() throws Exception {
        Parent root = open();

        assertThat(text(root, "todayValue")).isEqualTo("0");
        assertThat(text(root, "pendingValue")).isEqualTo("0");
        assertThat(text(root, "salesNote")).isEqualTo("0 ventas");
        assertThat(text(root, "lowStockValue")).isEqualTo("0");
        assertThat(visible(root, "agendaEmpty")).isTrue();
        assertThat(visible(root, "restockEmpty")).isTrue();
    }

    @Test
    @DisplayName("today's appointments are counted and listed with the customer, the treatment and the state")
    void showsTodaysAppointments() throws Exception {
        context.appointments().book(ana, camila, cut, today.atTime(10, 0), 16500);
        Parent root = open();

        assertThat(text(root, "todayValue")).isEqualTo("1");
        assertThat(text(root, "todayNote")).isEqualTo("1 por atender");
        assertThat(text(root, "pendingValue")).isEqualTo("1");
        assertThat(visible(root, "agendaEmpty")).isFalse();
        assertThat(rows(root, "agendaBox")).singleElement().asString()
            .contains("10:00").contains("Ana García").contains("Corte y peinado").contains("Camila Ruiz").contains("Pendiente");
    }

    @Test
    @DisplayName("the header says what today holds and what waits for an answer")
    void summaryOfTheDay() throws Exception {
        assertThat(HomeController.summary(0, 0)).isEqualTo("Hoy no tienes citas agendadas.");
        assertThat(HomeController.summary(1, 0)).isEqualTo("Hoy tienes 1 cita, todas confirmadas o atendidas.");
        assertThat(HomeController.summary(4, 2)).isEqualTo("Hoy tienes 4 citas y 2 esperan tu confirmación.");
        context.appointments().book(ana, camila, cut, today.atTime(10, 0), 16500);
        assertThat(text(open(), "summaryLabel")).isEqualTo("Hoy tienes 1 cita y 1 espera tu confirmación.");
    }

    @Test
    @DisplayName("the stock tile is marked when there is something to restock")
    void marksTheStockTile() throws Exception {
        Parent calm = open();
        assertThat(FxTest.onFxThread(() -> calm.lookup("#stockTile").getStyleClass())).doesNotContain("needs-attention");
        context.products().register(new ProductData("Mascarilla Reparadora", 1, 35000, 5), 1);
        Parent attention = open();
        assertThat(FxTest.onFxThread(() -> attention.lookup("#stockTile").getStyleClass())).contains("needs-attention");
    }

    @Test
    @DisplayName("an appointment of another day is not counted")
    void ignoresOtherDays() throws Exception {
        context.appointments().book(ana, camila, cut, today.plusDays(1).atTime(10, 0), 16500);
        Parent root = open();

        assertThat(text(root, "todayValue")).isEqualTo("0");
    }

    @Test
    @DisplayName("a product below its minimum appears to restock, and the count follows")
    void showsLowStock() throws Exception {
        context.products().register(new ProductData("Mascarilla Reparadora", 1, 35000, 5), 1);
        Parent root = open();

        assertThat(text(root, "lowStockValue")).isEqualTo("1");
        assertThat(visible(root, "restockEmpty")).isFalse();
        assertThat(rows(root, "restockBox")).singleElement().asString().contains("Mascarilla Reparadora").contains("Mínimo 5").contains("Reponer");
    }

    @Test
    @DisplayName("today's sales are added up")
    void addsTodaysSales() throws Exception {
        Sale sale = new Sale(ana);
        SaleService.addLine(sale, cut, 2);
        context.sales().register(sale);
        Parent root = open();

        assertThat(text(root, "salesNote")).isEqualTo("1 venta");
        assertThat(text(root, "salesValue")).contains("110.000");
    }
}
