package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.TreatmentData;
import com.maisonglow.service.TreatmentService;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.util.NumberInput;
import com.maisonglow.util.TextSearch;
import com.maisonglow.util.Validator;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FieldValidator;
import com.maisonglow.view.Icons;
import com.maisonglow.view.SlidePanel;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
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
    private final FieldValidator formFields = new FieldValidator();
    private Treatment editing;
    private SlidePanel formDrawer;
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
    @FXML
    private TableColumn<Treatment, Treatment> actionsColumn;
    @FXML
    private Button newButton;
    @FXML
    private ScrollPane formPanel;
    @FXML
    private Label formTitle;
    @FXML
    private TextField nameField;
    @FXML
    private Label nameMessage;
    @FXML
    private TextField descriptionField;
    @FXML
    private TextField priceField;
    @FXML
    private Label priceMessage;
    @FXML
    private TextField durationField;
    @FXML
    private Label durationMessage;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

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
        setUpForm();
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
        actionsColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        actionsColumn.setCellFactory(column -> new ActionsCell());
        SortedList<Treatment> sorted = new SortedList<>(shown);
        sorted.comparatorProperty().bind(treatmentsTable.comparatorProperty());
        treatmentsTable.setItems(sorted);
    }

    private void setUpForm() {
        formDrawer = new SlidePanel(formPanel, formPanel.getContent(), formPanel.getPrefWidth());
        formFields.field(nameField, nameMessage, "nombre", "llamado");
        formFields.field(priceField, priceMessage, "precio");
        formFields.field(durationField, durationMessage, "duración");
        newButton.setOnAction(event -> {
            startRegistering();
            formDrawer.open();
        });
        cancelButton.setOnAction(event -> {
            startRegistering();
            formDrawer.close();
        });
        saveButton.setOnAction(event -> save());
        startRegistering();
    }

    /** Leaves the form ready to register a new treatment. */
    private void startRegistering() {
        editing = null;
        formTitle.setText("Registrar tratamiento");
        nameField.clear();
        descriptionField.clear();
        priceField.clear();
        durationField.clear();
        formFields.clear();
        nameField.requestFocus();
    }

    /** Fills the form with a treatment so it can be changed. */
    private void startEditing(Treatment treatment) {
        editing = treatment;
        formTitle.setText("Editar tratamiento");
        nameField.setText(treatment.getName());
        descriptionField.setText(treatment.getDescription());
        priceField.setText(treatment.getPrice().stripTrailingZeros().toPlainString());
        durationField.setText(String.valueOf(treatment.getDurationMinutes()));
        formFields.clear();
        formDrawer.open();
        nameField.requestFocus();
    }

    private void save() {
        try {
            TreatmentData data = readForm();
            if (editing == null) {
                treatments.register(data);
                alerts.toast("Tratamiento registrado.");
            } else {
                treatments.update(editing.getId(), data);
                alerts.toast("Tratamiento actualizado.");
            }
            startRegistering();
            formDrawer.close();
            refresh();
        } catch (ValidationException | BusinessRuleException e) {
            if (!formFields.show(e)) {
                alerts.showError(e);
            }
        } catch (DataAccessException e) {
            alerts.showError(e);
        }
    }

    /** Reads the fields in the order they appear, so the first mistake is the one that shows. */
    private TreatmentData readForm() throws ValidationException {
        Validator.requireText(nameField.getText(), "nombre");
        BigDecimal price = NumberInput.money(priceField.getText(), "precio");
        int minutes = NumberInput.wholeNumber(durationField.getText(), "duración");
        return new TreatmentData(nameField.getText(), descriptionField.getText(), price, minutes);
    }

    private void deactivate(Treatment treatment) {
        if (!alerts.confirm("¿Desactivar «" + treatment.getName() + "»? Dejará de poder asignarse y agendarse; "
            + "podrás reactivarlo cuando quieras.")) {
            return;
        }
        changeActive(treatment, false, "Tratamiento desactivado.");
    }

    private void changeActive(Treatment treatment, boolean active, String notice) {
        try {
            if (active) {
                treatments.activate(treatment.getId());
            } else {
                treatments.deactivate(treatment.getId());
            }
            alerts.toast(notice);
            refresh();
        } catch (BusinessRuleException | DataAccessException e) {
            alerts.showError(e);
        }
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

    /** Edit, and deactivate or reactivate, as small icon buttons at the end of each row. */
    private final class ActionsCell extends TableCell<Treatment, Treatment> {
        @Override
        protected void updateItem(Treatment treatment, boolean empty) {
            super.updateItem(treatment, empty);
            if (empty || treatment == null) {
                setGraphic(null);
                return;
            }
            Button edit = iconButton(Icons.Kind.EDIT, "Editar " + treatment.getName());
            edit.setOnAction(event -> startEditing(treatment));
            Button toggle = treatment.isActive()
                ? iconButton(Icons.Kind.TRASH, "Desactivar " + treatment.getName())
                : iconButton(Icons.Kind.RESTORE, "Reactivar " + treatment.getName());
            if (treatment.isActive()) {
                toggle.getStyleClass().add("danger");
            }
            toggle.setOnAction(event -> {
                if (treatment.isActive()) {
                    deactivate(treatment);
                } else {
                    changeActive(treatment, true, "Tratamiento reactivado.");
                }
            });
            setGraphic(new HBox(4, edit, toggle));
        }

        private Button iconButton(Icons.Kind kind, String description) {
            Button button = new Button();
            button.setGraphic(Icons.small(kind, 16));
            button.getStyleClass().add("icon-button");
            button.setAccessibleText(description);
            button.setTooltip(new Tooltip(description));
            return button;
        }
    }
}
