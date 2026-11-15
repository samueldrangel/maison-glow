package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.AppointmentService;
import com.maisonglow.service.CustomerService;
import com.maisonglow.service.ProfessionalService;
import com.maisonglow.service.SettingsService;
import com.maisonglow.service.TreatmentService;
import com.maisonglow.service.rule.DepositRule;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.util.DateFormats;
import com.maisonglow.util.Validator;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FieldValidator;
import com.maisonglow.view.SlidePanel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Controller of {@code AppointmentFormPane.fxml}: the panel of the agenda to book or reschedule an appointment.
 * The professionals offered are the ones who offer the chosen treatment, and a summary recalculates, with what
 * has been chosen so far, when the appointment ends, whether the professional is free, and the deposit and the
 * reminder that the rules set. It only talks to services, never to a DAO.
 */
public final class AppointmentFormController {

    private static final String NOT_YET = "—";

    private final AppointmentService appointments;
    private final CustomerService customers;
    private final TreatmentService treatments;
    private final ProfessionalService professionals;
    private final SettingsService settings;
    private final DepositRule depositRule;
    private final FieldValidator fields = new FieldValidator();
    private Appointment rescheduling;
    private SlidePanel drawer;
    private Consumer<LocalDate> onSaved = day -> { };
    private AlertPresenter alerts;

