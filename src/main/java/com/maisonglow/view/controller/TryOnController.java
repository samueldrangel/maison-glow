package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.ai.SimulatedTryOnService;
import com.maisonglow.ai.TryOnRequest;
import com.maisonglow.ai.TryOnResult;
import com.maisonglow.ai.TryOnService;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Customer;
import com.maisonglow.view.AlertPresenter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
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
import java.util.Map;
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
public final class TryOnController {

    private static final Map<String, Color> TONES = Map.of("rojo cobrizo", Color.web("#b5523b"),
        "rubio dorado", Color.web("#d4a94f"), "castaño oscuro", Color.web("#4a2f23"));
    private static final Color OTHER_TONE = Color.web("#b9a9ac");

    private final TryOnService tryOn;
    private final boolean simulated;
    private final List<Customer> customerList;
    private final ToggleGroup tones = new ToggleGroup();
    private Executor executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "probador");
        thread.setDaemon(true);
        return thread;
    });
    private Supplier<Path> photoPicker = () -> choose(false);
    private Supplier<Path> savePicker = () -> choose(true);
    private AlertPresenter alerts;
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
    @FXML
    private Label photoMessage;
    @FXML
    private FlowPane transformationChips;
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
        List<Customer> found;
        try {
            found = context.customers().listAll();
        } catch (DataAccessException e) {
            found = List.of();
        }
        this.customerList = found;
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
                return null;
            }
        });
        customerBox.getItems().setAll(customerList);
        transformationChips.getChildren().setAll(SimulatedTryOnService.supportedTransformations().stream().map(this::toneChip).toList());
        clearMessages();
        saveButton.setDisable(true);
        uploadButton.setOnAction(event -> chooseThePhoto());
        generateButton.setOnAction(event -> generate());
        saveButton.setOnAction(event -> saveTheResult());
    }

    private ToggleButton toneChip(String name) {
        ToggleButton chip = new ToggleButton(name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1));
        chip.getStyleClass().add("tone-chip");
        chip.setGraphic(new Circle(7, TONES.getOrDefault(name, OTHER_TONE)));
        chip.setUserData(name);
        chip.setToggleGroup(tones);
        return chip;
    }

    private void chooseThePhoto() {
        clearMessages();
        Path chosen = photoPicker.get();
        if (chosen == null) {
            return;
        }
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

    /** Asks for the simulation with what the user chose; a missing choice comes back from the service as an error under its field. */
    private void generate() {
        clearMessages();
        Customer customer = customerBox.getValue();
        var tone = tones.getSelectedToggle();
        TryOnRequest request = new TryOnRequest(customer == null ? 0 : customer.getId(), photo,
            tone == null ? "" : (String) tone.getUserData());
        generateButton.setDisable(true);
        generateButton.setText("Generando…");
        CompletableFuture.supplyAsync(() -> {
            try {
                return tryOn.generate(request);
            } catch (ValidationException | DataAccessException e) {
                throw new CompletionException(e);
            }
        }, executor).whenComplete((generated, error) -> Platform.runLater(() -> {
            generateButton.setDisable(false);
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
        resultPlaceholder.setVisible(false);
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
}
