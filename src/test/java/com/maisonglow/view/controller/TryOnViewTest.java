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
            .containsExactlyElementsOf(com.maisonglow.ai.HairTones.names().stream()
                .map(name -> name.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + name.substring(1)).toList());
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
    void capturedPhotoCanBeSelectedGeneratedAndSaved() throws Exception {
        assumeTrue(toolkit,"JavaFX toolkit needs a display");
        Parent root=FxTest.onFxThread(() -> {
            ViewLoader.Loaded<TryOnController> loaded=ViewLoader.loadWithController(
                TryOnViewTest.class.getResource("/com/maisonglow/view/TryOnView.fxml"),context);
            loaded.controller().useExecutor(Runnable::run);
            loaded.controller().useCameraPicker(() -> photo);
            loaded.controller().useFilePickers(() -> null,() -> saveTo);
            new Scene(loaded.root(),1250,760);loaded.root().applyCss();loaded.root().layout();return loaded.root();
        });
        chooseCustomer(root);press(root,"cameraButton");chooseTone(root,"Rojo cobrizo");
        assertThat(hasImage(root,"photoView")).isTrue();
        press(root,"generateButton");waitForTheAnswer(root);press(root,"saveButton");
        assertThat(saveTo).exists();assertThat(ImageIO.read(saveTo.toFile())).isNotNull();
    }

    @Test
    void cancelingTheCameraKeepsThePreviousReferencePhoto() throws Exception {
        assumeTrue(toolkit,"JavaFX toolkit needs a display");
        Parent canceled=FxTest.onFxThread(() -> {
            ViewLoader.Loaded<TryOnController> loaded=ViewLoader.loadWithController(
                TryOnViewTest.class.getResource("/com/maisonglow/view/TryOnView.fxml"),context);
            loaded.controller().useFilePickers(() -> photo,() -> saveTo);
            loaded.controller().useCameraPicker(() -> null);
            new Scene(loaded.root(),1250,760);loaded.root().applyCss();loaded.root().layout();return loaded.root();
        });
        press(canceled,"uploadButton");
        var selected=FxTest.onFxThread(() -> ((ImageView)canceled.lookup("#photoView")).getImage());
        press(canceled,"cameraButton");
        assertThat(FxTest.onFxThread(() -> ((ImageView)canceled.lookup("#photoView")).getImage())).isSameAs(selected);
    }

    @Test
    void refreshesCustomersCreatedAfterOpeningTheCachedScreen() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        var host = FxTest.onFxThread(() -> {
            var pane = new javafx.scene.layout.StackPane();
            new Scene(pane,1250,760);
            return pane;
        });
        var navigator = new com.maisonglow.view.Navigator(host, context, com.maisonglow.view.access.AccessRole.ADMIN);
        FxTest.onFxThread(() -> { navigator.show(com.maisonglow.view.View.TRY_ON); return null; });
        Parent original = FxTest.onFxThread(() -> (Parent) host.getChildren().getFirst());
        FxTest.onFxThread(() -> { navigator.show(com.maisonglow.view.View.CUSTOMERS); return null; });
        Customer added = context.customers().register(new ContactData("1002", "Laura Martínez", "3002223344", "laura@example.com"));
        FxTest.onFxThread(() -> {
            navigator.show(com.maisonglow.view.View.TRY_ON);
            original.applyCss(); original.layout();
            assertThat(host.getChildren().getFirst()).isSameAs(original);
            ComboBox<?> box = (ComboBox<?>) original.lookup("#customerBox");
            assertThat(box.getItems().stream().map(c -> ((Customer)c).getId()).toList()).contains(added.getId());
            return null;
        });
    }

    @Test
    void changingToneOrPhotoInvalidatesThePreviousResult() throws Exception {
        Parent root = open(); chooseCustomer(root); press(root,"uploadButton"); chooseTone(root,"Rojo cobrizo");
        press(root,"generateButton"); waitForTheAnswer(root);
        assertThat(hasImage(root,"resultView")).isTrue();
        chooseTone(root,"Rubio dorado");
        assertThat(hasImage(root,"resultView")).isFalse();
        assertThat(FxTest.onFxThread(() -> root.lookup("#saveButton").isDisabled())).isTrue();
    }

    @Test
    void rendersPaletteWithoutClippingAtSupportedWindowSizes() throws Exception {
        Path previewPhoto = Path.of(System.getProperty("maison.visualPhoto", photo.toString()));
        Parent root = open(() -> previewPhoto);
        chooseCustomer(root); press(root,"uploadButton"); chooseTone(root,"Castaño chocolate");
        press(root,"generateButton"); waitForTheAnswer(root);
        Path output = Path.of("target", "visual-qa"); Files.createDirectories(output);
        for (int[] size : new int[][]{{836,520},{884,600},{1250,820},{1844,1000}}) {
            FxTest.onFxThread(() -> {
                root.resize(size[0],size[1]); root.applyCss(); root.layout();
                var beforeImage = root.lookup("#beforeView");
                var afterImage = root.lookup("#resultView");
                var beforeBounds = beforeImage.localToScene(beforeImage.getBoundsInLocal());
                var afterBounds = afterImage.localToScene(afterImage.getBoundsInLocal());
                assertThat(afterBounds.getMinX()).as("comparison images share the same horizontal origin")
                    .isCloseTo(beforeBounds.getMinX(), org.assertj.core.data.Offset.offset(1.0));
                assertThat(afterBounds.getMinY()).as("comparison images share the same vertical origin")
                    .isCloseTo(beforeBounds.getMinY(), org.assertj.core.data.Offset.offset(1.0));
                var palette = (javafx.scene.layout.TilePane)root.lookup("#transformationChips");
                for (String id : new String[]{"photoFrame", "resultFrame"}) {
                    var frame = root.lookup("#"+id);
                    assertThat(frame.getBoundsInParent().getMaxX()).isLessThanOrEqualTo(frame.getParent().getLayoutBounds().getWidth());
                }
                assertThat(palette.getChildren()).hasSize(15);
                for (var chip : palette.getChildren()) {
                    assertThat(chip.getBoundsInParent().getMaxX()).isLessThanOrEqualTo(palette.getWidth()+1);
                    assertThat(chip.getBoundsInParent().getMaxY()).isLessThanOrEqualTo(palette.getHeight()+1);
                }
                javafx.scene.image.WritableImage snapshot = root.snapshot(null,null);
                BufferedImage image = new BufferedImage((int)snapshot.getWidth(),(int)snapshot.getHeight(),BufferedImage.TYPE_INT_ARGB);
                var pixels=snapshot.getPixelReader();
                for(int y=0;y<image.getHeight();y++) for(int x=0;x<image.getWidth();x++) image.setRGB(x,y,pixels.getArgb(x,y));
                try { ImageIO.write(image,"png",output.resolve("tryon-"+size[0]+".png").toFile()); }
                catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
                return null;
            });
        }
    }

    @Test
    void differentAspectRatiosUseTheReferencePhotoInsteadOfAMisalignedWipe() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        AppContext testContext = org.mockito.Mockito.mock(AppContext.class);
        org.mockito.Mockito.when(testContext.customers()).thenReturn(context.customers());
        org.mockito.Mockito.when(testContext.ai()).thenReturn(new com.maisonglow.ai.AiModule(context.ai().assistant(),
            request -> new com.maisonglow.ai.TryOnResult(photo,"simulación, no garantía",160,160),false));
        Parent root = FxTest.onFxThread(() -> {
            ViewLoader.Loaded<TryOnController> loaded=ViewLoader.loadWithController(
                TryOnViewTest.class.getResource("/com/maisonglow/view/TryOnView.fxml"),testContext);
            loaded.controller().useExecutor(Runnable::run);loaded.controller().useFilePickers(() -> photo,() -> saveTo);
            new Scene(loaded.root(),1250,760);loaded.root().applyCss();loaded.root().layout();return loaded.root();
        });
        chooseCustomer(root);press(root,"uploadButton");chooseTone(root,"Rojo cobrizo");press(root,"generateButton");waitForTheAnswer(root);
        assertThat(FxTest.onFxThread(() -> root.lookup("#comparisonSlider").isVisible())).isFalse();
        assertThat(FxTest.onFxThread(() -> root.lookup("#comparisonHint").isVisible())).isTrue();
        assertThat(hasImage(root,"resultView")).isTrue();
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
        var viewport = FxTest.onFxThread(() -> ((ImageView) root.lookup("#resultView")).getViewport());
        assertThat(viewport.getWidth()).isEqualTo(160);
        assertThat(viewport.getHeight()).isEqualTo(200);
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
