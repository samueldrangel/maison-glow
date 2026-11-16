package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.Sale;
import com.maisonglow.service.AdminNotifier;
import com.maisonglow.service.AppointmentService;
import com.maisonglow.service.CustomerService;
import com.maisonglow.service.LowRotationService;
import com.maisonglow.service.ProductSales;
import com.maisonglow.service.PromotionService;
import com.maisonglow.service.PromotionSuggestion;
import com.maisonglow.service.SaleService;
import com.maisonglow.service.SettingsService;
import com.maisonglow.service.SimulatedAdminNotifier;
import com.maisonglow.service.StatisticsService;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.appointment.AppointmentLook;
import com.maisonglow.view.statistics.RangeSummary;
import com.maisonglow.view.FieldValidator;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.util.StringConverter;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Controller of {@code StatisticsView.fxml}: statistics and business intelligence (mockup M7). The range of dates
 * governs the charts of the most sold products and the most booked treatments; the frequent customers are of the
 * whole history. A chart with nothing to show is replaced by a message, never left blank. It only talks to
 * {@link StatisticsService}, never to a DAO.
 */
public final class StatisticsController {

    /** How many entries each ranking shows. */
    private static final int TOP = 5;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final StatisticsService statistics;
    private final AppointmentService appointments;
    private final SaleService sales;
    private final LowRotationService lowRotation;
    private final PromotionService promotions;
    private final CustomerService customers;
    private final SettingsService settings;
    private AdminNotifier notifier = new SimulatedAdminNotifier(System.out);
    private final Clock clock;
    private final FieldValidator rangeFields = new FieldValidator();
    private final ObservableList<Map.Entry<String, Integer>> frequent = FXCollections.observableArrayList();
    private boolean settingRange;
    private AlertPresenter alerts;

    @FXML
    private ToggleGroup rangeChips;
    @FXML
    private ToggleButton last7Chip;
    @FXML
    private ToggleButton last30Chip;
    @FXML
    private ToggleButton last90Chip;
    @FXML
    private DatePicker fromPicker;
    @FXML
    private DatePicker toPicker;
    @FXML
    private Label rangeMessage;
    @FXML
    private Label revenueValue;
    @FXML
    private Label revenueNote;
    @FXML
    private Label appointmentsValue;
    @FXML
    private Label noShowValue;
    @FXML
    private Label newCustomersValue;
    @FXML
    private BarChart<Number, String> statusChart;
    @FXML
    private Label statusEmpty;
    @FXML
    private BarChart<String, Number> weekdayChart;
    @FXML
    private Label weekdayEmpty;
    @FXML
    private BarChart<Number, String> productsChart;
    @FXML
    private Label productsEmpty;
    @FXML
    private BarChart<Number, String> treatmentsChart;
    @FXML
    private Label treatmentsEmpty;
    @FXML
    private TableView<Map.Entry<String, Integer>> customersTable;
    @FXML
    private TableColumn<Map.Entry<String, Integer>, Map.Entry<String, Integer>> rankColumn;
    @FXML
    private TableColumn<Map.Entry<String, Integer>, String> customerNameColumn;
    @FXML
    private TableColumn<Map.Entry<String, Integer>, Integer> purchasesColumn;
    @FXML
    private Label lowRotationHint;
    @FXML
    private TableView<ProductSales> lowRotationTable;
    @FXML
    private TableColumn<ProductSales, String> lowNameColumn;
    @FXML
    private TableColumn<ProductSales, String> lowStockColumn;
    @FXML
    private TableColumn<ProductSales, String> lowSoldColumn;
    @FXML
    private Button notifyButton;
    @FXML
    private ComboBox<Customer> promotionsCustomerBox;
    @FXML
    private TableView<PromotionSuggestion> promotionsTable;
    @FXML
    private TableColumn<PromotionSuggestion, String> promoProductColumn;
    @FXML
    private TableColumn<PromotionSuggestion, String> promoRegularColumn;
    @FXML
    private TableColumn<PromotionSuggestion, String> promoDiscountColumn;
    @FXML
    private TableColumn<PromotionSuggestion, String> promoPriceColumn;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public StatisticsController(AppContext context) {
        Objects.requireNonNull(context, "context");
        this.statistics = context.statistics();
        this.appointments = context.appointments();
        this.sales = context.sales();
        this.lowRotation = context.lowRotation();
        this.promotions = context.promotions();
        this.customers = context.customers();
        this.settings = context.settings();
        this.clock = context.clock();
    }

