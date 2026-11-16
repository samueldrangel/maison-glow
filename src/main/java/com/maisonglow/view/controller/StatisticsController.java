package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.service.StatisticsService;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FieldValidator;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.util.StringConverter;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public StatisticsController(AppContext context) {
        Objects.requireNonNull(context, "context");
        this.statistics = context.statistics();
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

    @FXML
    private void initialize() {
        alerts = new AlertPresenter(customersTable);
        rangeFields.field(toPicker, rangeMessage, "fecha");
        setUpCustomers();
        setUpRange();
        refresh();
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