    @FXML
    private ScrollPane formPanel;
    @FXML
    private Label formTitle;
    @FXML
    private ComboBox<Customer> customerBox;
    @FXML
    private Label customerMessage;
    @FXML
    private ComboBox<Treatment> treatmentBox;
    @FXML
    private Label treatmentMessage;
    @FXML
    private ComboBox<Professional> professionalBox;
    @FXML
    private Label professionalMessage;
    @FXML
    private DatePicker dateField;
    @FXML
    private TextField timeField;
    @FXML
    private Label timeMessage;
    @FXML
    private Label endValue;
    @FXML
    private Label availabilityValue;
    @FXML
    private Label depositValue;
    @FXML
    private Label reminderValue;
    @FXML
    private Label summaryNote;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public AppointmentFormController(AppContext context) {
        Objects.requireNonNull(context, "context");
        this.appointments = context.appointments();
        this.customers = context.customers();
        this.treatments = context.treatments();
        this.professionals = context.professionals();
        this.settings = context.settings();
        this.depositRule = new DepositRule(context.settings());
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
     * Sets what happens after an appointment is saved.
     *
     * @param action receives the day of the appointment, so the agenda can show it
     */
    public void onSaved(Consumer<LocalDate> action) {
        this.onSaved = Objects.requireNonNull(action, "action");
    }

    @FXML
    private void initialize() {
        alerts = new AlertPresenter(formPanel);
        drawer = new SlidePanel(formPanel, formPanel.getContent(), formPanel.getPrefWidth());
        showing(customerBox, customer -> customer.getName() + " · " + customer.getDocument());
        showing(treatmentBox, treatment -> treatment.getName() + " · " + treatment.getDurationMinutes() + " min");
        showing(professionalBox, Professional::getName);
        fields.field(customerBox, customerMessage, "cliente");
        fields.field(treatmentBox, treatmentMessage, "tratamiento");
        fields.field(professionalBox, professionalMessage, "profesional", "ofrece");
        fields.field(timeField, timeMessage, "hora", "fecha", "atiende", "cita en ese", "anticipación");
        treatmentBox.valueProperty().addListener((observable, before, treatment) -> offerProfessionalsOf(treatment));
        customerBox.valueProperty().addListener((observable, before, now) -> updateSummary());
        professionalBox.valueProperty().addListener((observable, before, now) -> updateSummary());
        dateField.valueProperty().addListener((observable, before, now) -> updateSummary());
        timeField.textProperty().addListener((observable, before, now) -> updateSummary());
        cancelButton.setOnAction(event -> drawer.close());
        saveButton.setOnAction(event -> save());
        updateSummary();
    }

    /**
     * Opens the panel empty, to book a new appointment.
     *
     * @param day the day the agenda is showing, which the date starts as
     */
    public void openToBook(LocalDate day) {
        rescheduling = null;
        formTitle.setText("Agendar cita");
        saveButton.setText("Agendar");
        customerBox.setDisable(false);
        treatmentBox.setDisable(false);
        professionalBox.setDisable(false);
        try {
            customerBox.getItems().setAll(customers.listAll());
            treatmentBox.getItems().setAll(treatments.listActive());
        } catch (DataAccessException e) {
            alerts.showError(e);
        }
        customerBox.setValue(null);
        treatmentBox.setValue(null);
        professionalBox.getItems().clear();
        dateField.setValue(day);
        timeField.clear();
        fields.clear();
        updateSummary();
        drawer.open();
    }

    /**
     * Opens the panel with an appointment, to move it to another day or time.
     *
     * @param appointment the appointment to move; its customer, treatment and professional stay as they are
     */
    public void openToReschedule(Appointment appointment) {
        rescheduling = Objects.requireNonNull(appointment, "appointment");
        formTitle.setText("Reprogramar cita");
        saveButton.setText("Reprogramar");
        customerBox.getItems().setAll(appointment.getCustomer());
        treatmentBox.getItems().setAll(appointment.getTreatment());
        professionalBox.getItems().setAll(appointment.getProfessional());
        customerBox.setValue(appointment.getCustomer());
        treatmentBox.setValue(appointment.getTreatment());
        professionalBox.setValue(appointment.getProfessional());
        customerBox.setDisable(true);
        treatmentBox.setDisable(true);
        professionalBox.setDisable(true);
        dateField.setValue(appointment.getStart().toLocalDate());
        timeField.setText(DateFormats.time(appointment.getStart().toLocalTime()));
        fields.clear();
        updateSummary();
        drawer.open();
    }

    private static <T> void showing(ComboBox<T> box, Function<T, String> text) {
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(T item) {
                return item == null ? "" : text.apply(item);
            }

            @Override
            public T fromString(String string) {
                return null;
            }
        });
    }

    /** Offers only the professionals who offer the chosen treatment. */
    private void offerProfessionalsOf(Treatment treatment) {
        if (rescheduling != null) {
            return;
        }
        professionalBox.setValue(null);
        try {
            professionalBox.getItems().setAll(treatment == null ? List.of() : professionals.listByTreatment(treatment.getId()));
        } catch (DataAccessException e) {
            alerts.showError(e);
        }
        professionalBox.setPromptText(treatment == null ? "Primero elige el tratamiento" : "Seleccionar profesional...");
        updateSummary();
    }

    /** The day and hour typed, or null while they are missing or not a time. */
    private LocalDateTime chosenStart() {
        try {
            LocalDate day = dateField.getValue();
            return day == null || timeField.getText() == null ? null : day.atTime(LocalTime.parse(timeField.getText().trim()));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Recalculates the summary with what has been chosen; what is still missing shows a dash. */
    private void updateSummary() {
        Treatment treatment = treatmentBox.getValue();
        Professional professional = professionalBox.getValue();
        Customer customer = customerBox.getValue();
        LocalDateTime start = chosenStart();
        endValue.setText(treatment == null || start == null ? NOT_YET
            : DateFormats.time(AppointmentService.calculateEnd(treatment, start).toLocalTime()));
        showAvailability(professional, treatment, start);
        boolean complete = customer != null && professional != null && treatment != null && start != null;
        try {
            depositValue.setText(complete ? CurrencyFormatter.format(depositFor(customer, professional, treatment, start)) : NOT_YET);
            reminderValue.setText(start == null ? NOT_YET
                : DateFormats.dateTime(start.minusHours(settings.reminderHoursBefore())));
            summaryNote.setText(complete ? depositNote(customer) : "Elige cliente, tratamiento, profesional y hora para ver el anticipo.");
        } catch (ValidationException | DataAccessException e) {
            alerts.showError(e);
        }
    }

    private void showAvailability(Professional professional, Treatment treatment, LocalDateTime start) {
        availabilityValue.getStyleClass().removeAll("chip-success", "chip-warning", "stat-value");
        if (professional == null || treatment == null || start == null) {
            availabilityValue.setText(NOT_YET);
            availabilityValue.getStyleClass().add("stat-value");
            return;
        }
        if (rescheduling != null) {
            availabilityValue.setText("Se comprueba al guardar");
            availabilityValue.getStyleClass().add("stat-value");
            return;
        }
        LocalDateTime end = AppointmentService.calculateEnd(treatment, start);
        try {
            boolean free = appointments.checkAvailability(professional, start, end);
            availabilityValue.setText(free ? "Disponible" : AppointmentService.worksDuring(professional, start, end)
                ? "Ya tiene una cita" : "Fuera de su jornada");
            availabilityValue.getStyleClass().add(free ? "chip-success" : "chip-warning");
        } catch (ValidationException | DataAccessException e) {
            alerts.showError(e);
        }
    }

    /** The deposit the rules ask for: the percentage of the price, or all of it for a customer with many no-shows. */
    private BigDecimal depositFor(Customer customer, Professional professional, Treatment treatment, LocalDateTime start)
        throws ValidationException, DataAccessException {
        if (rescheduling != null) {
            return rescheduling.getDeposit();
        }
        return depositRule.requiredDeposit(AppointmentService.newAppointment(0, customer, professional, treatment, start));
    }

    private String depositNote(Customer customer) throws ValidationException, DataAccessException {
        if (rescheduling != null) {
            return "Se mantiene el anticipo que ya pagó.";
        }
        if (customer.getNoShowCount() >= settings.noShowPenaltyThreshold()) {
            return "Paga el 100 % del tratamiento por adelantado: tiene " + customer.getNoShowCount() + " inasistencias.";
        }
        return "Anticipo del " + String.format("%.0f", settings.depositPercentage()) + " % del precio.";
    }

    private void save() {
        fields.clear();
        try {
            Customer customer = customerBox.getValue();
            Validator.requirePresent(customer, "cliente");
            Treatment treatment = treatmentBox.getValue();
            Validator.requirePresent(treatment, "tratamiento");
            Professional professional = professionalBox.getValue();
            Validator.requirePresent(professional, "profesional");
            LocalDateTime start = parseStart();
            Appointment saved;
            if (rescheduling == null) {
                saved = appointments.book(customer, professional, treatment, start,
                    depositFor(customer, professional, treatment, start));
                alerts.toast("Cita agendada.");
            } else {
                saved = appointments.reschedule(rescheduling.getId(), start);
                alerts.toast("Cita reprogramada.");
            }
            drawer.close();
            onSaved.accept(saved.getStart().toLocalDate());
        } catch (ValidationException | BusinessRuleException e) {
            if (!fields.show(e)) {
                alerts.showError(e);
            }
        } catch (DataAccessException e) {
            alerts.showError(e);
        }
    }

    private LocalDateTime parseStart() throws ValidationException {
        Validator.requirePresent(dateField.getValue(), "fecha");
        try {
            return dateField.getValue().atTime(LocalTime.parse(timeField.getText() == null ? "" : timeField.getText().trim()));
        } catch (DateTimeParseException e) {
            throw new ValidationException("La hora debe tener el formato HH:mm, por ejemplo 09:30.", e);
        }
    }
}
