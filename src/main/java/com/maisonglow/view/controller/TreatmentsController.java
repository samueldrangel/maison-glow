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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Objects;

/**
 * Controller of {@code TreatmentsPane.fxml}: the treatments tab of "Profesionales y tratamientos" (mockup M3).
 * Each treatment is a card with its name, what it includes, its duration, price and state, and the list can be
 * searched by name. It only talks to {@link TreatmentService}, never to a DAO.
 */
public final class TreatmentsController {

    /** Longest height of the description on a card; a longer text ends in an ellipsis. */
    private static final double DESCRIPTION_HEIGHT = 58;

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
    private FlowPane cardsPane;
    @FXML
    private Label countLabel;
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
    private TextArea descriptionField;
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
        alerts = new AlertPresenter(cardsPane);
        setUpForm();
        searchField.textProperty().addListener((observable, before, text) -> {
            shown.setPredicate(treatment -> TextSearch.contains(treatment.getName(), text));
            showCards();
        });
        refresh();
    }

    /** Reads the treatments again, so the cards show what is stored. */
    void refresh() {
        try {
            all.setAll(treatments.listAll());
            showCards();
        } catch (DataAccessException e) {
            alerts.showError(e);
        }
    }

    private void showCards() {
        cardsPane.getChildren().setAll(shown.stream()
            .sorted(Comparator.comparing(Treatment::getName, String.CASE_INSENSITIVE_ORDER)).map(this::card).toList());
        countLabel.setText("Mostrando " + shown.size() + " de " + all.size()
            + (all.size() == 1 ? " tratamiento" : " tratamientos"));
    }

    /** One treatment as a card: state and actions on top, then the name, what it includes, the duration and the price. */
    private Node card(Treatment treatment) {
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox top = new HBox(2, status(treatment), gap, actions(treatment));
        top.setAlignment(Pos.CENTER_LEFT);
        Label name = text(treatment.getName(), "tre-name");
        String included = treatment.getDescription() == null ? "" : treatment.getDescription().trim();
        Label description = included.isEmpty() ? text("Sin descripción", "tre-description-empty") : text(included, "tre-description");
        description.setMaxHeight(DESCRIPTION_HEIGHT);
        Region fill = new Region();
        VBox.setVgrow(fill, Priority.ALWAYS);
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);
        HBox bottom = new HBox(text(treatment.getDurationMinutes() + " min", "tre-meta"), space,
            text(CurrencyFormatter.format(treatment.getPrice()), "tre-price"));
        bottom.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(8, top, name, description, fill, bottom);
        card.getStyleClass().addAll("card", "tre-card");
        if (!treatment.isActive()) {
            card.getStyleClass().add("tre-card-inactive");
        }
        return card;
    }

    /** Edit, and deactivate or reactivate, as small icon buttons at the top of each card. */
    private Node actions(Treatment treatment) {
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
        return new HBox(4, edit, toggle);
    }

    private static Button iconButton(Icons.Kind kind, String description) {
        Button button = new Button();
        button.setGraphic(Icons.small(kind, 16));
        button.getStyleClass().add("icon-button");
        button.setAccessibleText(description);
        button.setTooltip(new Tooltip(description));
        return button;
    }

    private static Label text(String text, String style) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add(style);
        return label;
    }

    /** The state of a treatment as a chip with a dot. */
    private static Label status(Treatment treatment) {
        Label chip = new Label(treatment.isActive() ? "Activo" : "Inactivo");
        Circle dot = new Circle(3.5);
        dot.getStyleClass().add("chip-dot");
        chip.setGraphic(dot);
        chip.getStyleClass().add(treatment.isActive() ? "chip-success" : "chip-neutral");
        return chip;
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
}
