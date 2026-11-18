package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiModule;
import com.maisonglow.ai.HairTones;
import com.maisonglow.ai.TryOnRequest;
import com.maisonglow.ai.TryOnResult;
import com.maisonglow.model.Commerce.ChangeKind;
import com.maisonglow.service.ChangeHistoryService;
import com.maisonglow.service.CustomerPortalService;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.BackgroundTask;
import com.maisonglow.view.LoadingOverlay;
import com.maisonglow.view.ViewLifecycle;
import com.maisonglow.view.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Controller of {@code MyTryOnView.fxml}: the virtual try-on of a customer (task N-R05). It always works on the account of the
 * session: the customer is never chosen on screen, and the service refuses a request for anyone else. The customer takes or
 * uploads a photo, chooses a tone and an intensity, and the result is shown with the notice that it is a simulation and not a
 * guarantee. Nothing is kept on its own: "Guardar en Mis cambios" is an action of the customer and keeps the photo and the
 * result as a SIMULATION record, never as a real change. Changing the photo, the tone or the intensity discards the result
 * shown, so a result is never saved against other choices. The try-on runs in the background; an answer that arrives after the
 * session closed is ignored, and the mode notice is asked again after every request.
 */
public final class MyTryOnController implements ViewLifecycle {

    private final AppContext context;
    private final AiModule ai;
    private final CustomerPortalService portal;
    private final ChangeHistoryService changes;
    private final Clock clock;
    private final ToggleGroup tones = new ToggleGroup();
    private Executor executor = ForkJoinPool.commonPool();
    private AlertPresenter alerts;
    private LoadingOverlay loading;
    private PhotoPickerController picker;
    private Path photo;
    private TryOnResult result;
    private String resultTone;
    private double resultIntensity;
    private boolean disposed;