    /**
     * Replaces the presenter of dialogs, which the tests use so that no real window is opened.
     *
     * @param presenter the presenter to use from now on
     */
    public void useAlerts(AlertPresenter presenter) {
        this.alerts = Objects.requireNonNull(presenter, "presenter");
    }

    /**
     * Replaces who is told about the products of low rotation, which the tests use to read the notice.
     *
     * @param newNotifier the notifier to use from now on
     */
    public void useNotifier(AdminNotifier newNotifier) {
        this.notifier = Objects.requireNonNull(newNotifier, "newNotifier");
    }

    @FXML
    private void initialize() {
        alerts = new AlertPresenter(customersTable);
        rangeFields.field(toPicker, rangeMessage, "fecha");
        wholeNumbers(productsChart.getXAxis());
        wholeNumbers(treatmentsChart.getXAxis());
        wholeNumbers(statusChart.getXAxis());
        wholeNumbers(weekdayChart.getYAxis());
        setUpCustomers();
        setUpRange();
        setUpInsights();
        refresh();
        refreshLowRotation();
    }

    /** The counts are whole numbers, so an axis never labels 0,5 or 2,25: those marks stay unlabelled. */
    private static void wholeNumbers(javafx.scene.chart.Axis<?> axis) {
        ((NumberAxis) axis).setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return value.doubleValue() == Math.rint(value.doubleValue()) ? String.valueOf(value.intValue()) : "";
            }

