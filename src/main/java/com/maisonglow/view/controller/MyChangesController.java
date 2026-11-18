package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Commerce.Change;
import com.maisonglow.model.Commerce.ChangeKind;
import com.maisonglow.service.ChangeHistoryService;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.BackgroundTask;
import com.maisonglow.view.FieldValidator;
import com.maisonglow.view.LoadingOverlay;
import com.maisonglow.view.RefreshableView;
import com.maisonglow.view.ViewLifecycle;
import com.maisonglow.view.ViewLoader;
import com.maisonglow.view.history.ChangeCards;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/**
 * Controller of {@code MyChangesView.fxml}: "Mis cambios" of a customer (task N-R05). Their own history of changes of image:
 * a timeline with the notes and the photos before and after, a form to add a personal change (with photos taken by the camera
 * or chosen from a file, both optional), the way to take the photos of a record away, and the switch that lets the business
 * consult their photos and history. By default nobody of the business can see anything; the customer authorizes it and can
 * revoke it at any time. Everything comes from the account of the session through {@link ChangeHistoryService}; a simulation
 * saved from the virtual try-on appears here marked as a simulation, never as a real result. The data is read in the
 * background, the screen reads it again every time it is shown, and an answer that arrives after the session closed is ignored.
 */
public final class MyChangesController implements ViewLifecycle, RefreshableView {

    /** What the screen shows, read in one go. */
    private record Loaded(List<Change> changes, boolean staffAccess) { }

    private final ChangeHistoryService changes;
    private final AppContext context;
    private final Clock clock;
    private final FieldValidator fields = new FieldValidator();
    private Executor executor = ForkJoinPool.commonPool();
    private AlertPresenter alerts;
    private LoadingOverlay loading;
    private PhotoPickerController beforePicker;
    private PhotoPickerController afterPicker;
    private Path beforePhoto;
    private Path afterPhoto;
    private boolean disposed;
    private boolean reading;

    @FXML
    private StackPane host;
    @FXML
    private VBox timelineBox;
    @FXML
    private Label emptyLabel;
    @FXML
    private CheckBox staffAccessBox;
    @FXML
    private Label privacyNote;
    @FXML
    private DatePicker datePicker;
    @FXML
    private Label dateMessage;
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
    public MyChangesController(AppContext context) {
        this.context = Objects.requireNonNull(context, "context");
        this.changes = context.changes();
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
        alerts = new AlertPresenter(saveButton);
        loading = LoadingOverlay.over(host);
        fields.field(datePicker, dateMessage, "fecha");
        fields.field(notesArea, notesMessage, "descripción", "notas");
        datePicker.setValue(LocalDate.now(clock));
        datePicker.setDayCellFactory(unused -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now(clock)));
            }
        });
        beforePicker = addPicker(beforeBox, photo -> beforePhoto = photo);
        afterPicker = addPicker(afterBox, photo -> afterPhoto = photo);
        saveButton.setOnAction(event -> save());
        staffAccessBox.setOnAction(event -> changeConsent(staffAccessBox.isSelected()));
        showConsent(false);
        refresh();
    }

    private PhotoPickerController addPicker(VBox hostBox, Consumer<Path> keep) {
        ViewLoader.Loaded<PhotoPickerController> loaded = ViewLoader.loadWithController(
            MyChangesController.class.getResource("/com/maisonglow/view/PhotoPickerPane.fxml"), context);
        PhotoPickerController picker = loaded.controller();
        picker.onUse(photo -> {
            keep.accept(photo);
            picker.showCurrent(photo);
        });
        hostBox.getChildren().setAll(loaded.root());
        return picker;
    }

    // ---- Leer ----

    /** Reads the history and the consent again; it is called when the screen is shown. */
    @Override
    public void refresh() {
        if (reading) {
            return;
        }
        reading = true;
        BackgroundTask.run(executor, () -> new Loaded(changes.mine(), changes.staffAccessAllowed()), loaded -> {
            reading = false;
            show(loaded);
        }, error -> {
            reading = false;
            alerts.showError(error);
        }, () -> !disposed);
    }

    private void show(Loaded loaded) {
        showConsent(loaded.staffAccess());
        List<Node> cards = loaded.changes().stream().map(change -> ChangeCards.card(change, changes, this::removePhotos)).toList();
        if (cards.isEmpty()) {
            timelineBox.getChildren().setAll(emptyLabel);
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
        } else {
            timelineBox.getChildren().setAll(cards);
        }
    }

    // ---- Privacidad ----

    private void showConsent(boolean allowed) {
        staffAccessBox.setSelected(allowed);
        privacyNote.setText(allowed
            ? "El negocio puede consultar tus fotos y tu historial. Puedes quitarle el permiso cuando quieras."
            : "Por ahora nadie del negocio puede ver tus fotos ni tu historial.");
    }

    private void changeConsent(boolean allowed) {
        BackgroundTask.run(executor, () -> {
            changes.allowStaffAccess(allowed);
            return allowed;
        }, done -> {
            showConsent(done);
            alerts.toast(done ? "El negocio ya puede consultar tu historial." : "El negocio ya no puede consultar tu historial.");
        }, error -> {
            showConsent(!allowed);
            alerts.showError(error);
        }, () -> !disposed);
    }

    // ---- Añadir ----

    private void save() {
        fields.clear();
        LocalDate date = datePicker.getValue();
        String notes = notesArea.getText();
        if (date == null) {
            dateMessage.setText("Elige la fecha del cambio.");
            dateMessage.setVisible(true);
            dateMessage.setManaged(true);
            return;
        }
        if (notes == null || notes.isBlank()) {
            notesMessage.setText("Cuéntanos qué cambiaste.");
            notesMessage.setVisible(true);
            notesMessage.setManaged(true);
            return;
        }
        Path before = beforePhoto;
        Path after = afterPhoto;
        saveButton.setDisable(true);
        loading.show("Guardando tu cambio…");
        BackgroundTask.run(executor, () -> changes.addOwn(ChangeKind.PERSONAL, notes, date, before, after), saved -> {
            loading.hide();
            saveButton.setDisable(false);
            alerts.toast("Cambio guardado.");
            clearForm();
            refresh();
        }, error -> {
            loading.hide();
            saveButton.setDisable(false);
            if (!((error instanceof ValidationException || error instanceof BusinessRuleException) && fields.show(error))) {
                alerts.showError(error);
            }
        }, () -> !disposed);
    }

    private void clearForm() {
        notesArea.clear();
        datePicker.setValue(LocalDate.now(clock));
        beforePhoto = null;
        afterPhoto = null;
        beforePicker.showCurrent(null);
        afterPicker.showCurrent(null);
    }

    private void removePhotos(Change change) {
        if (!alerts.confirm("¿Retirar las fotos de este cambio? Tus notas se conservan.")) {
            return;
        }
        loading.show("Retirando las fotos…");
        BackgroundTask.run(executor, () -> {
            changes.removePhotos(change.id());
            return change.id();
        }, done -> {
            loading.hide();
            alerts.toast("Fotos retiradas.");
            refresh();
        }, error -> {
            loading.hide();
            alerts.showError(error);
        }, () -> !disposed);
    }

    /** Closing the session: an answer that arrives later is ignored and the history, the photos chosen and the form are dropped. */
    @Override
    public void dispose() {
        disposed = true;
        beforePhoto = null;
        afterPhoto = null;
        timelineBox.getChildren().clear();
        notesArea.clear();
        if (beforePicker != null) {
            beforePicker.dispose();
        }
        if (afterPicker != null) {
            afterPicker.dispose();
        }
    }
}
