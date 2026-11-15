package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Appointment;
import com.maisonglow.service.AppointmentService;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.util.DateFormats;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.appointment.AppointmentLook;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.shape.Circle;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;

/**
 * Controller of {@code AppointmentsView.fxml}: the agenda of the day (mockup M5). It shows the appointments of the
 * chosen day in order, with their state, and moves from one day to another. It only talks to
 * {@link AppointmentService}, never to a DAO.
 */
public final class AppointmentsController {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AppointmentService appointments;
    private final Clock clock;
    private final ObservableList<Appointment> shown = FXCollections.observableArrayList();
    private AlertPresenter alerts;

    @FXML
    private Button previousButton;
    @FXML
    private DatePicker dayPicker;
    @FXML
    private Button nextButton;
    @FXML
    private Button todayButton;
    @FXML
    private Label dayTitle;
    @FXML
    private TableView<Appointment> appointmentsTable;
    @FXML
    private TableColumn<Appointment, Appointment> timeColumn;
    @FXML
    private TableColumn<Appointment, String> customerColumn;
    @FXML
    private TableColumn<Appointment, String> treatmentColumn;
    @FXML
    private TableColumn<Appointment, String> professionalColumn;
    @FXML
    private TableColumn<Appointment, Appointment> statusColumn;
    @FXML
    private TableColumn<Appointment, BigDecimal> depositColumn;
    @FXML
    private Label countLabel;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public AppointmentsController(AppContext context) {
        Objects.requireNonNull(context, "context");
        this.appointments = context.appointments();
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
        alerts = new AlertPresenter(appointmentsTable);
        setUpTable();
        setUpDay();
        refresh();
    }

    private void setUpTable() {
        timeColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        timeColumn.setCellFactory(showing(appointment -> DateFormats.time(appointment.getStart().toLocalTime())
            + " - " + DateFormats.time(appointment.getEnd().toLocalTime())));
        customerColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getCustomer().getName()));
        treatmentColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getTreatment().getName()));
        professionalColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getProfessional().getName()));
        statusColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        statusColumn.setCellFactory(column -> new StatusCell());
        depositColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getDeposit()));
        depositColumn.setCellFactory(showing(CurrencyFormatter::format));
        appointmentsTable.setRowFactory(table -> new AppointmentRow());
        appointmentsTable.setItems(shown);
    }

    private void setUpDay() {
        dayPicker.setConverter(new StringConverter<>() {
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
        });
        dayPicker.setValue(LocalDate.now(clock));
        dayPicker.valueProperty().addListener((observable, before, day) -> {
            if (day == null) {
                dayPicker.setValue(before);
            } else {
                refresh();
            }
        });
        previousButton.setOnAction(event -> dayPicker.setValue(dayPicker.getValue().minusDays(1)));
        nextButton.setOnAction(event -> dayPicker.setValue(dayPicker.getValue().plusDays(1)));
        todayButton.setOnAction(event -> dayPicker.setValue(LocalDate.now(clock)));
    }

    /** Reads the appointments of the chosen day again, in order of time. */
    void refresh() {
        LocalDate day = dayPicker.getValue();
        try {
            shown.setAll(appointments.listByDate(day).stream().sorted(Comparator.comparing(Appointment::getStart)).toList());
        } catch (ValidationException | DataAccessException e) {
            alerts.showError(e);
        }
        String title = DateFormats.longDate(day);
        dayTitle.setText(title.substring(0, 1).toUpperCase() + title.substring(1));
        long pending = shown.stream().filter(appointment -> AppointmentService.blocksSchedule(appointment)).count();
        countLabel.setText(shown.size() + (shown.size() == 1 ? " cita" : " citas") + ", " + pending + " por atender");
    }

    private static <T> Callback<TableColumn<Appointment, T>, TableCell<Appointment, T>> showing(Function<T, String> text) {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : text.apply(item));
            }
        };
    }

    /** The state of an appointment as a chip with a dot. */
    private static final class StatusCell extends TableCell<Appointment, Appointment> {
        @Override
        protected void updateItem(Appointment appointment, boolean empty) {
            super.updateItem(appointment, empty);
            if (empty || appointment == null) {
                setGraphic(null);
                return;
            }
            AppointmentLook look = AppointmentLook.of(appointment.getStatus());
            Label chip = new Label(look.label());
            Circle dot = new Circle(3.5);
            dot.getStyleClass().add("chip-dot");
            chip.setGraphic(dot);
            chip.getStyleClass().add(look.chipStyle());
            setGraphic(chip);
        }
    }

    /** A row that warns for a no-show and goes quiet for an appointment that no longer holds the schedule. */
    private static final class AppointmentRow extends TableRow<Appointment> {
        @Override
        protected void updateItem(Appointment appointment, boolean empty) {
            super.updateItem(appointment, empty);
            getStyleClass().removeAll("row-noshow", "row-muted");
            if (empty || appointment == null) {
                return;
            }
            switch (appointment.getStatus()) {
                case NO_SHOW -> getStyleClass().add("row-noshow");
                case CANCELLED, COMPLETED -> getStyleClass().add("row-muted");
                default -> { }
            }
        }
    }
}
