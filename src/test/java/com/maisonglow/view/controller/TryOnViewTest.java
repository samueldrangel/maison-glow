package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import com.maisonglow.model.Customer;
import com.maisonglow.service.ContactData;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Virtual try-on view tests")
class TryOnViewTest {

    private static boolean toolkit;

    @TempDir
    Path data;

    @TempDir
    Path files;

    private AppContext context;
    private Path photo;
    private Path saveTo;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @BeforeEach
    void prepareData() throws Exception {
        AppContext.seed(data);
        context = new AppContext(data, Clock.systemDefaultZone(), AiConfig.simulated());
        context.customers().register(new ContactData("1001", "Ana García", "3001112233", "ana@correo.com"));
        photo = files.resolve("foto.png");
        BufferedImage image = new BufferedImage(160, 200, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 160; x++) {
            for (int y = 0; y < 200; y++) {
                image.setRGB(x, y, 0x6b4a3a);
            }
        }
        ImageIO.write(image, "png", photo.toFile());
        saveTo = files.resolve("guardada.png");
    }

    private Parent open(Supplier<Path> photoPicker) throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        context = new AppContext(data, Clock.systemDefaultZone(), AiConfig.simulated());
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<TryOnController> loaded = ViewLoader.loadWithController(
                TryOnViewTest.class.getResource("/com/maisonglow/view/TryOnView.fxml"), context);
            loaded.controller().useExecutor(Runnable::run);
            loaded.controller().useFilePickers(photoPicker, () -> saveTo);
            Parent root = loaded.root();
            new Scene(root, 1250, 760);
            root.applyCss();
            root.layout();
            return root;
        });
    }

    private Parent open() throws Exception {
        return open(() -> photo);
    }

    private static void press(Parent root, String id) throws Exception {
        FxTest.onFxThread(() -> {
            ((Button) root.lookup("#" + id)).fire();
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private static void chooseCustomer(Parent root) throws Exception {
        ComboBox<Customer> box = (ComboBox<Customer>) root.lookup("#customerBox");
        FxTest.onFxThread(() -> {
            box.getSelectionModel().select(0);
            return null;
        });
    }

    private static void chooseTone(Parent root, String name) throws Exception {
        FxTest.onFxThread(() -> {
            root.lookupAll(".tone-chip").stream().map(ToggleButton.class::cast)
                .filter(chip -> chip.getText().equalsIgnoreCase(name)).findFirst().orElseThrow().fire();
            return null;
        });
    }

    private static String message(Parent root, String id) throws Exception {
        Label label = (Label) root.lookup("#" + id);
        return FxTest.onFxThread(() -> label.isVisible() ? label.getText() : "");
    }

    /** The answer comes back on its own thread of the screen; this waits until the generate button is ready again. */
    private static void waitForTheAnswer(Parent root) throws Exception {
        Button generate = (Button) root.lookup("#generateButton");
        for (int attempt = 0; attempt < 80; attempt++) {
            if (FxTest.onFxThread(() -> !generate.isDisabled())) {
                return;
            }
            Thread.sleep(25);
        }
    }

    private static boolean hasImage(Parent root, String id) throws Exception {
        return FxTest.onFxThread(() -> ((ImageView) root.lookup("#" + id)).getImage() != null);
    }

    @Test
    @DisplayName("opens with the customers, the tones the service offers, the simulated notice and nothing to save yet")
    void opensReady() throws Exception {
        Parent root = open();

        assertThat(FxTest.onFxThread(() -> ((ComboBox<?>) root.lookup("#customerBox")).getItems().size())).isEqualTo(1);
        assertThat(FxTest.onFxThread(() -> root.lookupAll(".tone-chip").stream().map(chip -> ((ToggleButton) chip).getText()).toList()))
            .containsExactly("Rojo cobrizo", "Rubio dorado", "Castaño oscuro");
        assertThat(FxTest.onFxThread(() -> root.lookup("#simulatedBanner").isVisible())).isTrue();
        assertThat(FxTest.onFxThread(() -> root.lookup("#saveButton").isDisabled())).isTrue();
        assertThat(message(root, "customerMessage")).isEmpty();
    }

    @Test
    @DisplayName("choosing a photo shows it and hides the placeholder")
    void uploadShowsThePhoto() throws Exception {
        Parent root = open();

        press(root, "uploadButton");

        assertThat(hasImage(root, "photoView")).isTrue();
        assertThat(FxTest.onFxThread(() -> root.lookup("#photoPlaceholder").isVisible())).isFalse();
    }

    @Test
    @DisplayName("a file that is not an image is not accepted and says why")
    void rejectsAFileThatIsNotAnImage() throws Exception {
        Path notAnImage = files.resolve("no-es-imagen.png");
        Files.writeString(notAnImage, "esto no es una imagen");
        Parent root = open(() -> notAnImage);

        press(root, "uploadButton");

        assertThat(message(root, "photoMessage")).contains("No se pudo leer la foto");
        assertThat(hasImage(root, "photoView")).isFalse();
    }

    @Test
    @DisplayName("a request without a customer is rejected under the customer and nothing is generated")
    void rejectsARequestWithoutACustomer() throws Exception {
        Parent root = open();
        press(root, "uploadButton");
        chooseTone(root, "Rojo cobrizo");

        press(root, "generateButton");
        waitForTheAnswer(root);

        assertThat(message(root, "customerMessage")).contains("cliente");
        assertThat(hasImage(root, "resultView")).isFalse();
        assertThat(FxTest.onFxThread(() -> root.lookup("#saveButton").isDisabled())).isTrue();
    }

    @Test
    @DisplayName("a request without a tone is rejected under the tones")
    void rejectsARequestWithoutATone() throws Exception {
        Parent root = open();
        chooseCustomer(root);
        press(root, "uploadButton");

        press(root, "generateButton");
        waitForTheAnswer(root);

        assertThat(message(root, "transformationMessage")).isNotEmpty();
        assertThat(hasImage(root, "resultView")).isFalse();
    }

    @Test
    @DisplayName("a request without a photo is rejected under the photo")
    void rejectsARequestWithoutAPhoto() throws Exception {
        Parent root = open(() -> null);
        chooseCustomer(root);
        chooseTone(root, "Rubio dorado");
        press(root, "uploadButton");

        press(root, "generateButton");
        waitForTheAnswer(root);

        assertThat(message(root, "photoMessage")).isNotEmpty();
        assertThat(hasImage(root, "resultView")).isFalse();
    }

    @Test
    @DisplayName("with a customer, a photo and a tone it shows the result with the notice that it is a simulation")
    void generatesTheResult() throws Exception {
        Parent root = open();
        chooseCustomer(root);
        press(root, "uploadButton");
        chooseTone(root, "Rojo cobrizo");

        press(root, "generateButton");
        waitForTheAnswer(root);

        assertThat(hasImage(root, "resultView")).isTrue();
        assertThat(FxTest.onFxThread(() -> ((Label) root.lookup("#disclaimerLabel")).getText()).toLowerCase()).contains("simulación, no garantía");
        assertThat(FxTest.onFxThread(() -> root.lookup("#resultPlaceholder").isVisible())).isFalse();
        assertThat(FxTest.onFxThread(() -> root.lookup("#saveButton").isDisabled())).isFalse();
        assertThat(FxTest.onFxThread(() -> ((Button) root.lookup("#generateButton")).getText())).isEqualTo("Generar simulación");
    }

    @Test
    @DisplayName("Guardar copies the result to the place the user chose")
    void savesTheResult() throws Exception {
        Parent root = open();
        chooseCustomer(root);
        press(root, "uploadButton");
        chooseTone(root, "Castaño oscuro");
        press(root, "generateButton");
        waitForTheAnswer(root);

        press(root, "saveButton");

        assertThat(saveTo).exists();
        assertThat(Files.size(saveTo)).isGreaterThan(0);
        assertThat(ImageIO.read(saveTo.toFile())).isNotNull();
    }
}
