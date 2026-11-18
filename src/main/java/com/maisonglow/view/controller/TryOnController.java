package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.ai.TryOnRequest;
import com.maisonglow.ai.TryOnResult;
import com.maisonglow.ai.TryOnService;
import com.maisonglow.ai.HairTones;
import com.maisonglow.service.CustomerService;
import com.maisonglow.view.RefreshableView;
import com.maisonglow.view.camera.CameraCaptureDialog;
import com.maisonglow.view.camera.WebcamPhotoCamera;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Customer;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.ViewLifecycle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.ImagePattern;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Controller of {@code TryOnView.fxml}: the virtual try-on (mockup M8). The user chooses a customer, a reference
 * photo and a hair tone, and the screen shows the result with the notice that it is a simulation and not a
 * guarantee. A request with no customer, no photo or no tone is rejected by the service and the reason is shown under
 * the field it is about. The result can take a while, so it is asked in the background.
 */
public final class TryOnController implements RefreshableView, ViewLifecycle {

    private final TryOnService tryOn;
    private final boolean simulated;
    private final CustomerService customers;
    private List<Customer> customerList = List.of();
    private final ToggleGroup tones = new ToggleGroup();
    private Executor executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "probador");
        thread.setDaemon(true);
        return thread;
    });
    private Supplier<Path> photoPicker = () -> choose(false);
    private Supplier<Path> cameraPicker;
    private Supplier<Path> savePicker = () -> choose(true);
    private AlertPresenter alerts;
    private boolean disposed;
    private Path photo;
    private Path result;

    @FXML
    private HBox simulatedBanner;
    @FXML
    private ComboBox<Customer> customerBox;
    @FXML
    private Label customerMessage;
    @FXML
    private ImageView photoView;
    @FXML
    private Label photoPlaceholder;
    @FXML
    private Button uploadButton;
    @FXML private Button cameraButton;
    @FXML
    private Label photoMessage;
    @FXML
    private TilePane transformationChips;
    @FXML private Label selectedToneLabel;
    @FXML private Slider intensitySlider;
    @FXML private Label intensityValue;
    @FXML private StackPane photoFrame;
    @FXML private StackPane resultFrame;
    private final ImageView beforeView = new ImageView();
    @FXML private HBox comparisonBadges;
    @FXML private Slider comparisonSlider;
    @FXML private Label comparisonHint;
    @FXML
    private Label transformationMessage;
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
    public TryOnController(AppContext context) {
        Objects.requireNonNull(context, "context");
        this.tryOn = context.ai().tryOn();
        this.simulated = context.ai().isSimulated();
        this.customers = context.customers();
        cameraPicker = () -> new CameraCaptureDialog(new WebcamPhotoCamera(), context.dataFolder().resolve("captures"))
            .show(cameraButton.getScene() == null ? null : cameraButton.getScene().getWindow());
    }

    /**
     * Replaces where the try-on is asked, which the tests use to answer on the spot.
     *
     * @param newExecutor runs the request to the service
     */
    public void useExecutor(Executor newExecutor) {
        this.executor = Objects.requireNonNull(newExecutor, "newExecutor");
    }

    /**
     * Replaces the file dialogs, which the tests use so that no real window is opened.
     *
     * @param photos gives the photo chosen, or null if the user did not choose one
     * @param saves  gives where to save the result, or null if the user did not choose
     */
    public void useFilePickers(Supplier<Path> photos, Supplier<Path> saves) {
        this.photoPicker = Objects.requireNonNull(photos, "photos");
        this.savePicker = Objects.requireNonNull(saves, "saves");
    }

    /** Replaces camera acquisition for tests without using a physical camera. */
    public void useCameraPicker(Supplier<Path> picker) {
        cameraPicker = Objects.requireNonNull(picker, "picker");
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
        alerts = new AlertPresenter(generateButton);
        simulatedBanner.setVisible(simulated);
        simulatedBanner.setManaged(simulated);
        customerBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Customer customer) {
                return customer == null ? "" : customer.getName() + " · " + customer.getDocument();
            }

            @Override
            public Customer fromString(String text) {
                return customerList.stream().filter(c -> toString(c).equals(text) || c.getDocument().equals(text)).findFirst().orElse(null);
            }
        });
        customerBox.setOnShowing(event -> refresh());
        customerBox.getEditor().setOnKeyReleased(event -> {
            if (event.getCode().isNavigationKey() || event.getCode() == javafx.scene.input.KeyCode.ENTER || event.getCode() == javafx.scene.input.KeyCode.ESCAPE) return;
            String typed = customerBox.getEditor().getText();
            String wanted = normalized(typed);
            if (!customerBox.isShowing()) customerBox.show();
            customerBox.getItems().setAll(customerList.stream().filter(c -> normalized(c.getName() + " " + c.getDocument()).contains(wanted)).toList());
            customerBox.getEditor().setText(typed);
            customerBox.getEditor().positionCaret(typed.length());
        });
        transformationChips.getChildren().setAll(HairTones.ALL.stream().map(this::toneChip).toList());
        intensityValue.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
            () -> Math.round(intensitySlider.getValue() * 100) + "%", intensitySlider.valueProperty()));
        fit(photoView, photoFrame);
        fit(resultView, resultFrame);
        beforeView.setPreserveRatio(true);
        beforeView.setId("beforeView");
        fit(beforeView, resultFrame);
        Rectangle beforeClip = new Rectangle();
        beforeClip.widthProperty().bind(resultFrame.widthProperty().multiply(comparisonSlider.valueProperty()));
        beforeClip.heightProperty().bind(resultFrame.heightProperty());
        // Clip a full-size overlay so the dividing line stays in the frame's coordinates.
        StackPane beforeLayer = new StackPane(beforeView);
        beforeLayer.setMouseTransparent(true);
        beforeLayer.setClip(beforeClip);
        resultFrame.getChildren().add(1, beforeLayer);
        clearMessages();
        saveButton.setDisable(true);
        uploadButton.setOnAction(event -> chooseThePhoto());
        cameraButton.setOnAction(event -> {
            clearMessages();
            try { loadPhoto(cameraPicker.get()); }
            catch (RuntimeException e) { show(photoMessage, "No se pudo abrir la cámara. Inténtalo de nuevo o usa Subir foto."); }
        });
        generateButton.setOnAction(event -> generate());
        saveButton.setOnAction(event -> saveTheResult());
        refresh();
        intensitySlider.valueProperty().addListener((o, before, value) -> invalidateResult());
        customerBox.valueProperty().addListener((o, before, value) -> {
            if (before != null && (value == null || before.getId() != value.getId())) invalidateResult();
        });
    }

    /** Reloads live customers while preserving the selected customer, photo and tone. */
    @Override public void refresh() {
        Customer selected = customerBox.getValue();
        try {
            customerList = customers.listAll();
            customerBox.getItems().setAll(customerList);
            customerBox.setValue(selected == null ? null : customerList.stream().filter(c -> c.getId() == selected.getId()).findFirst().orElse(null));
            customerBox.setPromptText(customerList.isEmpty() ? "Registra un cliente en Clientes" : "Buscar cliente...");
        } catch (DataAccessException e) { show(customerMessage, "No se pudo cargar la lista de clientes. Vuelve a intentarlo."); }
    }

    private static String normalized(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static void fit(ImageView image, StackPane frame) {
        image.fitWidthProperty().bind(frame.widthProperty().subtract(16));
        image.fitHeightProperty().bind(frame.heightProperty().subtract(16));
    }

    private ToggleButton toneChip(HairTones.Tone tone) {
        String name = tone.name();
        ToggleButton chip = new ToggleButton(name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1));
        chip.getStyleClass().add("tone-chip");
        Circle swatch = new Circle(22);
        swatch.setFill(new ImagePattern(new Image(TryOnController.class.getResource("/com/maisonglow/view/images/tones/" + tone.swatch() + ".png").toExternalForm())));
        chip.setGraphic(swatch);
        chip.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        chip.setTooltip(new Tooltip(chip.getText()));
        chip.setAccessibleText(chip.getText());
        chip.setOnAction(event -> {
            selectedToneLabel.setText(tones.getSelectedToggle() == null ? "Selecciona una tonalidad" : chip.getText());
            invalidateResult();
        });
        chip.setUserData(name);
        chip.setToggleGroup(tones);
        return chip;
    }

    private void chooseThePhoto() {
        clearMessages();
        loadPhoto(photoPicker.get());
    }

    private void loadPhoto(Path chosen) {
        if (chosen == null) {
            return;
        }
        invalidateResult();
        Image preview = new Image(chosen.toUri().toString(), 600, 680, true, true);
        if (preview.isError()) {
            photo = null;
            photoView.setImage(null);
            photoPlaceholder.setVisible(true);
            show(photoMessage, "No se pudo leer la foto seleccionada. Elige una imagen PNG o JPEG.");
            return;
        }
        photo = chosen;
        photoView.setImage(preview);
        photoPlaceholder.setVisible(false);
    }

    private void invalidateResult() {
        result = null;
        resultView.setImage(null);
        beforeView.setImage(null);
        resultPlaceholder.setVisible(true);
        comparisonBadges.setVisible(false);
        comparisonSlider.setVisible(false);
        comparisonSlider.setManaged(false);
        comparisonHint.setVisible(false);
        comparisonHint.setManaged(false);
        saveButton.setDisable(true);
    }

    /** Asks for the simulation with what the user chose; a missing choice comes back from the service as an error under its field. */
    private void generate() {
        if (disposed) {
            return;
        }
        clearMessages();
        Customer customer = customerBox.getValue();
        var tone = tones.getSelectedToggle();
        TryOnRequest request = new TryOnRequest(customer == null ? 0 : customer.getId(), photo,
            tone == null ? "" : (String) tone.getUserData(), intensitySlider.getValue());
        generateButton.setDisable(true);
        uploadButton.setDisable(true);
        cameraButton.setDisable(true);
        customerBox.setDisable(true);
        transformationChips.setDisable(true);
        intensitySlider.setDisable(true);
        generateButton.setText("Generando…");
        CompletableFuture.supplyAsync(() -> {
            try {
                return tryOn.generate(request);
            } catch (ValidationException | DataAccessException e) {
                throw new CompletionException(e);
            }
        }, executor).whenComplete((generated, error) -> Platform.runLater(() -> {
            if (disposed) {
                return;
            }
            generateButton.setDisable(false);
            uploadButton.setDisable(false);
            cameraButton.setDisable(false);
            customerBox.setDisable(false);
            transformationChips.setDisable(false);
            intensitySlider.setDisable(false);
            generateButton.setText("Generar simulación");
            if (error != null) {
                showError(error.getCause() instanceof Exception cause ? cause : new IllegalStateException(error));
            } else {
                showResult(generated);
            }
        }));
    }

    private void showResult(TryOnResult generated) {
        result = generated.image();
        resultView.setImage(new Image(result.toUri().toString()));
        resultView.setViewport(generated.contentWidth() > 0 && generated.contentHeight() > 0
            ? new javafx.geometry.Rectangle2D(0, 0, generated.contentWidth(), generated.contentHeight()) : null);
        resultPlaceholder.setVisible(false);
        beforeView.setImage(photoView.getImage());
        Image source = photoView.getImage();
        double generatedRatio = generated.contentWidth() > 0 && generated.contentHeight() > 0
            ? (double) generated.contentWidth() / generated.contentHeight() : resultView.getImage().getWidth() / resultView.getImage().getHeight();
        boolean aligned = source != null && Math.abs(source.getWidth() / source.getHeight() - generatedRatio) < 0.01;
        beforeView.setVisible(aligned);
        comparisonBadges.getChildren().getFirst().setVisible(aligned);
        comparisonBadges.getChildren().getFirst().setManaged(aligned);
        comparisonBadges.setVisible(true);
        comparisonSlider.setVisible(aligned);
        comparisonSlider.setManaged(aligned);
        comparisonHint.setVisible(!aligned);
        comparisonHint.setManaged(!aligned);
        disclaimerLabel.setText(generated.disclaimer().substring(0, 1).toUpperCase(Locale.ROOT) + generated.disclaimer().substring(1));
        saveButton.setDisable(false);
    }

    private void saveTheResult() {
        Path target = result == null ? null : savePicker.get();
        if (target == null) {
            return;
        }
        try {
            Files.copy(result, target, StandardCopyOption.REPLACE_EXISTING);
            alerts.toast("Imagen guardada.");
        } catch (IOException e) {
            alerts.showError(new DataAccessException("No se pudo guardar la imagen.", e));
        }
    }

    /** Puts the reason of a rejected request under the field it is about; anything else goes to a dialog. */
    private void showError(Exception error) {
        String text = Normalizer.normalize(String.valueOf(error.getMessage()), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        if (error instanceof ValidationException && text.contains("cliente")) {
            show(customerMessage, error.getMessage());
        } else if (error instanceof ValidationException && text.contains("foto")) {
            show(photoMessage, error.getMessage());
        } else if (error instanceof ValidationException && text.contains("transformacion")) {
            show(transformationMessage, error.getMessage());
        } else {
            alerts.showError(error);
        }
    }

    private static void show(Label message, String text) {
        message.setText(text);
        message.setVisible(true);
        message.setManaged(true);
    }

    private void clearMessages() {
        for (Label message : List.of(customerMessage, photoMessage, transformationMessage)) {
            message.setVisible(false);
            message.setManaged(false);
        }
    }

    /** The default file dialog of the system, over the window of the screen. */
    private Path choose(boolean saving) {
        FileChooser chooser = new FileChooser();
        var window = uploadButton.getScene() == null ? null : uploadButton.getScene().getWindow();
        File file;
        if (saving) {
            chooser.setTitle("Guardar la simulación");
            chooser.setInitialFileName("simulacion.png");
            file = chooser.showSaveDialog(window);
        } else {
            chooser.setTitle("Elegir la foto de referencia");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
            file = chooser.showOpenDialog(window);
        }
        return file == null ? null : file.toPath();
    }

    /** Closing the session: a simulation still being generated is ignored when it arrives, and its result is never shown. */
    @Override
    public void dispose() {
        disposed = true;
        photo = null;
        result = null;
    }
}
