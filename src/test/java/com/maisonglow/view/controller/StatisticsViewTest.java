package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import com.maisonglow.model.AppointmentStatus;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Product;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Sale;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.ContactData;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.service.ProductSales;
import com.maisonglow.service.PromotionSuggestion;
import com.maisonglow.service.SimulatedAdminNotifier;
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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.math.BigDecimal;
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
        return open(new SimulatedAdminNotifier(new PrintStream(OutputStream.nullOutputStream())));
    }

    /** Opens the screen; the notice to the administrator goes to the notifier given, so a test can read it. */
    private Parent open(SimulatedAdminNotifier notifier) throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        context = new AppContext(data);
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<StatisticsController> loaded = ViewLoader.loadWithController(
                StatisticsViewTest.class.getResource("/com/maisonglow/view/StatisticsView.fxml"), context);
            loaded.controller().useNotifier(notifier);
            Parent root = loaded.root();
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

    /** A third product that nobody has bought: the one of lowest rotation. */
    private Product unsoldProduct() throws Exception {
        return context.products().register(new ProductData("Esmalte Rosa Nude", 1, 18000, 5), 20);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> rows(Parent root, String tableId) throws Exception {
        TableView<T> table = (TableView<T>) root.lookup("#" + tableId);
        return FxTest.onFxThread(() -> List.copyOf(table.getItems()));
    }

    @SuppressWarnings("unchecked")
    private static void chooseCustomer(Parent root, String document) throws Exception {
        ComboBox<Customer> box = (ComboBox<Customer>) root.lookup("#promotionsCustomerBox");
        FxTest.onFxThread(() -> {
            box.getSelectionModel().select(box.getItems().stream().filter(c -> c.getDocument().equals(document)).findFirst().orElseThrow());
            return null;
        });
    }

    @Test
    @DisplayName("low rotation lists the products that sold least first, with their stock and units sold")
    void lowRotationListsTheLeastSoldFirst() throws Exception {
        sellSomething();
        unsoldProduct();
        Parent root = open();

        List<ProductSales> low = rows(root, "lowRotationTable");

        assertThat(low).extracting(sales -> sales.product().getName() + " " + sales.unitsSold())
            .containsExactly("Esmalte Rosa Nude 0", "Mascarilla Reparadora 1", "Champú Nutritivo 5");
        assertThat(FxTest.onFxThread(() -> ((Label) root.lookup("#lowRotationHint")).getText())).contains("30 días");
    }

    @Test
    @DisplayName("Avisar al administrador sends the notice with the products of low rotation")
    void notifiesTheAdministrator() throws Exception {
        sellSomething();
        unsoldProduct();
        SimulatedAdminNotifier notifier = new SimulatedAdminNotifier(new PrintStream(OutputStream.nullOutputStream()));
        Parent root = open(notifier);

        FxTest.onFxThread(() -> {
            ((Button) root.lookup("#notifyButton")).fire();
            return null;
        });

        assertThat(notifier.getNotifications()).singleElement().asString()
            .contains("Esmalte Rosa Nude", "Mascarilla Reparadora", "30 días");
    }

    @Test
    @DisplayName("with no product of low rotation the notice button is off and the table says so")
    void noLowRotationNoNotice() throws Exception {
        context.products().deactivate(shampoo.getId());
        context.products().deactivate(mask.getId());
        Parent root = open();

        assertThat(rows(root, "lowRotationTable")).isEmpty();
        assertThat(FxTest.onFxThread(() -> root.lookup("#notifyButton").isDisabled())).isTrue();
    }

    @Test
    @DisplayName("promotions suggest the products of low rotation that the customer has not bought, with the discount of the settings")
    void suggestsPromotions() throws Exception {
        sellSomething();
        unsoldProduct();
        Parent root = open();
        assertThat(rows(root, "promotionsTable")).isEmpty();

        chooseCustomer(root, "1001");

        List<PromotionSuggestion> suggestions = rows(root, "promotionsTable");
        assertThat(suggestions).extracting(PromotionSuggestion::productName).containsExactly("Esmalte Rosa Nude");
        assertThat(suggestions.get(0).discountPercentage()).isEqualTo(15);
        assertThat(suggestions.get(0).promotionalPrice()).isEqualByComparingTo("15300");

        chooseCustomer(root, "1002");
        assertThat(this.<PromotionSuggestion>rows(root, "promotionsTable")).extracting(PromotionSuggestion::productName)
            .containsExactly("Esmalte Rosa Nude", "Mascarilla Reparadora");
    }

    @Test
    @DisplayName("a customer who already bought every product of low rotation gets a message instead of an empty table")
    void customerWithNothingToSuggest() throws Exception {
        sellSomething();
        Product unsold = unsoldProduct();
        sell(ana, unsold, 1);
        Parent root = open();

        chooseCustomer(root, "1001");

        assertThat(rows(root, "promotionsTable")).isEmpty();
        assertThat(FxTest.onFxThread(() -> ((Label) ((TableView<?>) root.lookup("#promotionsTable")).getPlaceholder()).getText()))
            .contains("ya compró todos");
    }

    /** Books an appointment some days ago, with the clock of those days so the rules accept it, and leaves it in that state. */
    private void bookInThePast(int daysAgo, int hour, Customer customer, AppointmentStatus status) throws Exception {
        LocalDate day = LocalDate.now().minusDays(daysAgo);
        AppContext past = new AppContext(data, Clock.fixed(day.minusDays(3).atTime(8, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()), AiConfig.simulated());
        var appointment = past.appointments().book(past.customers().findById(customer.getId()), past.professionals().findById(camila.getId()),
            past.treatments().findById(cut.getId()), day.atTime(hour, 0), 16500);
        switch (status) {
            case COMPLETED -> past.appointments().complete(appointment.getId());
            case NO_SHOW -> past.appointments().markNoShow(appointment.getId());
            case CANCELLED -> past.appointments().cancel(appointment.getId());
            default -> { }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Number> weekdayBars(Parent root) throws Exception {
        BarChart<String, Number> chart = (BarChart<String, Number>) root.lookup("#weekdayChart");
        return FxTest.onFxThread(() -> {
            Map<String, Number> drawn = new java.util.LinkedHashMap<>();
            chart.getData().get(0).getData().forEach(point -> drawn.put(point.getXValue(), point.getYValue()));
            return drawn;
        });
    }

    private static String text(Parent root, String id) throws Exception {
        return FxTest.onFxThread(() -> ((Label) root.lookup("#" + id)).getText());
    }

    @Test
    @DisplayName("the summary shows the revenue, the appointments, the no-show rate and the new customers of the range")
    void summaryTiles() throws Exception {
        sellSomething();
        bookInThePast(5, 10, ana, AppointmentStatus.COMPLETED);
        bookInThePast(4, 10, laura, AppointmentStatus.COMPLETED);
        bookInThePast(3, 10, ana, AppointmentStatus.NO_SHOW);
        bookInThePast(2, 10, laura, AppointmentStatus.CANCELLED);
        Parent root = open();

        assertThat(text(root, "revenueValue")).isEqualTo(CurrencyFormatter.format(new BigDecimal("175000")));
        assertThat(text(root, "revenueNote")).isEqualTo("3 ventas");
        assertThat(text(root, "appointmentsValue")).isEqualTo("4");
        assertThat(text(root, "noShowValue")).isEqualTo("33 %");
        assertThat(text(root, "newCustomersValue")).isEqualTo("2");
    }

    @Test
    @DisplayName("the state chart counts how the appointments ended and the weekday chart leaves the cancelled ones out")
    void stateAndWeekdayCharts() throws Exception {
        bookInThePast(5, 10, ana, AppointmentStatus.COMPLETED);
        bookInThePast(4, 10, laura, AppointmentStatus.COMPLETED);
        bookInThePast(3, 10, ana, AppointmentStatus.NO_SHOW);
        bookInThePast(2, 10, laura, AppointmentStatus.CANCELLED);
        Parent root = open();

        assertThat(bars(root, "statusChart")).containsExactly(Map.entry("Pendiente", 0), Map.entry("Confirmada", 0),
            Map.entry("Completada", 2), Map.entry("Cancelada", 1), Map.entry("Inasistencia", 1));
        Map<String, Number> weekdays = weekdayBars(root);
        assertThat(weekdays).hasSize(7);
        assertThat(weekdays.values().stream().mapToInt(Number::intValue).sum()).isEqualTo(3);
        LocalDate completedDay = LocalDate.now().minusDays(5);
        String name = completedDay.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.forLanguageTag("es-CO"));
        assertThat(weekdays.get(name.substring(0, 1).toUpperCase() + name.substring(1)).intValue()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("with no appointments the summary shows dashes and zeros, and both charts give way to a message")
    void summaryWithoutAppointments() throws Exception {
        Parent root = open();

        assertThat(text(root, "appointmentsValue")).isEqualTo("0");
        assertThat(text(root, "noShowValue")).isEqualTo("—");
        assertThat(text(root, "revenueNote")).isEqualTo("0 ventas");
        assertThat(visible(root, "statusChart")).isFalse();
        assertThat(visible(root, "statusEmpty")).isTrue();
        assertThat(visible(root, "weekdayChart")).isFalse();
        assertThat(visible(root, "weekdayEmpty")).isTrue();
    }

    @Test
    @DisplayName("changing the dates updates the summary too")
    void summaryFollowsTheRange() throws Exception {
        sellSomething();
        bookInThePast(5, 10, ana, AppointmentStatus.COMPLETED);
        Parent root = open();
        assertThat(text(root, "appointmentsValue")).isEqualTo("1");

        setRange(root, LocalDate.now().minusDays(100), LocalDate.now().minusDays(50));

        assertThat(text(root, "appointmentsValue")).isEqualTo("0");
        assertThat(text(root, "revenueValue")).isEqualTo(CurrencyFormatter.format(BigDecimal.ZERO));
        assertThat(text(root, "newCustomersValue")).isEqualTo("0");
    }

    @Test
    @DisplayName("the income block adds the sales and the completed treatments, and shows the deposits held and the income lost")
    void incomeBlock() throws Exception {
        sellSomething();
        bookInThePast(5, 10, ana, AppointmentStatus.COMPLETED);
        bookInThePast(4, 10, laura, AppointmentStatus.COMPLETED);
        bookInThePast(3, 10, ana, AppointmentStatus.NO_SHOW);
        bookInThePast(2, 10, laura, AppointmentStatus.CANCELLED);
        bookAhead();
        Parent root = open();

        setRange(root, LocalDate.now().minusDays(30), LocalDate.now().plusDays(7));

        assertThat(text(root, "totalIncomeValue")).isEqualTo(CurrencyFormatter.format(new BigDecimal("285000")));
        assertThat(text(root, "totalIncomeNote")).contains(CurrencyFormatter.format(new BigDecimal("175000")), CurrencyFormatter.format(new BigDecimal("110000")));
        assertThat(text(root, "treatmentIncomeValue")).isEqualTo(CurrencyFormatter.format(new BigDecimal("110000")));
        assertThat(text(root, "pendingDepositsValue")).isEqualTo(CurrencyFormatter.format(new BigDecimal("43500")));
        assertThat(text(root, "lostIncomeValue")).isEqualTo(CurrencyFormatter.format(new BigDecimal("110000")));
        assertThat(text(root, "lostIncomeNote")).startsWith("2 citas");
    }

    @Test
    @DisplayName("the income charts rank the completed appointments by professional and by treatment")
    void incomeCharts() throws Exception {
        bookInThePast(5, 10, ana, AppointmentStatus.COMPLETED);
        bookInThePast(4, 10, laura, AppointmentStatus.COMPLETED);
        bookInThePast(3, 10, ana, AppointmentStatus.NO_SHOW);
        Parent root = open();

        assertThat(bars(root, "professionalIncomeChart")).hasSize(1);
        assertThat(bars(root, "professionalIncomeChart").get("Camila Ruiz").doubleValue()).isEqualTo(110000.0);
        assertThat(bars(root, "treatmentIncomeChart").get("Corte y peinado").doubleValue()).isEqualTo(110000.0);
        assertThat(visible(root, "professionalIncomeEmpty")).isFalse();
    }

    @Test
    @DisplayName("without income the block shows zeros and the charts give way to a message")
    void incomeWithoutData() throws Exception {
        Parent root = open();

        assertThat(text(root, "totalIncomeValue")).isEqualTo(CurrencyFormatter.format(BigDecimal.ZERO));
        assertThat(text(root, "lostIncomeNote")).startsWith("0 citas");
        assertThat(visible(root, "professionalIncomeChart")).isFalse();
        assertThat(visible(root, "professionalIncomeEmpty")).isTrue();
        assertThat(visible(root, "treatmentIncomeEmpty")).isTrue();
    }
}
