package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.view.ViewLifecycle;
import com.maisonglow.view.camera.CameraCaptureDialog;
import com.maisonglow.view.camera.WebcamPhotoCamera;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Controller of {@code PhotoPickerPane.fxml}: a reusable panel to choose a photo with the camera or from a file. It
 * shows the photo already kept ({@link #showCurrent}) or, once the person takes or uploads a new one, a preview marked
 * as not saved. Nothing is kept until "Usar esta foto" is pressed, which hands the file to whoever owns the panel
 * through {@link #onUse}; "Descartar" throws the preview away, and taking or uploading again replaces it. A file that
 * is not an image or weighs more than 10 MB is refused under the panel. This class never stores a photo: keeping it is
 * the job of the service the owning screen calls.
 */
public final class PhotoPickerController implements ViewLifecycle {

    /** Biggest photo accepted, in bytes. */
    public static final long MAX_BYTES = 10L * 1024 * 1024;

    private Supplier<Path> cameraPicker;
    private Supplier<Path> filePicker = () -> chooseFile();
    private Consumer<Path> use = photo -> { };
    private Path pending;
    private Path kept;
    private boolean enabled = true;

    @FXML
    private ImageView photoView;
    @FXML
    private Label placeholder;
    @FXML
    private Label stateLabel;
    @FXML
    private Button cameraButton;
    @FXML
    private Button uploadButton;
    @FXML
    private HBox pendingBox;
    @FXML
    private Button useButton;
    @FXML
    private Button discardButton;
    @FXML
    private Label photoMessage;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public PhotoPickerController(AppContext context) {
        Objects.requireNonNull(context, "context");
        this.cameraPicker = () -> new CameraCaptureDialog(new WebcamPhotoCamera(), context.dataFolder().resolve("captures"))
            .show(cameraButton.getScene() == null ? null : cameraButton.getScene().getWindow());
    }

    /**
     * Sets what happens when the person confirms the photo.
     *
     * @param action receives the file chosen
     */
    public void onUse(Consumer<Path> action) {
        this.use = Objects.requireNonNull(action, "action");
    }

    /**
     * Replaces the camera window, which the tests use so that no camera opens.
     *
     * @param picker gives the file captured, or null if the person cancelled
     */
    public void useCameraPicker(Supplier<Path> picker) {
        this.cameraPicker = Objects.requireNonNull(picker, "picker");
    }

    /**
     * Replaces the file dialog, which the tests use so that no real window opens.
     *
     * @param picker gives the file chosen, or null if the person cancelled
     */
    public void useFilePicker(Supplier<Path> picker) {
        this.filePicker = Objects.requireNonNull(picker, "picker");
    }

    @FXML
    private void initialize() {
        hideMessage();
        cameraButton.setOnAction(event -> choose(cameraPicker));
        uploadButton.setOnAction(event -> choose(filePicker));
        useButton.setOnAction(event -> useThePhoto());
        discardButton.setOnAction(event -> discard());
        showCurrent(null);
    }

    /**
     * Shows the photo that is already kept, or the empty frame if there is none. A preview that was waiting is dropped.
     *
     * @param photo the kept photo, or null
     */
    public void showCurrent(Path photo) {
        pending = null;
        kept = photo;
        hideMessage();
        Image image = photo == null ? null : load(photo);
        photoView.setImage(image);
        placeholder.setVisible(image == null);
        stateLabel.setVisible(false);
        stateLabel.setManaged(false);
        pendingBox.setVisible(false);
        pendingBox.setManaged(false);
    }

    /**
     * Turns the panel on or off; off while there is nowhere to keep the photo (for example, a product not saved yet).
     *
     * @param value true to let the person choose
     * @param reason what to tell the person when it is off; ignored when on
     */
    public void setEnabled(boolean value, String reason) {
        enabled = value;
        cameraButton.setDisable(!value);
        uploadButton.setDisable(!value);
        if (!value) {
            showCurrent(null);
            stateLabel.setText(Objects.requireNonNull(reason, "reason"));
            stateLabel.setVisible(true);
            stateLabel.setManaged(true);
        }
    }

    /** @return the preview waiting for "Usar esta foto", empty if there is none */
    public Optional<Path> pending() {
        return Optional.ofNullable(pending);
    }

    private void choose(Supplier<Path> picker) {
        if (!enabled) {
            return;
        }
        hideMessage();
        Path chosen;
        try {
            chosen = picker.get();
        } catch (RuntimeException e) {
            show("No se pudo abrir la cámara. Inténtalo de nuevo o usa «Subir foto».");
            return;
        }
        if (chosen == null) {
            return;
        }
        try {
            if (Files.size(chosen) > MAX_BYTES) {
                show("La foto pesa más de 10 MB. Elige una más liviana.");
                return;
            }
        } catch (IOException e) {
            show("No se pudo leer la foto seleccionada.");
            return;
        }
        Image image = load(chosen);
        if (image == null) {
            show("No se pudo leer la foto. Elige una imagen PNG o JPEG.");
            return;
        }
        pending = chosen;
        photoView.setImage(image);
        placeholder.setVisible(false);
        stateLabel.setText("Vista previa: aún sin guardar");
        stateLabel.setVisible(true);
        stateLabel.setManaged(true);
        pendingBox.setVisible(true);
        pendingBox.setManaged(true);
    }

    private void useThePhoto() {
        if (pending != null) {
            Path photo = pending;
            use.accept(photo);
        }
    }

    /** Throws the preview away and shows the kept photo again, or the empty frame if there was none. */
    private void discard() {
        showCurrent(kept);
    }

    private static Image load(Path file) {
        Image image = new Image(file.toUri().toString(), 520, 400, true, true);
        return image.isError() ? null : image;
    }

    private Path chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Elegir la foto");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
        var window = uploadButton.getScene() == null ? null : uploadButton.getScene().getWindow();
        var file = chooser.showOpenDialog(window);
        return file == null ? null : file.toPath();
    }

    private void show(String text) {
        photoMessage.setText(text);
        photoMessage.setVisible(true);
        photoMessage.setManaged(true);
    }

    private void hideMessage() {
        photoMessage.setVisible(false);
        photoMessage.setManaged(false);
    }

    /** Closing the session: the preview and the handler are dropped, so nothing of the person stays in the panel. */
    @Override
    public void dispose() {
        pending = null;
        use = photo -> { };
        photoView.setImage(null);
    }
}
