package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.TestAccounts;
import com.maisonglow.model.Product;
import com.maisonglow.service.ProductData;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Products view: cover and publication, over the real catalog service")
class ProductsCoverViewTest {

    private static boolean toolkit;

    @TempDir
    Path data;

    @TempDir
    Path files;

    private AppContext context;
    private Product serum;
    private Path first;
    private Path second;
    private final List<String> dialogs = new ArrayList<>();

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @BeforeEach
    void prepare() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        AppContext.seed(data);
        context = TestAccounts.open(data);
        serum = context.products().register(new ProductData("Sérum Vitamina C", 1, 48000, 5), 8);
        first = image("primera.png", 160, 120, 0x6b4a3a);
        second = image("segunda.png", 90, 160, 0xd4a94f);
    }

    private Path image(String name, int width, int height, int color) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, color);
            }
        }
        Path file = files.resolve(name);
        ImageIO.write(image, "png", file.toFile());
        return file;
    }

    private ViewLoader.Loaded<ProductsController> open(boolean confirm) throws Exception {
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<ProductsController> loaded = ViewLoader.loadWithController(
                ProductsCoverViewTest.class.getResource("/com/maisonglow/view/ProductsView.fxml"), context);
            loaded.controller().useExecutor(Runnable::run);
            loaded.controller().useAlerts(new AlertPresenter(() -> null, alert -> {
                dialogs.add(alert.getContentText());
                return alert.getButtonTypes().stream().filter(button -> confirm && "Confirmar".equals(button.getText())).findFirst();
            }));
            new Scene(loaded.root(), 1200, 760);
            loaded.root().applyCss();
            loaded.root().layout();
            return loaded;
        });
    }

    private static void settle() throws Exception {
        for (int round = 0; round < 3; round++) {
            FxTest.onFxThread(() -> null);
            Thread.sleep(60);
        }
    }

    private static void edit(Parent root, String productName) throws Exception {
        FxTest.onFxThread(() -> {
            ((Button) root.lookupAll(".icon-button").stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> ("Editar " + productName).equals(button.getAccessibleText())).findFirst().orElseThrow()).fire();
            return null;
        });
        settle();
    }

    private static void press(Parent root, String id) throws Exception {
        FxTest.onFxThread(() -> {
            ((Button) root.lookup("#" + id)).fire();
            return null;
        });
        settle();
    }

    private static String text(Parent root, String id) throws Exception {
        return FxTest.onFxThread(() -> ((javafx.scene.control.Labeled) root.lookup("#" + id)).getText());
    }

    private static boolean disabled(Parent root, String id) throws Exception {
        return FxTest.onFxThread(() -> root.lookup("#" + id).isDisabled());
    }

    private static boolean hasImage(Parent root) throws Exception {
        return FxTest.onFxThread(() -> ((ImageView) root.lookup("#photoView")).getImage() != null);
    }

    @SuppressWarnings("unchecked")
    private static List<String> published(Parent root) throws Exception {
        return FxTest.onFxThread(() -> ((TableView<Product>) root.lookup("#productsTable")).getItems().stream()
            .map(Product::getName).toList());
    }

    @Test
    @DisplayName("while a product is being registered the cover card is off and nothing can be published")
    void offWhileRegistering() throws Exception {
        var screen = open(true);

        assertThat(disabled(screen.root(), "uploadButton")).isTrue();
        assertThat(disabled(screen.root(), "cameraButton")).isTrue();
        assertThat(disabled(screen.root(), "publishButton")).isTrue();
        assertThat(text(screen.root(), "stateLabel")).contains("portada");
    }

    @Test
    @DisplayName("editing a product turns the card on: a new product starts not published and with no cover")
    void editingTurnsItOn() throws Exception {
        var screen = open(true);

        edit(screen.root(), "Sérum Vitamina C");

        assertThat(disabled(screen.root(), "uploadButton")).isFalse();
        assertThat(disabled(screen.root(), "publishButton")).isFalse();
        assertThat(text(screen.root(), "publishedState")).isEqualTo("No publicado");
        assertThat(text(screen.root(), "publishButton")).isEqualTo("Publicar en la tienda");
        assertThat(hasImage(screen.root())).isFalse();
    }

    @Test
    @DisplayName("publishing makes the product appear in the shop catalog and the list; withdrawing asks first and removes it")
    void publishAndWithdraw() throws Exception {
        var screen = open(true);
        edit(screen.root(), "Sérum Vitamina C");

        press(screen.root(), "publishButton");

        assertThat(context.catalog().listPublished("")).extracting("name").containsExactly("Sérum Vitamina C");
        assertThat(text(screen.root(), "publishedState")).isEqualTo("Publicado");
        assertThat(text(screen.root(), "publishButton")).isEqualTo("Retirar de la tienda");
        assertThat(FxTest.onFxThread(() -> screen.root().lookupAll(".chip-success").stream().map(node -> ((Label) node).getText()).toList()))
            .contains("Publicado");

        press(screen.root(), "publishButton");

        assertThat(dialogs).anyMatch(question -> question.contains("Retirar") && question.contains("inventario"));
        assertThat(context.catalog().listPublished("")).isEmpty();
        assertThat(text(screen.root(), "publishedState")).isEqualTo("No publicado");
        assertThat(context.products().findById(serum.getId()).getStock()).isEqualTo(8);
    }

    @Test
    @DisplayName("answering no to withdrawing keeps the product published")
    void declinedWithdrawalChangesNothing() throws Exception {
        context.catalog().setPublished(serum.getId(), true);
        var screen = open(false);
        edit(screen.root(), "Sérum Vitamina C");

        press(screen.root(), "publishButton");

        assertThat(context.catalog().listPublished("")).hasSize(1);
        assertThat(text(screen.root(), "publishedState")).isEqualTo("Publicado");
    }

    @Test
    @DisplayName("an inactive product cannot be published: the service's reason is shown and nothing changes")
    void inactiveProductIsNotPublished() throws Exception {
        context.products().deactivate(serum.getId());
        var screen = open(true);
        edit(screen.root(), "Sérum Vitamina C");

        press(screen.root(), "publishButton");

        assertThat(dialogs).anyMatch(text -> text.contains("Activa el producto antes de publicarlo"));
        assertThat(context.catalog().listPublished("")).isEmpty();
    }

    @Test
    @DisplayName("a photo is kept as the cover only when it is used, replaces the previous one and shows after reopening")
    void coverIsKeptReplacedAndReloaded() throws Exception {
        var screen = open(true);
        edit(screen.root(), "Sérum Vitamina C");
        screen.controller().photoPicker().useFilePicker(() -> first);

        press(screen.root(), "uploadButton");
        assertThat(Files.exists(coverOrNull())).isFalse();
        press(screen.root(), "useButton");

        Path kept = context.catalog().cover(serum.getId());
        assertThat(Files.isRegularFile(kept)).isTrue();
        assertThat(kept.getParent().getFileName().toString()).isEqualTo("media");
        assertThat(screen.controller().photoPicker().pending()).isEmpty();
        assertThat(hasImage(screen.root())).isTrue();
        assertThat(dialogs).isEmpty();

        screen.controller().photoPicker().useFilePicker(() -> second);
        press(screen.root(), "uploadButton");
        press(screen.root(), "useButton");
        Path replaced = context.catalog().cover(serum.getId());
        assertThat(replaced).isNotEqualTo(kept);
        assertThat(Files.exists(kept)).isFalse();

        var reopened = open(true);
        edit(reopened.root(), "Sérum Vitamina C");
        assertThat(hasImage(reopened.root())).isTrue();
    }

    private Path coverOrNull() {
        try {
            return context.catalog().cover(serum.getId());
        } catch (Exception e) {
            return files.resolve("no-hay-portada");
        }
    }

    @Test
    @DisplayName("a photo the service refuses keeps the previous cover, says why and leaves the preview to try again")
    void refusedPhotoKeepsThePreviousCover() throws Exception {
        var screen = open(true);
        edit(screen.root(), "Sérum Vitamina C");
        screen.controller().photoPicker().useFilePicker(() -> first);
        press(screen.root(), "uploadButton");
        press(screen.root(), "useButton");
        Path kept = context.catalog().cover(serum.getId());
        Path tooWide = image("ancha.png", 4100, 10, 0x123456);
        screen.controller().photoPicker().useFilePicker(() -> tooWide);

        press(screen.root(), "uploadButton");
        press(screen.root(), "useButton");

        assertThat(dialogs).anyMatch(text -> text.contains("4096"));
        assertThat(context.catalog().cover(serum.getId())).isEqualTo(kept);
        assertThat(Files.isRegularFile(kept)).isTrue();
        assertThat(screen.controller().photoPicker().pending()).contains(tooWide);
    }

    @Test
    @DisplayName("closing the session while a cover is being saved ignores the answer, and no preview stays")
    void disposeDropsTheAnswer() throws Exception {
        var loaded = FxTest.onFxThread(() -> {
            ViewLoader.Loaded<ProductsController> opened = ViewLoader.loadWithController(
                ProductsCoverViewTest.class.getResource("/com/maisonglow/view/ProductsView.fxml"), context);
            List<Runnable> held = new ArrayList<>();
            opened.controller().useExecutor(held::add);
            new Scene(opened.root(), 1200, 760);
            opened.root().applyCss();
            opened.root().layout();
            return java.util.Map.entry(opened, held);
        });
        var screen = loaded.getKey();
        edit(screen.root(), "Sérum Vitamina C");
        screen.controller().photoPicker().useFilePicker(() -> first);
        press(screen.root(), "uploadButton");
        press(screen.root(), "useButton");

        FxTest.onFxThread(() -> { screen.controller().dispose(); return null; });
        loaded.getValue().forEach(Runnable::run);
        settle();

        assertThat(screen.controller().photoPicker().pending()).isEmpty();
        assertThat(Optional.of(published(screen.root()))).isPresent();
    }
}
