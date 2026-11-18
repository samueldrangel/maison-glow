package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.AuthorizationException;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.AppointmentStatus;
import com.maisonglow.model.Commerce.Change;
import com.maisonglow.model.Customer;
import com.maisonglow.service.AppointmentService;
import com.maisonglow.service.ChangeHistoryService;
import com.maisonglow.service.CustomerService;
import com.maisonglow.util.DateFormats;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.BackgroundTask;
import com.maisonglow.view.FieldValidator;
import com.maisonglow.view.LoadingOverlay;
import com.maisonglow.view.ViewLifecycle;
import com.maisonglow.view.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Controller of {@code HistoryView.fxml}: the history of changes of a customer, for the administrator (task I-R05).
 * The administrator chooses a customer; if the customer authorized the business to consult their photos and history the
 * timeline appears, with the photos, and the attention of a completed appointment can be registered with notes and
 * before-and-after photos. Without the authorization nothing is shown and the reason is explained: the service enforces
 * it on every call, the screen only reflects it. Every call runs in the background with a loading veil, and an answer
 * that arrives after the screen was discarded is ignored.
 */
public final class HistoryController implements ViewLifecycle {

    /** What a customer's history gives back: the timeline and the completed appointments, or why they cannot be seen. */
    private record Loaded(int customerId, List<Change> changes, List<Appointment> completed, String blocked) { }

    private final ChangeHistoryService changes;
    private final CustomerService customers;
    private final AppointmentService appointments;
    private final AppContext context;
    private final Clock clock;
    private final FieldValidator fields = new FieldValidator();
    private Executor executor = ForkJoinPool.commonPool();
    private AlertPresenter alerts;
    private LoadingOverlay loading;
    private boolean disposed;
    private Customer current;
    private PhotoPickerController beforePicker;
    private PhotoPickerController afterPicker;
    private Path beforePhoto;
    private Path afterPhoto;

