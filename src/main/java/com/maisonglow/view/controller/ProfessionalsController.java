package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.ProfessionalService;
import com.maisonglow.service.TreatmentService;
import com.maisonglow.service.WorkSchedule;
import com.maisonglow.util.DateFormats;
import com.maisonglow.util.Initials;
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
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.StringConverter;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

/**
 * Controller of {@code ProfessionalsPane.fxml}: the professionals tab of "Profesionales y tratamientos"
 * (mockup M3). Each professional is a card with the name, specialty, working hours and state; the list can
 * be searched and filtered by the treatment a professional offers. It only talks to
 * {@link ProfessionalService} and {@link TreatmentService}, never to a DAO.
 */
public final class ProfessionalsController {

    /** The entry of the treatment filter that lets every professional through. */
    private static final Treatment ALL_TREATMENTS = new Treatment(0, "Todos los tratamientos", "", 1, 1);

    private final ProfessionalService professionals;
    private final TreatmentService treatments;
    private final ObservableList<Professional> all = FXCollections.observableArrayList();
    private final FilteredList<Professional> shown = new FilteredList<>(all);
    private final FieldValidator formFields = new FieldValidator();
    private Professional editing;
    private SlidePanel formDrawer;
    private AlertPresenter alerts;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<Treatment> treatmentFilter;
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
    private TextField documentField;
    @FXML
    private Label documentMessage;
    @FXML
    private TextField nameField;
    @FXML
    private Label nameMessage;
    @FXML
    private TextField phoneField;
    @FXML
    private Label phoneMessage;
    @FXML
    private TextField emailField;
    @FXML
    private Label emailMessage;
    @FXML
    private TextField specialtyField;
    @FXML
    private Label specialtyMessage;
    @FXML
    private TextField startField;
    @FXML
    private TextField endField;
    @FXML
    private Label scheduleMessage;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public ProfessionalsController(AppContext context) {
        Objects.requireNonNull(context, "context");
        this.professionals = context.professionals();
        this.treatments = context.treatments();
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
        setUpFilters();
        setUpForm();
        refresh();
    }