    @FXML
    private HBox simulatedBanner;
    @FXML
    private VBox photoBox;
    @FXML
    private Label needPhotoMessage;
    @FXML
    private TilePane tonesPane;
    @FXML
    private Label selectedToneLabel;
    @FXML
    private Label toneMessage;
    @FXML
    private Slider intensitySlider;
    @FXML
    private Label intensityValue;
    @FXML
    private ImageView resultView;
    @FXML
    private Label resultPlaceholder;
    @FXML
    private Label disclaimerLabel;
    @FXML
    private Button generateButton;
    @FXML
    private Button saveButton;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public MyTryOnController(AppContext context) {
        this.context = Objects.requireNonNull(context, "context");
        this.ai = context.ai();
        this.portal = context.portal();
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

    /** @return the photo panel, which the tests drive without opening a camera or a file window */
    PhotoPickerController photoPicker() {
        return picker;
    }

    @FXML
    private void initialize() {
        alerts = new AlertPresenter(generateButton);
        loading = LoadingOverlay.over((StackPane) resultView.getParent());
        ViewLoader.Loaded<PhotoPickerController> loaded = ViewLoader.loadWithController(
            MyTryOnController.class.getResource("/com/maisonglow/view/PhotoPickerPane.fxml"), context);
        picker = loaded.controller();
        picker.onUse(chosen -> {
            photo = chosen;
            picker.showCurrent(chosen);
            invalidateResult();
        });
        photoBox.getChildren().setAll(loaded.root());
        tonesPane.getChildren().setAll(HairTones.ALL.stream().map(this::toneChip).toList());
        tones.selectedToggleProperty().addListener((observable, before, now) -> {
            selectedToneLabel.setText(now == null ? "Selecciona una tonalidad" : ((ToggleButton) now).getText());
            invalidateResult();
        });
        intensitySlider.valueProperty().addListener((observable, before, now) -> invalidateResult());
        intensityValue.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
            () -> Math.round(intensitySlider.getValue() * 100) + "%", intensitySlider.valueProperty()));
        generateButton.setOnAction(event -> generate());
        saveButton.setOnAction(event -> save());
        hide(needPhotoMessage);
        hide(toneMessage);
        showMode();
        invalidateResult();
    }

    private ToggleButton toneChip(HairTones.Tone tone) {
        String name = tone.name();
        ToggleButton chip = new ToggleButton(name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1));
        chip.getStyleClass().add("tone-chip");
        Circle swatch = new Circle(22);
        swatch.setFill(new ImagePattern(new Image(MyTryOnController.class.getResource(
            "/com/maisonglow/view/images/tones/" + tone.swatch() + ".png").toExternalForm())));
        chip.setGraphic(swatch);
        chip.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        chip.setTooltip(new Tooltip(chip.getText()));
        chip.setAccessibleText(chip.getText());
        chip.setUserData(name);
        chip.setToggleGroup(tones);
        return chip;
    }

    /** Shows the notice only while the try-on really is the simulated one; it is asked again after every request. */
    private void showMode() {
        boolean simulated = ai.isSimulated();
        simulatedBanner.setVisible(simulated);
        simulatedBanner.setManaged(simulated);
    }

    // ---- Generar ----

    private void generate() {
        hide(needPhotoMessage);
        hide(toneMessage);
        if (photo == null) {
            show(needPhotoMessage, "Sube o toma una foto para empezar.");
            return;
        }
        if (tones.getSelectedToggle() == null) {
            show(toneMessage, "Elige un tono.");
            return;
        }
        String tone = (String) tones.getSelectedToggle().getUserData();
        double intensity = intensitySlider.getValue();
        Path chosen = photo;
        generateButton.setDisable(true);
        loading.show("Generando tu simulación…");
        BackgroundTask.run(executor, () -> ai.tryOn().generate(new TryOnRequest(portal.profile().getId(), chosen, tone, intensity)),
            generated -> {
                loading.hide();
                generateButton.setDisable(false);
                showResult(generated, tone, intensity);
                showMode();
            }, error -> {
                loading.hide();
                generateButton.setDisable(false);
                showMode();
                alerts.showError(error);
            }, () -> !disposed);
    }

    private void showResult(TryOnResult generated, String tone, double intensity) {
        result = generated;
        resultTone = tone;
        resultIntensity = intensity;
        Image image = new Image(generated.image().toUri().toString());
        resultView.setImage(image.isError() ? null : image);
        resultPlaceholder.setVisible(image.isError());
        disclaimerLabel.setText(generated.disclaimer().substring(0, 1).toUpperCase(Locale.ROOT) + generated.disclaimer().substring(1));
        saveButton.setDisable(false);
    }

    /** Discards the result: it was made with other choices, so it could not be saved as if it were of these. */
    private void invalidateResult() {
        result = null;
        resultView.setImage(null);
        resultPlaceholder.setVisible(true);
        disclaimerLabel.setText("Simulación, no garantía");
        saveButton.setDisable(true);
    }

    // ---- Guardar ----

    /** An action of the customer: keeps the photo and the result as a simulation of their own history. */
    private void save() {
        if (result == null || photo == null) {
            return;
        }
        TryOnResult toKeep = result;
        Path source = photo;
        String notes = "Simulación del tono «" + resultTone + "» al " + Math.round(resultIntensity * 100) + " % de intensidad.";
        saveButton.setDisable(true);
        loading.show("Guardando en Mis cambios…");
        BackgroundTask.run(executor, () -> changes.addOwn(ChangeKind.SIMULATION, notes, LocalDate.now(clock), source, toKeep.image()),
            saved -> {
                loading.hide();
                saveButton.setDisable(false);
                alerts.toast("Guardada en Mis cambios como simulación.");
            }, error -> {
                loading.hide();
                saveButton.setDisable(false);
                alerts.showError(error);
            }, () -> !disposed);
    }

    private static void show(Label label, String text) {
        label.setText(text);
        label.setVisible(true);
        label.setManaged(true);
    }

    private static void hide(Node node) {
        node.setVisible(false);
        node.setManaged(false);
    }

    /** Closing the session: an answer that arrives later is ignored, and the photo and the result are dropped. */
    @Override
    public void dispose() {
        disposed = true;
        photo = null;
        result = null;
        resultView.setImage(null);
        if (picker != null) {
            picker.dispose();
        }
    }

    /** @return the result shown, for the tests */
    TryOnResult result() {
        return result;
    }
}
