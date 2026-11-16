package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Product;
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
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Statistics view tests")
class StatisticsViewTest {

    private static boolean toolkit;

    @TempDir
    Path data;

    private AppContext context;
    private Customer ana;
    private Customer laura;
    private Product shampoo;
    private Product mask;
    private Treatment manicure;
    private Treatment cut;
    private Professional camila;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @BeforeEach
    void prepareData() throws Exception {
        AppContext.seed(data);
        context = new AppContext(data);
        ana = context.customers().register(new ContactData("1001", "Ana García", "3001112233", "ana@correo.com"));
        laura = context.customers().register(new ContactData("1002", "Laura Méndez", "3124445566", "laura@correo.com"));
        shampoo = context.products().register(new ProductData("Champú Nutritivo", 1, 28000, 5), 50);
        mask = context.products().register(new ProductData("Mascarilla Reparadora", 1, 35000, 5), 50);
        manicure = context.treatments().register(new TreatmentData("Manicura gel", "", 45000, 50));
        cut = context.treatments().register(new TreatmentData("Corte y peinado", "", 55000, 60));
        camila = context.professionals().register(new ContactData("2001", "Camila Ruiz", "3001112233", "c@correo.com"),
            "Estilista", new WorkSchedule(LocalTime.of(8, 0), LocalTime.of(17, 0)));
        context.professionals().assignTreatment(camila.getId(), cut.getId());
        context.professionals().assignTreatment(camila.getId(), manicure.getId());
        camila = context.professionals().findById(camila.getId());
    }

    private void sell(Customer customer, Product product, int units) throws Exception {
        Sale sale = new Sale(customer);
        SaleService.addLine(sale, product, units);
        context.sales().register(sale);
    }

    /** Some sales, all of today: the shampoo sells most. */
    private void sellSomething() throws Exception {
        sell(ana, shampoo, 3);
        sell(ana, mask, 1);
        sell(laura, shampoo, 2);
    }

    /** Books treatments in the days that follow, which only a range that reaches ahead includes. */
    private void bookAhead() throws Exception {
        LocalDate day = LocalDate.now().plusDays(1);
        context.appointments().book(ana, camila, manicure, day.atTime(9, 0), 13500);
        context.appointments().book(laura, camila, manicure, day.atTime(11, 0), 13500);
        context.appointments().book(ana, camila, cut, day.atTime(14, 0), 16500);
    }

