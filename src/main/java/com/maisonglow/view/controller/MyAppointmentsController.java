package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.AppointmentStatus;
import com.maisonglow.model.Commerce.ProfessionalOption;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.CustomerPortalService;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.util.DateFormats;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.BackgroundTask;
import com.maisonglow.view.LoadingOverlay;
import com.maisonglow.view.RefreshableView;
import com.maisonglow.view.ViewLifecycle;
import com.maisonglow.view.appointment.AppointmentLook;
import com.maisonglow.view.appointment.SlotPicker;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Controller of {@code MyAppointmentsView.fxml}: "Mis citas" of a customer (task N-R04). The customer chooses a treatment, a
 * professional that offers it, a day and one of the free hours, and books; the new appointment is theirs and stays pending until
 * the business confirms it on receiving the deposit, because a customer books with a deposit of zero and never pays or confirms
 * here. Their own appointments show with their state and the deposit that is asked for, and can be rescheduled or cancelled
 * while the rules allow it: the service decides (notice period, schedule, conflicts) and its reason is shown. Which hours are
 * free is asked to the service for each candidate hour, in the background. Nothing here chooses a customer: the service takes
 * it from the session. An answer that arrives after the session closed is ignored.
 */
public final class MyAppointmentsController implements ViewLifecycle, RefreshableView {

    /** An appointment of the customer with the deposit asked for it, if it is still open. */
    private record Row(Appointment appointment, BigDecimal requiredDeposit) { }

    private final CustomerPortalService portal;
    private final Clock clock;
    private final SlotPicker bookPicker = new SlotPicker();
    private final SlotPicker reschedulePicker = new SlotPicker();
    private Executor executor = ForkJoinPool.commonPool();
    private AlertPresenter alerts;
    private LoadingOverlay loading;
    private boolean disposed;
    private int bookRequest;
    private int rescheduleRequest;
    private Appointment rescheduling;

    @FXML
    private StackPane host;
    @FXML
    private ComboBox<Treatment> treatmentBox;
    @FXML
    private Label treatmentSummary;
    @FXML
    private ComboBox<ProfessionalOption> professionalBox;
    @FXML
    private DatePicker datePicker;
    @FXML
    private VBox slotsHost;
    @FXML
    private Button bookButton;
    @FXML
    private VBox appointmentsBox;
    @FXML
    private Label appointmentsEmpty;
    @FXML
    private VBox rescheduleCard;
    @FXML
    private Label rescheduleTitle;
    @FXML
    private DatePicker rescheduleDate;
    @FXML
    private VBox rescheduleSlotsHost;
    @FXML
    private Button rescheduleButton;
    @FXML
    private Button rescheduleClose;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public MyAppointmentsController(AppContext context) {
        Objects.requireNonNull(context, "context");
        this.portal = context.portal();
        this.clock = context.clock();
    }

    /**
     * Replaces where the calls run, which the tests use to hold an answer back.
     *
     * @param newExecutor runs the call
     */
    public void useExecutor(Executor newExecutor) {
        this.executor = Objects.requireNonNull(newExecutor, "newExecutor");
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
        alerts = new AlertPresenter(bookButton);
        loading = LoadingOverlay.over(host);
        treatmentBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Treatment treatment) {
                return treatment == null ? "" : treatment.getName();
            }

