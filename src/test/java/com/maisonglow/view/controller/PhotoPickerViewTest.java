package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Photo picker panel tests")
class PhotoPickerViewTest {

    private static boolean toolkit;

    @TempDir
    Path data;

    @TempDir
    Path files;

    private AppContext context;
    private Path first;
    private Path second;
    private final List<Path> used = new ArrayList<>();

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @BeforeEach
    void prepare() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        context = new AppContext(data, Clock.systemDefaultZone(), AiConfig.simulated());
        first = image("primera.png", 0x6b4a3a);
        second = image("segunda.png", 0xd4a94f);
    }

    private Path image(String name, int color) throws Exception {
        BufferedImage image = new BufferedImage(120, 90, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 120; x++) {
            for (int y = 0; y < 90; y++) {
                image.setRGB(x, y, color);
            }
        }
        Path file = files.resolve(name);
        ImageIO.write(image, "png", file.toFile());
        return file;
    }

    private ViewLoader.Loaded<PhotoPickerController> open(Supplier<Path> camera, Supplier<Path> file) throws Exception {
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<PhotoPickerController> loaded = ViewLoader.loadWithController(
                PhotoPickerViewTest.class.getResource("/com/maisonglow/view/PhotoPickerPane.fxml"), context);
            loaded.controller().useCameraPicker(camera);
            loaded.controller().useFilePicker(file);
            loaded.controller().onUse(used::add);
            new Scene(loaded.root(), 320, 520);
            loaded.root().applyCss();
            loaded.root().layout();
            return loaded;
        });
    }

    private static void press(Parent root, String id) throws Exception {
        FxTest.onFxThread(() -> {
            ((Button) root.lookup("#" + id)).fire();
            return null;
        });
    }

    private static boolean visible(Parent root, String id) throws Exception {
        return FxTest.onFxThread(() -> root.lookup("#" + id).isVisible());
    }

    private static boolean hasImage(Parent root) throws Exception {
        return FxTest.onFxThread(() -> ((ImageView) root.lookup("#photoView")).getImage() != null);
    }

    private static String message(Parent root) throws Exception {
        return FxTest.onFxThread(() -> {
            Label label = (Label) root.lookup("#photoMessage");
            return label.isVisible() ? label.getText() : "";
        });
    }

    @Test
    @DisplayName("opens with an empty frame, no preview to confirm and nothing handed over")
    void opensEmpty() throws Exception {
        var panel = open(() -> null, () -> null);

        assertThat(hasImage(panel.root())).isFalse();
        assertThat(visible(panel.root(), "placeholder")).isTrue();
        assertThat(visible(panel.root(), "pendingBox")).isFalse();
        assertThat(message(panel.root())).isEmpty();
        assertThat(used).isEmpty();
    }

    @Test
    @DisplayName("a photo from a file shows as a preview not saved yet, and nothing is handed over until it is used")
    void fileGivesAPreview() throws Exception {
        var panel = open(() -> null, () -> first);

        press(panel.root(), "uploadButton");

        assertThat(hasImage(panel.root())).isTrue();
        assertThat(visible(panel.root(), "pendingBox")).isTrue();
        assertThat(visible(panel.root(), "stateLabel")).isTrue();
        assertThat(panel.controller().pending()).contains(first);
        assertThat(used).isEmpty();

        press(panel.root(), "useButton");

        assertThat(used).containsExactly(first);
    }

    @Test
    @DisplayName("the camera works the same way, and taking another photo replaces the preview")
    void cameraAndRepeat() throws Exception {
        List<Path> shots = new ArrayList<>(List.of(first, second));
        var panel = open(() -> shots.remove(0), () -> null);

        press(panel.root(), "cameraButton");
        assertThat(panel.controller().pending()).contains(first);
        press(panel.root(), "cameraButton");
        assertThat(panel.controller().pending()).contains(second);
        press(panel.root(), "useButton");

        assertThat(used).containsExactly(second);
    }

    @Test
    @DisplayName("Descartar drops the preview and shows the photo that was kept before")
    void discardReturnsToTheKeptPhoto() throws Exception {
        var panel = open(() -> null, () -> second);
        FxTest.onFxThread(() -> { panel.controller().showCurrent(first); return null; });
        assertThat(hasImage(panel.root())).isTrue();

        press(panel.root(), "uploadButton");
        press(panel.root(), "discardButton");

        assertThat(panel.controller().pending()).isEmpty();
        assertThat(visible(panel.root(), "pendingBox")).isFalse();
        assertThat(hasImage(panel.root())).isTrue();
        assertThat(visible(panel.root(), "placeholder")).isFalse();
        assertThat(used).isEmpty();
    }

    @Test
    @DisplayName("cancelling the file window or the camera changes nothing")
    void cancelledPickerChangesNothing() throws Exception {
        var panel = open(() -> null, () -> null);

        press(panel.root(), "uploadButton");
        press(panel.root(), "cameraButton");

        assertThat(panel.controller().pending()).isEmpty();
        assertThat(message(panel.root())).isEmpty();
    }

    @Test
    @DisplayName("a file that is not an image, one that is too big and a camera that fails are refused with a clear message")
    void refusesWhatIsNotUsable() throws Exception {
        Path text = files.resolve("texto.png");
        Files.writeString(text, "esto no es una imagen");
        Path big = files.resolve("grande.png");
        try (RandomAccessFile file = new RandomAccessFile(big.toFile(), "rw")) {
            file.setLength(PhotoPickerController.MAX_BYTES + 1);
        }
        Supplier<Path> broken = () -> { throw new IllegalStateException("sin camara"); };
        List<Path> choices = new ArrayList<>(List.of(text, big));
        var panel = open(broken, () -> choices.remove(0));

        press(panel.root(), "uploadButton");
        assertThat(message(panel.root())).contains("No se pudo leer la foto");
        press(panel.root(), "uploadButton");
        assertThat(message(panel.root())).contains("10 MB");
        press(panel.root(), "cameraButton");
        assertThat(message(panel.root())).contains("cámara").contains("Subir foto");

        assertThat(panel.controller().pending()).isEmpty();
        assertThat(hasImage(panel.root())).isFalse();
    }

    @Test
    @DisplayName("turned off it explains why and neither the camera nor the file can be used; turned on again it works")
    void canBeTurnedOff() throws Exception {
        var panel = open(() -> first, () -> first);

        FxTest.onFxThread(() -> { panel.controller().setEnabled(false, "Guarda el producto para añadir su portada."); return null; });
        assertThat(FxTest.onFxThread(() -> ((Label) panel.root().lookup("#stateLabel")).getText())).contains("Guarda el producto");
        assertThat(FxTest.onFxThread(() -> panel.root().lookup("#cameraButton").isDisabled())).isTrue();
        assertThat(FxTest.onFxThread(() -> panel.root().lookup("#uploadButton").isDisabled())).isTrue();

        FxTest.onFxThread(() -> { panel.controller().setEnabled(true, ""); return null; });
        press(panel.root(), "uploadButton");

        assertThat(panel.controller().pending()).contains(first);
    }

    @Test
    @DisplayName("closing the session drops the preview and the handler, so nothing is handed over afterwards")
    void disposeDropsThePreview() throws Exception {
        var panel = open(() -> null, () -> first);
        press(panel.root(), "uploadButton");

        FxTest.onFxThread(() -> { panel.controller().dispose(); return null; });
        press(panel.root(), "useButton");

        assertThat(panel.controller().pending()).isEmpty();
        assertThat(hasImage(panel.root())).isFalse();
        assertThat(used).isEmpty();
    }
}