    private Parent open() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        context = new AppContext(data);
        return FxTest.onFxThread(() -> {
            Parent root = ViewLoader.load(StatisticsViewTest.class.getResource("/com/maisonglow/view/StatisticsView.fxml"), context);
            new Scene(root, 1200, 800);
            root.applyCss();
            root.layout();
            return root;
        });
    }

    private static void setRange(Parent root, LocalDate from, LocalDate to) throws Exception {
        FxTest.onFxThread(() -> {
            ((DatePicker) root.lookup("#fromPicker")).setValue(from);
            ((DatePicker) root.lookup("#toPicker")).setValue(to);
            root.applyCss();
            root.layout();
            return null;
        });
    }

    /** What a chart draws, as the user reads it: from the top bar down. */
    @SuppressWarnings("unchecked")
    private static Map<String, Number> bars(Parent root, String chartId) throws Exception {
        BarChart<Number, String> chart = (BarChart<Number, String>) root.lookup("#" + chartId);
        return FxTest.onFxThread(() -> {
            Map<String, Number> drawn = new java.util.LinkedHashMap<>();
            List<XYChart.Data<Number, String>> points = chart.getData().isEmpty() ? List.of() : List.copyOf(chart.getData().get(0).getData());
            for (int index = points.size() - 1; index >= 0; index--) {
                drawn.put(points.get(index).getYValue(), points.get(index).getXValue());
            }
            return drawn;
        });
    }

    private static boolean visible(Parent root, String id) throws Exception {
        return FxTest.onFxThread(() -> root.lookup("#" + id).isVisible());
    }

    @Test
    @DisplayName("opens on the last 30 days and draws the products that sold most, the first on top")
    void opensOnTheLastThirtyDays() throws Exception {
        sellSomething();
        Parent root = open();

        assertThat(FxTest.onFxThread(() -> ((DatePicker) root.lookup("#toPicker")).getValue())).isEqualTo(LocalDate.now());
        assertThat(FxTest.onFxThread(() -> ((DatePicker) root.lookup("#fromPicker")).getValue())).isEqualTo(LocalDate.now().minusDays(29));
        assertThat(FxTest.onFxThread(() -> ((ToggleButton) root.lookup("#last30Chip")).isSelected())).isTrue();
        assertThat(bars(root, "productsChart")).containsExactly(
            Map.entry("Champú Nutritivo", 5), Map.entry("Mascarilla Reparadora", 1));
        assertThat(visible(root, "productsChart")).isTrue();
        assertThat(visible(root, "productsEmpty")).isFalse();
    }

    @Test
    @DisplayName("without data, each chart is replaced by a message instead of being left blank")
    void emptyChartsShowAMessage() throws Exception {
        Parent root = open();

        assertThat(visible(root, "productsChart")).isFalse();
        assertThat(visible(root, "productsEmpty")).isTrue();
        assertThat(visible(root, "treatmentsChart")).isFalse();
        assertThat(visible(root, "treatmentsEmpty")).isTrue();
        assertThat(FxTest.onFxThread(() -> ((Label) root.lookup("#productsEmpty")).getText())).contains("Sin ventas");
    }

    @Test
    @DisplayName("changing the dates updates the charts: a range that reaches ahead brings in the booked treatments")
    void changingTheDatesUpdatesTheCharts() throws Exception {
        sellSomething();
        bookAhead();
        Parent root = open();
        assertThat(visible(root, "treatmentsChart")).isFalse();

        setRange(root, LocalDate.now(), LocalDate.now().plusDays(7));

        assertThat(bars(root, "treatmentsChart")).containsExactly(Map.entry("Manicura gel", 2), Map.entry("Corte y peinado", 1));
        assertThat(visible(root, "treatmentsEmpty")).isFalse();
        assertThat(FxTest.onFxThread(() -> ((ToggleButton) root.lookup("#last30Chip")).isSelected())).isFalse();

        setRange(root, LocalDate.now().minusDays(100), LocalDate.now().minusDays(50));

        assertThat(visible(root, "productsChart")).isFalse();
        assertThat(visible(root, "productsEmpty")).isTrue();
        assertThat(visible(root, "treatmentsEmpty")).isTrue();
    }

    @Test
    @DisplayName("the shortcuts set the dates and the charts follow")
    void shortcutsSetTheDates() throws Exception {
        sellSomething();
        Parent root = open();

        FxTest.onFxThread(() -> {
            ((ToggleButton) root.lookup("#last7Chip")).fire();
            return null;
        });
        assertThat(FxTest.onFxThread(() -> ((DatePicker) root.lookup("#fromPicker")).getValue())).isEqualTo(LocalDate.now().minusDays(6));
        assertThat(bars(root, "productsChart")).containsKey("Champú Nutritivo");

        FxTest.onFxThread(() -> {
            ((ToggleButton) root.lookup("#last90Chip")).fire();
            return null;
        });
        assertThat(FxTest.onFxThread(() -> ((DatePicker) root.lookup("#fromPicker")).getValue())).isEqualTo(LocalDate.now().minusDays(89));
    }

    @Test
    @DisplayName("a range that ends before it starts is rejected under the dates and the charts keep what they showed")
    void rejectsAnInvertedRange() throws Exception {
        sellSomething();
        Parent root = open();

        setRange(root, LocalDate.now(), LocalDate.now().minusDays(5));

        Label message = (Label) root.lookup("#rangeMessage");
        assertThat(FxTest.onFxThread(() -> message.isVisible() ? message.getText() : "")).contains("posterior");
        assertThat(bars(root, "productsChart")).containsKey("Champú Nutritivo");
    }

    @Test
    @DisplayName("the frequent customers are ranked by purchases, of the whole history")
    void frequentCustomers() throws Exception {
        sellSomething();
        Parent root = open();

        @SuppressWarnings("unchecked")
        TableView<Map.Entry<String, Integer>> table = (TableView<Map.Entry<String, Integer>>) root.lookup("#customersTable");
        assertThat(FxTest.onFxThread(() -> table.getItems().stream().map(e -> e.getKey() + " " + e.getValue()).toList()))
            .containsExactly("Ana García 2", "Laura Méndez 1");
        assertThat(FxTest.onFxThread(() -> table.getItems().size())).isEqualTo(2);
    }

    @Test
    @DisplayName("past appointments show in the default range, drawn from the data of a clock set in the past")
    void pastTreatmentsShowInTheDefaultRange() throws Exception {
        LocalDate then = LocalDate.now().minusDays(5);
        AppContext past = new AppContext(data, Clock.fixed(then.minusDays(3).atTime(8, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()), AiConfig.simulated());
        Professional pro = past.professionals().findById(camila.getId());
        past.appointments().book(ana, pro, past.treatments().findById(manicure.getId()), then.atTime(10, 0), 13500);
        Parent root = open();

        assertThat(bars(root, "treatmentsChart")).containsExactly(Map.entry("Manicura gel", 1));
    }
}