            @Override
            public Number fromString(String text) {
                return text == null || text.isBlank() ? 0 : Integer.valueOf(text);
            }
        });
    }

    private void setUpCustomers() {
        rankColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        rankColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Map.Entry<String, Integer> entry, boolean empty) {
                super.updateItem(entry, empty);
                setText(empty || entry == null ? null : String.valueOf(getIndex() + 1));
            }
        });
        customerNameColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getKey()));
        purchasesColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getValue()));
        customersTable.setItems(frequent);
    }

    private void setUpInsights() {
        lowNameColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().product().getName()));
        lowStockColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().product().getStock() + " uds."));
        lowSoldColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().unitsSold() + " uds."));
        notifyButton.setOnAction(event -> notifyAdministrator());
        promotionsCustomerBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Customer customer) {
                return customer == null ? "" : customer.getName() + " · " + customer.getDocument();
            }

            @Override
            public Customer fromString(String text) {
                return null;
            }
        });
        try {
            promotionsCustomerBox.getItems().setAll(customers.listAll());
        } catch (DataAccessException e) {
            alerts.showError(e);
        }
        promoProductColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().productName()));
        promoRegularColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(CurrencyFormatter.format(row.getValue().regularPrice())));
        promoDiscountColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(String.format("%.0f %%", row.getValue().discountPercentage())));
        promoPriceColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(CurrencyFormatter.format(row.getValue().promotionalPrice())));
        promotionsCustomerBox.valueProperty().addListener((observable, before, customer) -> showPromotions(customer));
    }

    /** Lists the products that sold least in the window of the settings, and what the business can tell the administrator. */
    void refreshLowRotation() {
        try {
            List<ProductSales> low = lowRotation.lowRotationProducts(LocalDate.now(clock));
            lowRotationTable.getItems().setAll(low);
            notifyButton.setDisable(low.isEmpty());
            lowRotationHint.setText("Los que menos se vendieron en los últimos " + settings.lowRotationWindowDays() + " días.");
        } catch (ValidationException | DataAccessException e) {
            alerts.showError(e);
        }
    }

    private void notifyAdministrator() {
        try {
            lowRotation.notifyAdministrator(notifier, LocalDate.now(clock));
            alerts.toast("Aviso enviado al administrador (simulado).");
        } catch (ValidationException | DataAccessException e) {
            alerts.showError(e);
        }
    }

    /** Suggests, for the chosen customer, the products of low rotation that customer has not bought yet. */
    private void showPromotions(Customer customer) {
        promotionsTable.getItems().clear();
        if (customer == null) {
            promotionsTable.setPlaceholder(notice("Elige un cliente para ver sus promociones"));
            return;
        }
        try {
            promotionsTable.getItems().setAll(promotions.suggestPromotions(customer.getId(), LocalDate.now(clock)));
            promotionsTable.setPlaceholder(notice("Este cliente ya compró todos los productos de baja rotación"));
        } catch (ValidationException | DataAccessException e) {
            alerts.showError(e);
        }
    }

    private static Label notice(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("placeholder-notice");
        return label;
    }

    private void setUpRange() {
        StringConverter<LocalDate> converter = new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : DAY.format(date);
            }

            @Override
            public LocalDate fromString(String text) {
                try {
                    return text == null || text.isBlank() ? null : LocalDate.parse(text.trim(), DAY);
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        };
        fromPicker.setConverter(converter);
        toPicker.setConverter(converter);
        last7Chip.setUserData(7);
        last30Chip.setUserData(30);
        last90Chip.setUserData(90);
        rangeChips.selectedToggleProperty().addListener((observable, before, chip) -> {
            if (chip != null) {
                int days = (Integer) chip.getUserData();
                LocalDate today = LocalDate.now(clock);
                setRange(today.minusDays(days - 1L), today);
            }
        });
        fromPicker.valueProperty().addListener((observable, before, now) -> rangeTypedByHand());
        toPicker.valueProperty().addListener((observable, before, now) -> rangeTypedByHand());
        LocalDate today = LocalDate.now(clock);
        setRange(today.minusDays(29), today);
    }

    /** Dates written by hand no longer match a shortcut, so none stays chosen. */
    private void rangeTypedByHand() {
        if (!settingRange) {
            rangeChips.selectToggle(null);
            refresh();
        }
    }

    private void setRange(LocalDate from, LocalDate to) {
        settingRange = true;
        fromPicker.setValue(from);
        toPicker.setValue(to);
        settingRange = false;
        refresh();
    }

    /** Reads the rankings again for the dates chosen. */
    void refresh() {
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();
        rangeFields.clear();
        try {
            if (from != null && to != null) {
                show(productsChart, productsEmpty, statistics.topProducts(TOP, from, to));
                show(treatmentsChart, treatmentsEmpty, statistics.topTreatments(TOP, from, to));
                showSummary(from, to);
            }
            frequent.setAll(new ArrayList<>(statistics.frequentCustomers(TOP).entrySet()));
        } catch (ValidationException e) {
            if (!rangeFields.show(e)) {
                alerts.showError(e);
            }
        } catch (DataAccessException e) {
            alerts.showError(e);
        }
    }

    /** Works out the summary of the range and draws how the appointments ended and on which days they fall. */
    private void showSummary(LocalDate from, LocalDate to) throws ValidationException, DataAccessException {
        List<Customer> everyone = customers.listAll();
        List<Sale> inTheRange = new ArrayList<>();
        for (Customer customer : everyone) {
            for (Sale sale : sales.listByCustomer(customer.getId())) {
                LocalDate day = sale.getSoldAt().toLocalDate();
                if (!day.isBefore(from) && !day.isAfter(to)) {
                    inTheRange.add(sale);
                }
            }
        }
        List<Appointment> booked = appointments.listBetween(from, to);
        RangeSummary summary = RangeSummary.of(inTheRange, booked, everyone, from, to);
        revenueValue.setText(CurrencyFormatter.format(summary.revenue()));
        revenueNote.setText(summary.sales() + (summary.sales() == 1 ? " venta" : " ventas"));
        appointmentsValue.setText(String.valueOf(summary.appointments()));
        noShowValue.setText(summary.noShowRate().isPresent() ? Math.round(summary.noShowRate().getAsDouble() * 100) + " %" : "—");
        newCustomersValue.setText(String.valueOf(summary.newCustomers()));
        Map<String, Integer> states = new LinkedHashMap<>();
        summary.byStatus().forEach((status, count) -> states.put(AppointmentLook.of(status).label(), count));
        show(statusChart, statusEmpty, summary.appointments() == 0 ? Map.of() : states);
        XYChart.Series<String, Number> weekdays = new XYChart.Series<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            String name = day.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es-CO"));
            weekdays.getData().add(new XYChart.Data<>(name.substring(0, 1).toUpperCase() + name.substring(1), summary.byWeekday().get(day)));
        }
        boolean none = summary.byWeekday().values().stream().mapToInt(Integer::intValue).sum() == 0;
        weekdayChart.getData().setAll(List.of(weekdays));
        weekdayChart.setVisible(!none);
        weekdayEmpty.setVisible(none);
    }

    /** Draws a ranking as horizontal bars, the first on top, or shows the message when there is nothing to draw. */
    private static void show(BarChart<Number, String> chart, Label empty, Map<String, Integer> ranking) {
        XYChart.Series<Number, String> series = new XYChart.Series<>();
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(ranking.entrySet());
        Collections.reverse(entries);
        entries.forEach(entry -> series.getData().add(new XYChart.Data<>(entry.getValue(), entry.getKey())));
        chart.getData().setAll(List.of(series));
        chart.setVisible(!ranking.isEmpty());
        empty.setVisible(ranking.isEmpty());
    }
}