    private void setUpFilters() {
        treatmentFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(Treatment treatment) {
                return treatment == null ? "" : treatment.getName();
            }

            @Override
            public Treatment fromString(String text) {
                return null;
            }
        });
        searchField.textProperty().addListener((observable, before, text) -> applyFilters());
        treatmentFilter.valueProperty().addListener((observable, before, treatment) -> applyFilters());
    }

    private void setUpForm() {
        formDrawer = new SlidePanel(formPanel, formPanel.getContent(), formPanel.getPrefWidth());
        // The order matters: the first keyword that appears in the message picks the field.
        formFields.field(documentField, documentMessage, "documento");
        formFields.field(nameField, nameMessage, "nombre");
        formFields.field(phoneField, phoneMessage, "teléfono");
        formFields.field(emailField, emailMessage, "correo");
        formFields.field(specialtyField, specialtyMessage, "especialidad");
        formFields.field(endField, scheduleMessage, "jornada", "hora");
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

    /** Leaves the form ready to register a new professional, with the usual working day filled in. */
    private void startRegistering() {
        editing = null;
        formTitle.setText("Registrar profesional");
        documentField.clear();
        nameField.clear();
        phoneField.clear();
        emailField.clear();
        specialtyField.clear();
        startField.setText("08:00");
        endField.setText("17:00");
        documentField.setDisable(false);
        formFields.clear();
        documentField.requestFocus();
    }

    /** Fills the form with a professional so it can be changed; the document cannot be changed. */
    private void startEditing(Professional professional) {
        editing = professional;
        formTitle.setText("Editar profesional");
        documentField.setText(professional.getDocument());
        nameField.setText(professional.getName());
        phoneField.setText(professional.getPhone());
        emailField.setText(professional.getEmail());
        specialtyField.setText(professional.getSpecialty());
        startField.setText(DateFormats.time(professional.getWorkStart()));
        endField.setText(DateFormats.time(professional.getWorkEnd()));
        documentField.setDisable(true);
        formFields.clear();
        formDrawer.open();
        nameField.requestFocus();
    }

    private void save() {
        try {
            ContactData contact = new ContactData(documentField.getText(), nameField.getText(),
                phoneField.getText(), emailField.getText());
            contact.validate();
            Validator.requireText(specialtyField.getText(), "especialidad");
            WorkSchedule schedule = new WorkSchedule(time(startField, "entrada"), time(endField, "salida"));
            if (editing == null) {
                professionals.register(contact, specialtyField.getText(), schedule);
                alerts.toast("Profesional registrado.");
            } else {
                professionals.update(editing.getId(), contact, specialtyField.getText());
                professionals.changeWorkingHours(editing.getId(), schedule);
                alerts.toast("Profesional actualizado.");
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

    private static LocalTime time(TextField field, String which) throws ValidationException {
        try {
            return LocalTime.parse(field.getText().trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("La hora de " + which + " de la jornada debe tener el formato HH:mm, por ejemplo 08:00.", e);
        }
    }

    private void deactivate(Professional professional) {
        if (!alerts.confirm("¿Desactivar a «" + professional.getName() + "»? Dejará de aparecer en la agenda; "
            + "podrás reactivar al profesional cuando quieras.")) {
            return;
        }
        changeActive(professional, false, "Profesional desactivado.");
    }

    private void changeActive(Professional professional, boolean active, String notice) {
        try {
            if (active) {
                professionals.activate(professional.getId());
            } else {
                professionals.deactivate(professional.getId());
            }
            alerts.toast(notice);
            refresh();
        } catch (BusinessRuleException | DataAccessException e) {
            alerts.showError(e);
        }
    }

    private void applyFilters() {
        String text = searchField.getText();
        Treatment wanted = treatmentFilter.getValue();
        shown.setPredicate(professional ->
            (TextSearch.contains(professional.getName(), text) || TextSearch.contains(professional.getSpecialty(), text))
                && (wanted == null || wanted == ALL_TREATMENTS || professional.getTreatments().contains(wanted)));
        showCards();
    }

    /** Reads the professionals and treatments again, so the cards show what is stored. */
    void refresh() {
        try {
            all.setAll(professionals.listAll());
            Treatment chosen = treatmentFilter.getValue();
            List<Treatment> options = new java.util.ArrayList<>(List.of(ALL_TREATMENTS));
            options.addAll(treatments.listAll());
            treatmentFilter.getItems().setAll(options);
            treatmentFilter.setValue(options.stream().filter(option -> option.equals(chosen)).findFirst().orElse(ALL_TREATMENTS));
            showCards();
        } catch (DataAccessException e) {
            alerts.showError(e);
        }
    }

    private void showCards() {
        cardsPane.getChildren().setAll(shown.stream().map(this::card).toList());
        countLabel.setText("Mostrando " + shown.size() + " de " + all.size()
            + (all.size() == 1 ? " profesional" : " profesionales"));
    }

    /** One professional as a card: medallion with the initials, name, specialty, hours, services and state. */
    private Node card(Professional professional) {
        Label initials = new Label(Initials.of(professional.getName()));
        initials.getStyleClass().add("pro-initials");
        StackPane medallion = new StackPane(initials);
        medallion.getStyleClass().add("pro-medallion");
        medallion.setMinSize(64, 64);
        medallion.setPrefSize(64, 64);
        medallion.setMaxSize(64, 64);
        int services = professional.getTreatments().size();
        VBox card = new VBox(actions(professional), medallion,
            text(professional.getName(), "pro-name"),
            text(professional.getSpecialty(), "pro-specialty"),
            text(DateFormats.time(professional.getWorkStart()) + " - " + DateFormats.time(professional.getWorkEnd()), "pro-hours"),
            text(services == 0 ? "Sin tratamientos" : services + (services == 1 ? " tratamiento" : " tratamientos"), "pro-hours"),
            status(professional));
        card.getStyleClass().addAll("card", "pro-card");
        if (!professional.isActive()) {
            card.getStyleClass().add("pro-card-inactive");
        }
        return card;
    }

    /** The row at the top of a card with its action buttons. */
    private Node actions(Professional professional) {
        Button edit = iconButton(Icons.Kind.EDIT, "Editar " + professional.getName());
        edit.setOnAction(event -> startEditing(professional));
        Button toggle = professional.isActive()
            ? iconButton(Icons.Kind.TRASH, "Desactivar " + professional.getName())
            : iconButton(Icons.Kind.RESTORE, "Reactivar " + professional.getName());
        if (professional.isActive()) {
            toggle.getStyleClass().add("danger");
        }
        toggle.setOnAction(event -> {
            if (professional.isActive()) {
                deactivate(professional);
            } else {
                changeActive(professional, true, "Profesional reactivado.");
            }
        });
        HBox row = new HBox(2, edit, toggle);
        row.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        return row;
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
        label.getStyleClass().add(style);
        return label;
    }

    private static Label status(Professional professional) {
        Label chip = new Label(professional.isActive() ? "Activo" : "Inactivo");
        Circle dot = new Circle(3.5);
        dot.getStyleClass().add("chip-dot");
        chip.setGraphic(dot);
        chip.getStyleClass().add(professional.isActive() ? "chip-success" : "chip-neutral");
        return chip;
    }
}