            @Override
            public Treatment fromString(String text) {
                return null;
            }
        });
        professionalBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ProfessionalOption option) {
                return option == null ? "" : option.name() + " · " + option.specialty();
            }

            @Override
            public ProfessionalOption fromString(String text) {
                return null;
            }
        });
        slotsHost.getChildren().setAll(bookPicker);
        rescheduleSlotsHost.getChildren().setAll(reschedulePicker);
        notBeforeToday(datePicker);
        notBeforeToday(rescheduleDate);
        treatmentBox.valueProperty().addListener((observable, before, treatment) -> chooseTreatment(treatment));
        professionalBox.valueProperty().addListener((observable, before, professional) -> loadBookSlots());
        datePicker.valueProperty().addListener((observable, before, date) -> loadBookSlots());
        bookPicker.onChosen(this::updateBookButton);
        bookButton.setOnAction(event -> book());
        rescheduleDate.valueProperty().addListener((observable, before, date) -> loadRescheduleSlots());
        reschedulePicker.onChosen(() -> rescheduleButton.setDisable(reschedulePicker.selected().isEmpty()));
        rescheduleButton.setOnAction(event -> reschedule());
        rescheduleClose.setOnAction(event -> closeReschedule());
        closeReschedule();
        resetBooking();
        BackgroundTask.run(executor, portal::treatments, treatments -> treatmentBox.getItems().setAll(treatments),
            error -> alerts.showError(error), () -> !disposed);
        refresh();
    }

    /** The days before today cannot be chosen. */
    private void notBeforeToday(DatePicker picker) {
        picker.setDayCellFactory(unused -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now(clock)));
            }
        });
    }

    // ---- Reservar ----

    private void resetBooking() {
        bookRequest++;
        treatmentBox.setValue(null);
        professionalBox.getItems().clear();
        professionalBox.setValue(null);
        professionalBox.setDisable(true);
        datePicker.setValue(null);
        datePicker.setDisable(true);
        treatmentSummary.setText("");
        bookPicker.showHint("Elige un tratamiento para empezar.");
        bookButton.setDisable(true);
    }

    private void chooseTreatment(Treatment treatment) {
        bookRequest++;
        professionalBox.getItems().clear();
        professionalBox.setValue(null);
        professionalBox.setDisable(true);
        datePicker.setValue(null);
        datePicker.setDisable(true);
        bookButton.setDisable(true);
        if (treatment == null) {
            treatmentSummary.setText("");
            bookPicker.showHint("Elige un tratamiento para empezar.");
            return;
        }
        treatmentSummary.setText(treatment.getDurationMinutes() + " min · " + CurrencyFormatter.format(treatment.getPrice()));
        bookPicker.showHint("Elige un profesional.");
        int id = treatment.getId();
        BackgroundTask.run(executor, () -> portal.professionals(id), options -> {
            if (treatmentBox.getValue() != null && treatmentBox.getValue().getId() == id) {
                professionalBox.getItems().setAll(options);
                professionalBox.setDisable(options.isEmpty());
                if (options.isEmpty()) {
                    bookPicker.showHint("Por ahora ningún profesional ofrece este tratamiento.");
                }
            }
        }, error -> alerts.showError(error), () -> !disposed);
    }

    private void loadBookSlots() {
        bookButton.setDisable(true);
        Treatment treatment = treatmentBox.getValue();
        ProfessionalOption professional = professionalBox.getValue();
        datePicker.setDisable(professional == null);
        if (treatment == null || professional == null) {
            return;
        }
        LocalDate date = datePicker.getValue();
        if (date == null) {
            bookPicker.showHint("Elige la fecha para ver los horarios.");
            return;
        }
        int request = ++bookRequest;
        bookPicker.showHint("Buscando horarios…");
        BackgroundTask.run(executor, () -> freeHours(professional.id(), treatment.getId(), date), hours -> {
            if (request == bookRequest) {
                bookPicker.showSlots(hours);
            }
        }, error -> alerts.showError(error), () -> !disposed);
    }

    /** Asks the service, hour by hour, which of the candidate hours of a day are free. */
    private List<LocalTime> freeHours(int professionalId, int treatmentId, LocalDate date)
        throws ValidationException, BusinessRuleException, DataAccessException {
        List<LocalTime> free = new ArrayList<>();
        for (LocalTime hour : SlotPicker.candidates()) {
            if (portal.available(professionalId, treatmentId, LocalDateTime.of(date, hour))) {
                free.add(hour);
            }
        }
        return free;
    }

    private void updateBookButton() {
        bookButton.setDisable(bookPicker.selected().isEmpty());
    }

    private void book() {
        Treatment treatment = treatmentBox.getValue();
        ProfessionalOption professional = professionalBox.getValue();
        LocalDate date = datePicker.getValue();
        if (treatment == null || professional == null || date == null || bookPicker.selected().isEmpty()) {
            return;
        }
        LocalDateTime start = LocalDateTime.of(date, bookPicker.selected().get());
        bookButton.setDisable(true);
        loading.show("Reservando tu cita…");
        BackgroundTask.run(executor, () -> portal.book(professional.id(), treatment.getId(), start), booked -> {
            loading.hide();
            alerts.toast("Cita reservada. Queda pendiente hasta que el negocio la confirme.");
            resetBooking();
            refresh();
        }, error -> {
            loading.hide();
            updateBookButton();
            alerts.showError(error);
            loadBookSlots();
        }, () -> !disposed);
    }

    // ---- Mis citas ----

    /** Reads the appointments again; it is called when the screen is shown, so a change made by the business shows. */
    @Override
    public void refresh() {
        BackgroundTask.run(executor, this::read, rows -> showRows(rows), error -> alerts.showError(error), () -> !disposed);
    }

    private List<Row> read() throws BusinessRuleException, DataAccessException, ValidationException {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Appointment> mine = portal.myAppointments();
        List<Appointment> upcoming = mine.stream().filter(appointment -> isOpen(appointment) && !appointment.getStart().isBefore(now))
            .sorted(Comparator.comparing(Appointment::getStart)).toList();
        List<Appointment> rest = mine.stream().filter(appointment -> !upcoming.contains(appointment))
            .sorted(Comparator.comparing(Appointment::getStart).reversed()).toList();
        List<Row> rows = new ArrayList<>();
        for (Appointment appointment : upcoming) {
            rows.add(new Row(appointment, portal.requiredDeposit(appointment.getId())));
        }
        rest.forEach(appointment -> rows.add(new Row(appointment, null)));
        return rows;
    }

    private static boolean isOpen(Appointment appointment) {
        return appointment.getStatus() == AppointmentStatus.PENDING || appointment.getStatus() == AppointmentStatus.CONFIRMED;
    }

    private void showRows(List<Row> rows) {
        appointmentsBox.getChildren().setAll(rows.stream().map(this::card).toList());
        appointmentsEmpty.setVisible(rows.isEmpty());
        appointmentsEmpty.setManaged(rows.isEmpty());
    }

    private Node card(Row row) {
        Appointment appointment = row.appointment();
        AppointmentLook look = AppointmentLook.of(appointment.getStatus());
        Label what = new Label(appointment.getTreatment().getName());
        what.getStyleClass().add("home-row-title");
        Label chip = new Label(look.label());
        chip.getStyleClass().add(look.chipStyle());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(8, what, spacer, chip);
        header.setAlignment(Pos.CENTER_LEFT);
        Label when = new Label(DateFormats.dateTime(appointment.getStart()) + " · " + appointment.getProfessional().getName());
        when.getStyleClass().add("home-row-detail");
        VBox card = new VBox(4, header, when);
        card.getStyleClass().add("home-row");
        if (row.requiredDeposit() != null) {
            Label deposit = new Label("Anticipo que se pide: " + CurrencyFormatter.format(row.requiredDeposit())
                + " · registrado: " + CurrencyFormatter.format(appointment.getDeposit()) + ". El negocio lo registra al recibirlo.");
            deposit.getStyleClass().add("home-row-detail");
            deposit.setWrapText(true);
            Button change = new Button("Reprogramar");
            change.getStyleClass().addAll("button-secondary", "button-compact");
            change.setOnAction(event -> openReschedule(appointment));
            Button cancel = new Button("Cancelar");
            cancel.getStyleClass().addAll("button-secondary", "button-compact");
            cancel.setOnAction(event -> cancel(appointment));
            card.getChildren().addAll(deposit, new HBox(8, change, cancel));
        }
        return card;
    }

    private void cancel(Appointment appointment) {
        if (!alerts.confirm("¿Cancelar tu cita de " + appointment.getTreatment().getName() + " del "
            + DateFormats.dateTime(appointment.getStart()) + "?")) {
            return;
        }
        loading.show("Cancelando tu cita…");
        BackgroundTask.run(executor, () -> {
            portal.cancelAppointment(appointment.getId());
            return appointment.getId();
        }, done -> {
            loading.hide();
            alerts.toast("Cita cancelada.");
            closeReschedule();
            refresh();
        }, error -> {
            loading.hide();
            alerts.showError(error);
            refresh();
        }, () -> !disposed);
    }

    // ---- Reprogramar ----

    private void openReschedule(Appointment appointment) {
        rescheduling = appointment;
        rescheduleRequest++;
        rescheduleTitle.setText("Reprogramar: " + appointment.getTreatment().getName());
        rescheduleDate.setValue(null);
        reschedulePicker.showHint("Elige la nueva fecha para ver los horarios.");
        rescheduleButton.setDisable(true);
        rescheduleCard.setVisible(true);
        rescheduleCard.setManaged(true);
    }

    private void closeReschedule() {
        rescheduling = null;
        rescheduleRequest++;
        rescheduleCard.setVisible(false);
        rescheduleCard.setManaged(false);
    }

    private void loadRescheduleSlots() {
        rescheduleButton.setDisable(true);
        Appointment appointment = rescheduling;
        LocalDate date = rescheduleDate.getValue();
        if (appointment == null || date == null) {
            return;
        }
        int request = ++rescheduleRequest;
        reschedulePicker.showHint("Buscando horarios…");
        BackgroundTask.run(executor, () -> freeHours(appointment.getProfessional().getId(), appointment.getTreatment().getId(), date),
            hours -> {
                if (request == rescheduleRequest) {
                    reschedulePicker.showSlots(hours);
                }
            }, error -> alerts.showError(error), () -> !disposed);
    }

    private void reschedule() {
        Appointment appointment = rescheduling;
        LocalDate date = rescheduleDate.getValue();
        if (appointment == null || date == null || reschedulePicker.selected().isEmpty()) {
            return;
        }
        LocalDateTime start = LocalDateTime.of(date, reschedulePicker.selected().get());
        rescheduleButton.setDisable(true);
        loading.show("Reprogramando tu cita…");
        BackgroundTask.run(executor, () -> portal.reschedule(appointment.getId(), start), moved -> {
            loading.hide();
            alerts.toast("Cita reprogramada.");
            closeReschedule();
            refresh();
        }, error -> {
            loading.hide();
            rescheduleButton.setDisable(false);
            alerts.showError(error);
            loadRescheduleSlots();
        }, () -> !disposed);
    }

    /** Closing the session: an answer that arrives later is ignored and the appointments shown are dropped. */
    @Override
    public void dispose() {
        disposed = true;
        rescheduling = null;
        appointmentsBox.getChildren().clear();
    }
}