    @FXML
    private ComboBox<Customer> customerBox;
    @FXML
    private HBox authorizationBanner;
    @FXML
    private Label authorizationText;
    @FXML
    private StackPane contentHost;
    @FXML
    private VBox timelineBox;
    @FXML
    private Label emptyLabel;
    @FXML
    private VBox registerCard;
    @FXML
    private ComboBox<Appointment> appointmentBox;
    @FXML
    private Label appointmentMessage;
    @FXML
    private TextArea notesArea;
    @FXML
    private Label notesMessage;
    @FXML
    private VBox beforeBox;
    @FXML
    private VBox afterBox;
    @FXML
    private Button saveButton;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public HistoryController(AppContext context) {
        this.context = Objects.requireNonNull(context, "context");
        this.changes = context.changes();
        this.customers = context.customers();
        this.appointments = context.appointments();
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

    /** @return the panel of the photo before, which the tests drive without opening a camera or a file window */
    PhotoPickerController beforePicker() {
        return beforePicker;
    }

    /** @return the panel of the photo after, which the tests drive without opening a camera or a file window */
    PhotoPickerController afterPicker() {
        return afterPicker;
    }

    @FXML
    private void initialize() {
        alerts = new AlertPresenter(customerBox);
        loading = LoadingOverlay.over(contentHost);
        customerBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Customer customer) {
                return customer == null ? "" : customer.getName() + " · " + customer.getDocument();
            }

            @Override
            public Customer fromString(String text) {
                return null;
            }
        });
        appointmentBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Appointment appointment) {
                return appointment == null ? "" : DateFormats.dateTime(appointment.getStart()) + " · " + appointment.getTreatment().getName();
            }

            @Override
            public Appointment fromString(String text) {
                return null;
            }
        });
        fields.field(appointmentBox, appointmentMessage, "cita");
        fields.field(notesArea, notesMessage, "descripción", "notas");
        beforePicker = addPicker(beforeBox, photo -> beforePhoto = photo);
        afterPicker = addPicker(afterBox, photo -> afterPhoto = photo);
        saveButton.setOnAction(event -> save());
        customerBox.valueProperty().addListener((observable, before, customer) -> choose(customer));
        try {
            customerBox.getItems().setAll(customers.listAll());
        } catch (DataAccessException e) {
            alerts.showError(e);
        }
        showIdle("Elige un cliente para ver su historial.");
    }

    /** Puts a photo panel in a card; a photo used there is kept to be sent with the record, and shown as chosen. */
    private PhotoPickerController addPicker(VBox host, java.util.function.Consumer<Path> keep) {
        ViewLoader.Loaded<PhotoPickerController> loaded = ViewLoader.loadWithController(
            HistoryController.class.getResource("/com/maisonglow/view/PhotoPickerPane.fxml"), context);
        PhotoPickerController picker = loaded.controller();
        picker.onUse(photo -> {
            keep.accept(photo);
            picker.showCurrent(photo);
        });
        host.getChildren().setAll(loaded.root());
        return picker;
    }

    private void choose(Customer customer) {
        current = customer;
        if (customer == null) {
            showIdle("Elige un cliente para ver su historial.");
            return;
        }
        reload();
    }

    /** Reads the history of the chosen customer; the service says no if the customer did not authorize it. */
    private void reload() {
        Customer customer = current;
        if (customer == null) {
            return;
        }
        loading.show("Cargando el historial…");
        BackgroundTask.run(executor, () -> read(customer), loaded -> {
            loading.hide();
            if (current != null && current.getId() == loaded.customerId()) {
                show(loaded);
            }
        }, this::fail, () -> !disposed);
    }

    private Loaded read(Customer customer) throws BusinessRuleException, DataAccessException {
        try {
            List<Change> timeline = changes.forCustomer(customer.getId());
            LocalDate today = LocalDate.now(clock);
            List<Appointment> completed = appointments.listBetween(today.minusYears(1), today).stream()
                .filter(appointment -> appointment.getCustomer().getId() == customer.getId())
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.COMPLETED).toList();
            return new Loaded(customer.getId(), timeline, completed, null);
        } catch (AuthorizationException e) {
            return new Loaded(customer.getId(), List.of(), List.of(), e.getMessage());
        } catch (ValidationException e) {
            throw new BusinessRuleException(e.getMessage(), e);
        }
    }

    private void show(Loaded loaded) {
        boolean blocked = loaded.blocked() != null;
        authorizationBanner.setVisible(blocked);
        authorizationBanner.setManaged(blocked);
        registerCard.setDisable(blocked);
        if (blocked) {
            authorizationText.setText(loaded.blocked());
            showIdle("No se puede mostrar el historial de este cliente.");
            return;
        }
        appointmentBox.getItems().setAll(loaded.completed());
        List<Node> cards = new ArrayList<>(loaded.changes().stream().map(this::card).toList());
        if (cards.isEmpty()) {
            showIdle("Este cliente todavía no tiene cambios registrados.");
        } else {
            timelineBox.getChildren().setAll(cards);
        }
    }

    private void showIdle(String text) {
        emptyLabel.setText(text);
        timelineBox.getChildren().setAll(emptyLabel);
        if (current == null) {
            authorizationBanner.setVisible(false);
            authorizationBanner.setManaged(false);
            registerCard.setDisable(true);
            appointmentBox.getItems().clear();
        }
    }

    /** One record of the timeline: its date, what kind it is, the notes and the photos with a way to remove them. */
    private Node card(Change change) {
        Label date = new Label(DateFormats.date(change.happenedOn()));
        date.getStyleClass().add("stat-value");
        Label kind = new Label(switch (change.kind()) {
            case SERVICE -> "Servicio del negocio";
            case PERSONAL -> "Cambio personal";
            case SIMULATION -> "Simulación";
        });
        kind.getStyleClass().add(switch (change.kind()) {
            case SERVICE -> "chip-gold";
            case PERSONAL -> "chip-rose";
            case SIMULATION -> "chip-neutral";
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox header = new HBox(8, date, kind, spacer);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label notes = new Label(change.notes());
        notes.getStyleClass().add("form-hint");
        notes.setWrapText(true);
        VBox card = new VBox(8, header, notes);
        card.getStyleClass().addAll("card", "form-card");
        HBox photos = new HBox(10);
        addPhoto(photos, change, false, change.sourceKey() == null ? null : "Antes");
        addPhoto(photos, change, true, change.resultKey() == null ? null : "Después");
        if (!photos.getChildren().isEmpty()) {
            card.getChildren().add(photos);
            Button remove = new Button("Retirar fotos");
            remove.getStyleClass().addAll("button-secondary", "button-compact");
            remove.setOnAction(event -> removePhotos(change));
            card.getChildren().add(remove);
        }
        return card;
    }

    private void addPhoto(HBox photos, Change change, boolean result, String caption) {
        if (caption == null) {
            return;
        }
        try {
            Path file = changes.photo(change.id(), result);
            Image image = new Image(file.toUri().toString(), 180, 140, true, true);
            if (image.isError()) {
                return;
            }
            ImageView view = new ImageView(image);
            view.setPreserveRatio(true);
            Label label = new Label(caption);
            label.getStyleClass().add("stat-caption");
            photos.getChildren().add(new VBox(4, view, label));
        } catch (BusinessRuleException | DataAccessException e) {
            // A photo that is gone from the disk is simply not shown; the record and its notes stay.
        }
    }

    private void removePhotos(Change change) {
        if (!alerts.confirm("¿Retirar las fotos de este registro? Las notas y el registro se conservan.")) {
            return;
        }
        loading.show("Retirando las fotos…");
        BackgroundTask.run(executor, () -> {
            changes.removePhotos(change.id());
            return change.id();
        }, done -> {
            loading.hide();
            alerts.toast("Fotos retiradas.");
            reload();
        }, this::fail, () -> !disposed);
    }

    /** Registers the attention of the chosen completed appointment, with the photos chosen, if any. */
    private void save() {
        fields.clear();
        Appointment appointment = appointmentBox.getValue();
        String notes = notesArea.getText();
        if (appointment == null) {
            appointmentMessage.setText("Elige la cita completada que se atendió.");
            appointmentMessage.setVisible(true);
            appointmentMessage.setManaged(true);
            return;
        }
        if (notes == null || notes.isBlank()) {
            notesMessage.setText("Escribe las notas del servicio.");
            notesMessage.setVisible(true);
            notesMessage.setManaged(true);
            return;
        }
        int id = appointment.getId();
        Path before = beforePhoto;
        Path after = afterPhoto;
        loading.show("Guardando el registro…");
        BackgroundTask.run(executor, () -> changes.addService(id, notes, before, after), saved -> {
            loading.hide();
            alerts.toast("Registro guardado.");
            clearForm();
            reload();
        }, error -> {
            loading.hide();
            if (!((error instanceof ValidationException || error instanceof BusinessRuleException) && fields.show(error))) {
                alerts.showError(error);
            }
        }, () -> !disposed);
    }

    private void clearForm() {
        notesArea.clear();
        appointmentBox.setValue(null);
        beforePhoto = null;
        afterPhoto = null;
        beforePicker.showCurrent(null);
        afterPicker.showCurrent(null);
    }

    private void fail(Exception error) {
        loading.hide();
        alerts.showError(error);
    }

    /** Closing the session: an answer that arrives later is ignored and the history and the photos chosen are dropped. */
    @Override
    public void dispose() {
        disposed = true;
        current = null;
        beforePhoto = null;
        afterPhoto = null;
        timelineBox.getChildren().clear();
        if (beforePicker != null) {
            beforePicker.dispose();
        }
        if (afterPicker != null) {
            afterPicker.dispose();
        }
    }
}
