package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import com.maisonglow.model.Product;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.ProductData;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLifecycle;
import com.maisonglow.view.ViewLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Shop view tests, over the real catalog")
class ShopViewTest {

    private static final char[] PASSWORD = "clave-segura-123".toCharArray();

    private static boolean toolkit;

    @TempDir
    Path data;

    @TempDir
    Path files;

    private AppContext context;
    private Product champu;
    private Product esmalte;
    private Product mascarilla;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @BeforeEach
    void prepare() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        AppContext.seed(data);
        context = new AppContext(data, Clock.systemDefaultZone(), AiConfig.simulated());
        context.auth().createFirstAdmin("admin", PASSWORD);
        context.auth().login("admin", PASSWORD);
        context.auth().registerCustomer(new ContactData("1001", "Ana García", "3001112233", "ana@correo.com"), "ana", PASSWORD);
        context.auth().registerCustomer(new ContactData("1002", "Laura Pérez", "3002223344", "laura@correo.com"), "laura", PASSWORD);
        champu = context.products().register(new ProductData("Champú Nutritivo", 1, 28000, 10), 10);
        esmalte = context.products().register(new ProductData("Esmalte Rosa Nude", 3, 18000, 2), 2);
        mascarilla = context.products().register(new ProductData("Mascarilla Reparadora", 1, 35000, 1), 1);
        context.products().register(new ProductData("Aceite de Cutículas", 3, 16000, 1), 5);
        context.catalog().setCover(champu.getId(), image("champu.png"));
        for (Product product : List.of(champu, esmalte, mascarilla)) {
            context.catalog().setPublished(product.getId(), true);
        }
        context.auth().login("laura", PASSWORD);
        context.orders().place(UUID.randomUUID().toString(), Map.of(mascarilla.getId(), 1));
        context.auth().login("ana", PASSWORD);
    }

    private Path image(String name) throws Exception {
        BufferedImage image = new BufferedImage(120, 90, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 120; x++) {
            for (int y = 0; y < 90; y++) {
                image.setRGB(x, y, 0xc99269);
            }
        }
        Path file = files.resolve(name);
        ImageIO.write(image, "png", file.toFile());
        return file;
    }

    private ViewLoader.Loaded<ShopController> open() throws Exception {
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<ShopController> loaded = ViewLoader.loadWithController(
                ShopViewTest.class.getResource("/com/maisonglow/view/ShopView.fxml"), context);
            new Scene(loaded.root(), 1100, 700);
            loaded.root().applyCss();
            loaded.root().layout();
            return loaded;
        });
    }

    private static void settle() throws Exception {
        for (int round = 0; round < 4; round++) {
            FxTest.onFxThread(() -> null);
            Thread.sleep(80);
        }
    }

    private static void collect(Node node, List<String> found) {
        if (node instanceof Label label && label.getText() != null) {
            found.add(label.getText());
        }
        if (node instanceof Pane pane) {
            pane.getChildren().forEach(child -> collect(child, found));
        }
    }

    private static List<Node> cards(Parent root) throws Exception {
        settle();
        return FxTest.onFxThread(() -> new ArrayList<>(((Pane) root.lookup("#productsPane")).getChildren()));
    }

    private static List<String> textsOf(Node node) throws Exception {
        return FxTest.onFxThread(() -> {
            List<String> found = new ArrayList<>();
            collect(node, found);
            return found;
        });
    }

    private static List<String> names(Parent root) throws Exception {
        List<String> names = new ArrayList<>();
        for (Node card : cards(root)) {
            names.add(textsOf(card).getFirst().equals("Sin foto") ? textsOf(card).get(1) : textsOf(card).getFirst());
        }
        return names;
    }

    private static void click(Node card) throws Exception {
        FxTest.onFxThread(() -> {
            card.getOnMouseClicked().handle(null);
            return null;
        });
        settle();
    }

    private static String text(Parent root, String id) throws Exception {
        settle();
        return FxTest.onFxThread(() -> ((Label) root.lookup("#" + id)).getText());
    }

    private static boolean shown(Parent root, String id) throws Exception {
        settle();
        return FxTest.onFxThread(() -> root.lookup("#" + id).isVisible());
    }

    @Test
    @DisplayName("lists only what the business published, by name, and nothing that is only active")
    void listsOnlyPublished() throws Exception {
        Parent root = open().root();

        assertThat(names(root)).containsExactly("Champú Nutritivo", "Esmalte Rosa Nude", "Mascarilla Reparadora");
        assertThat(shown(root, "emptyLabel")).isFalse();
    }

    @Test
    @DisplayName("each card says available, few units or sold out (counting what others reserved) and never a stock figure")
    void availabilityWithoutFigures() throws Exception {
        Parent root = open().root();
        List<Node> cards = cards(root);

        assertThat(textsOf(cards.get(0))).contains("Disponible", "Cabello", "$ 28.000");
        assertThat(textsOf(cards.get(1))).contains("Pocas unidades", "Uñas");
        assertThat(textsOf(cards.get(2))).contains("Agotado");
        for (Node card : cards) {
            assertThat(textsOf(card)).noneMatch(text -> text.toLowerCase().contains("stock") || text.toLowerCase().contains("mínimo")
                || text.matches(".*\\b(10|2|1) (unidades|uds).*"));
        }
    }

    @Test
    @DisplayName("the cover of a product shows on its card; the others say they have no photo")
    void covers() throws Exception {
        Parent root = open().root();
        List<Node> cards = cards(root);

        assertThat(FxTest.onFxThread(() -> cards.get(0).lookupAll(".image-view").stream()
            .anyMatch(view -> view instanceof ImageView image && image.getImage() != null))).isTrue();
        assertThat(textsOf(cards.get(1))).contains("Sin foto");
    }

    @Test
    @DisplayName("the category chips come from the published products and filter the list; pressing one again shows all")
    void categoryChips() throws Exception {
        Parent root = open().root();
        settle();
        List<ToggleButton> chips = FxTest.onFxThread(() -> root.lookupAll(".filter-chip").stream().map(ToggleButton.class::cast).toList());
        assertThat(chips.stream().map(ToggleButton::getText)).containsExactly("Cabello", "Uñas");

        FxTest.onFxThread(() -> { chips.get(1).fire(); return null; });
        assertThat(names(root)).containsExactly("Esmalte Rosa Nude");
        FxTest.onFxThread(() -> { chips.get(1).fire(); return null; });
        assertThat(names(root)).hasSize(3);
    }

    @Test
    @DisplayName("the search finds by name ignoring accents and case, and says so when nothing matches")
    void search() throws Exception {
        Parent root = open().root();
        settle();

        FxTest.onFxThread(() -> { ((TextField) root.lookup("#searchField")).setText("CHAMPU"); return null; });
        assertThat(names(root)).containsExactly("Champú Nutritivo");
        FxTest.onFxThread(() -> { ((TextField) root.lookup("#searchField")).setText("zzz"); return null; });
        assertThat(names(root)).isEmpty();
        assertThat(text(root, "emptyLabel")).contains("coincide");
        assertThat(shown(root, "emptyLabel")).isTrue();
    }

    @Test
    @DisplayName("choosing a product opens its detail with name, category, price and availability, and no internal figures")
    void detail() throws Exception {
        var shop = open();
        Parent root = shop.root();
        click(cards(root).get(1));

        assertThat(text(root, "detailName")).isEqualTo("Esmalte Rosa Nude");
        assertThat(text(root, "detailCategory")).isEqualTo("Uñas");
        assertThat(text(root, "detailAvailability")).isEqualTo("Pocas unidades");
        assertThat(text(root, "detailPrice")).isEqualTo("$ 18.000");
        assertThat(text(root, "detailNote")).isEqualTo("Se paga y se retira en el local.");
        assertThat(shown(root, "detailUnavailable")).isFalse();
        assertThat(shop.controller().selected()).isEqualTo(esmalte.getId());

        click(cards(root).get(2));
        assertThat(text(root, "detailAvailability")).isEqualTo("Agotado");
        assertThat(text(root, "detailNote")).contains("no hay unidades");
    }

    @Test
    @DisplayName("a product the business withdraws while it is being looked at says it is no longer available and leaves the list")
    void withdrawnProduct() throws Exception {
        Parent root = open().root();
        List<Node> stale = cards(root);
        context.auth().login("admin", PASSWORD);
        context.catalog().setPublished(champu.getId(), false);
        context.auth().login("ana", PASSWORD);

        click(stale.get(0));

        assertThat(shown(root, "detailUnavailable")).isTrue();
        assertThat(text(root, "detailAvailability")).isEqualTo("Agotado");
        assertThat(shown(root, "detailNote")).isFalse();
        assertThat(names(root)).containsExactly("Esmalte Rosa Nude", "Mascarilla Reparadora");
    }

    @Test
    @DisplayName("showing the shop again reads it again: a product published meanwhile appears")
    void refreshShowsNewProducts() throws Exception {
        var shop = open();
        assertThat(names(shop.root())).hasSize(3);
        context.auth().login("admin", PASSWORD);
        context.catalog().setPublished(context.products().findById(context.products().listAll().stream()
            .filter(product -> product.getName().startsWith("Aceite")).findFirst().orElseThrow().getId()).getId(), true);
        context.auth().login("ana", PASSWORD);

        FxTest.onFxThread(() -> { shop.controller().refresh(); return null; });

        assertThat(names(shop.root())).hasSize(4).contains("Aceite de Cutículas");
    }

    @Test
    @DisplayName("with nothing published the shop says there is nothing yet")
    void emptyShop() throws Exception {
        context.auth().login("admin", PASSWORD);
        for (Product product : List.of(champu, esmalte, mascarilla)) {
            context.catalog().setPublished(product.getId(), false);
        }
        context.auth().login("ana", PASSWORD);

        Parent root = open().root();

        assertThat(cards(root)).isEmpty();
        assertThat(text(root, "emptyLabel")).isEqualTo("Aún no hay productos en la tienda.");
        assertThat(shown(root, "emptyLabel")).isTrue();
        assertThat(shown(root, "detailBox")).isFalse();
    }

    @Test
    @DisplayName("an answer that arrives after the session closed is ignored and no product stays on screen")
    void lateAnswerIsIgnored() throws Exception {
        ViewLoader.Loaded<ShopController> loaded = FxTest.onFxThread(() -> {
            ViewLoader.Loaded<ShopController> opened = ViewLoader.loadWithController(
                ShopViewTest.class.getResource("/com/maisonglow/view/ShopView.fxml"), context);
            new Scene(opened.root(), 1100, 700);
            opened.root().applyCss();
            opened.root().layout();
            ((ViewLifecycle) opened.controller()).dispose();
            return opened;
        });
        Thread.sleep(300);
        settle();

        assertThat(cards(loaded.root())).isEmpty();
    }
}
