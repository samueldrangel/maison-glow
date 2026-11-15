package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.TreatmentService;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.util.TextSearch;
import com.maisonglow.view.AlertPresenter;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.shape.Circle;
import javafx.util.Callback;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Function;

/**
 * Controller of {@code TreatmentsPane.fxml}: the treatments tab of "Profesionales y servicios" (mockup M3).
 * It lists the treatments with their duration, price and state, and searches them by name. It only talks to
 * {@link TreatmentService}, never to a DAO.
 */
public final class TreatmentsController {

    private final TreatmentService treatments;
    private final ObservableList<Treatment> all = FXCollections.observableArrayList();
    private final FilteredList<Treatment> shown = new FilteredList<>(all);
    private AlertPresenter alerts;

    @FXML
    private TextField searchField;
    @FXML
    private TableView<Treatment> treatmentsTable;
    @FXML
    private TableColumn<Treatment, String> nameColumn;
    @FXML
    private TableColumn<Treatment, Integer> durationColumn;
    @FXML
    private TableColumn<Treatment, BigDecimal> priceColumn;
    @FXML
    private TableColumn<Treatment, Treatment> statusColumn;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public TreatmentsController(AppContext context) {
        this.treatments = Objects.requireNonNull(context, "context").treatments();
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
        alerts = new AlertPresenter(treatmentsTable);
        setUpTable();
        searchField.textProperty().addListener((observable, before, text) ->
            shown.setPredicate(treatment -> TextSearch.contains(treatment.getName(), text)));
        refresh();
    }

    private void setUpTable() {
        nameColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getName()));
        durationColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getDurationMinutes()));
        durationColumn.setCellFactory(showing(minutes -> minutes + " min"));
        priceColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getPrice()));
        priceColumn.setCellFactory(showing(CurrencyFormatter::format));
        statusColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        statusColumn.setCellFactory(column -> new StatusCell());
        SortedList<Treatment> sorted = new SortedList<>(shown);
        sorted.comparatorProperty().bind(treatmentsTable.comparatorProperty());
        treatmentsTable.setItems(sorted);
    }

    /** Reads the treatments again, so the table shows what is stored. */
    void refresh() {
        try {
            all.setAll(treatments.listAll());
        } catch (DataAccessException e) {
            alerts.showError(e);
        }
    }

    private static <T> Callback<TableColumn<Treatment, T>, TableCell<Treatment, T>> showing(Function<T, String> text) {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : text.apply(item));
            }
        };
    }

    /** The state of a treatment as a chip with a dot. */
    private static final class StatusCell extends TableCell<Treatment, Treatment> {
        @Override
        protected void updateItem(Treatment treatment, boolean empty) {
            super.updateItem(treatment, empty);
            if (empty || treatment == null) {
                setGraphic(null);
                return;
            }
            Label chip = new Label(treatment.isActive() ? "Activo" : "Inactivo");
            Circle dot = new Circle(3.5);
            dot.getStyleClass().add("chip-dot");
            chip.setGraphic(dot);
            chip.getStyleClass().add(treatment.isActive() ? "chip-success" : "chip-neutral");
            setGraphic(chip);
        }
    }
}
