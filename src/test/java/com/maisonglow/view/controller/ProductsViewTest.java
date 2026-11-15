package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.model.Product;
import com.maisonglow.model.ProductCategory;
import com.maisonglow.service.ProductData;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Products view tests")
class ProductsViewTest {

    private static final int HAIR = 1;
    private static final int NAILS = 3;

    private static boolean toolkit;

    @TempDir
    Path data;

    private final List<String> dialogs = new ArrayList<>();

    private AppContext context;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @BeforeEach
    void prepareData() throws Exception {
        AppContext.seed(data);
        context = new AppContext(data);
    }

    /** The three products of the mockup: two to restock (one with no units) and one that is fine. */
    private void registerSomeProducts() throws Exception {
        context.products().register(new ProductData("Champú Nutritivo", HAIR, 28000, 10), 5);
        context.products().register(new ProductData("Esmalte Rosa Nude", NAILS, 18000, 8), 0);
        context.products().register(new ProductData("Aceite de Cutículas", NAILS, 16000, 6), 12);
    }

    private Parent open() throws Exception {
        return openScreen().root();
    }

    /** Opens the screen with a presenter that records the dialogs instead of showing them. */
    private ViewLoader.Loaded<ProductsController> openScreen() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        ViewLoader.Loaded<ProductsController> screen = FxTest.onFxThread(() -> {
            ViewLoader.Loaded<ProductsController> loaded =
                ViewLoader.loadWithController(ProductsViewTest.class.getResource("/com/maisonglow/view/ProductsView.fxml"), context);
            new Scene(loaded.root(), 1100, 700);
            loaded.root().applyCss();
            loaded.root().layout();
            return loaded;
        });
        screen.controller().useAlerts(new AlertPresenter(() -> null, alert -> {
            dialogs.add(((Label) alert.getDialogPane().getContent().lookup(".dialog-text")).getText());
            return Optional.empty();
        }));
        return screen;
    }

    private static void fill(Parent root, String id, String text) throws Exception {
        FxTest.onFxThread(() -> {
            ((TextField) root.lookup(id)).setText(text);
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private static void choose(Parent root, String categoryName) throws Exception {
        ComboBox<ProductCategory> box = (ComboBox<ProductCategory>) root.lookup("#categoryBox");
        FxTest.onFxThread(() -> {
            box.getItems().stream().filter(category -> category.getName().equals(categoryName)).findFirst().ifPresent(box::setValue);
            return null;
        });
    }

    private static void press(Parent root, String id) throws Exception {
        FxTest.onFxThread(() -> {
            ((Button) root.lookup(id)).fire();
            return null;
        });
    }

    private static String message(Parent root, String id) throws Exception {
        return FxTest.onFxThread(() -> ((Label) root.lookup(id)).getText());
    }

    private static boolean marked(Parent root, String id) throws Exception {
        return FxTest.onFxThread(() -> root.lookup(id).getStyleClass().contains("field-error"));
    }

    /** The small button of a row, found by what it does and to which product, for example "Editar Champú". */
    private static Button iconButton(Parent root, String description) throws Exception {
        return FxTest.onFxThread(() -> {
            root.applyCss();
            root.layout();
            return root.lookupAll(".icon-button").stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> description.equals(button.getAccessibleText())).findFirst().orElseThrow();
        });
    }

    private static void fire(Button button) throws Exception {
        FxTest.onFxThread(() -> {
            button.fire();
            return null;
        });
    }

    /** Answers "Confirmar" to every question, as a user who accepts would. */
    private static void acceptQuestions(ViewLoader.Loaded<ProductsController> screen) {
        screen.controller().useAlerts(new AlertPresenter(() -> null, alert ->
            alert.getButtonTypes().stream().filter(button -> "Confirmar".equals(button.getText())).findFirst()));
    }

    @SuppressWarnings("unchecked")
    private static void pick(Parent root, String productName) throws Exception {
        ComboBox<Product> box = (ComboBox<Product>) root.lookup("#adjustProductBox");
        FxTest.onFxThread(() -> {
            box.getItems().stream().filter(product -> product.getName().equals(productName)).findFirst().ifPresent(box::setValue);
            return null;
        });
    }

    /** Fills the registration form and presses Guardar. */
    private static void register(Parent root, String name, String category, String price, String minimum, String stock) throws Exception {
        fill(root, "#nameField", name);
        if (category != null) {
            choose(root, category);
        }
        fill(root, "#priceField", price);
        fill(root, "#minimumField", minimum);
        fill(root, "#initialStockField", stock);
        press(root, "#saveButton");
    }

    @SuppressWarnings("unchecked")
    private static List<String> names(Parent root) throws Exception {
        TableView<com.maisonglow.model.Product> table = (TableView<com.maisonglow.model.Product>) root.lookup("#productsTable");
        return FxTest.onFxThread(() -> table.getItems().stream().map(com.maisonglow.model.Product::getName).toList());
    }

    private static ToggleButton chip(Parent root, String text) {
        return root.lookupAll(".filter-chip").stream().map(ToggleButton.class::cast)
            .filter(chip -> text.equals(chip.getText())).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("lists every product and warns about the ones to restock")
    void listsAndWarns() throws Exception {
        registerSomeProducts();

        Parent root = open();

        assertThat(names(root)).containsExactlyInAnyOrder("Champú Nutritivo", "Esmalte Rosa Nude", "Aceite de Cutículas");
        assertThat(root.lookup("#lowStockBanner").isVisible()).isTrue();
        assertThat(((Label) root.lookup("#lowStockText")).getText()).isEqualTo("2 productos por debajo del stock mínimo");
    }

    @Test
    @DisplayName("shows no warning when nothing needs restocking")
    void noWarningWhenAllIsFine() throws Exception {
        context.products().register(new ProductData("Aceite de Cutículas", NAILS, 16000, 6), 12);

        Parent root = open();

        assertThat(root.lookup("#lowStockBanner").isVisible()).isFalse();
        assertThat(root.lookup("#lowStockBanner").isManaged()).isFalse();
    }

    @Test
    @DisplayName("starts empty and shows a chip for each category")
    void startsEmptyWithItsCategories() throws Exception {
        Parent root = open();

        assertThat(names(root)).isEmpty();
        assertThat(root.lookupAll(".filter-chip")).extracting(chip -> ((ToggleButton) chip).getText())
            .containsExactly("Cabello", "Piel", "Uñas", "Maquillaje", "Accesorios");
    }

    @Test
    @DisplayName("searches by name without caring for accents or capitals")
    void searches() throws Exception {
        registerSomeProducts();
        Parent root = open();

        FxTest.onFxThread(() -> {
            ((TextField) root.lookup("#searchField")).setText("CHAMPU");
            return null;
        });

        assertThat(names(root)).containsExactly("Champú Nutritivo");
    }

    @Test
    @DisplayName("filters by category with the chips, and shows everything again when the chip is pressed twice")
    void filtersByCategory() throws Exception {
        registerSomeProducts();
        Parent root = open();

        FxTest.onFxThread(() -> {
            chip(root, "Uñas").fire();
            return null;
        });
        assertThat(names(root)).containsExactlyInAnyOrder("Esmalte Rosa Nude", "Aceite de Cutículas");

        FxTest.onFxThread(() -> {
            chip(root, "Uñas").fire();
            return null;
        });
        assertThat(names(root)).hasSize(3);
    }

    @Test
    @DisplayName("shows only the products to restock when asked")
    void onlyRestock() throws Exception {
        registerSomeProducts();
        Parent root = open();
        Button show = (Button) root.lookup("#lowStockButton");

        FxTest.onFxThread(() -> {
            show.fire();
            return null;
        });

        assertThat(names(root)).containsExactlyInAnyOrder("Champú Nutritivo", "Esmalte Rosa Nude");
        assertThat(show.getText()).isEqualTo("Ver todos");
    }

    @Test
    @DisplayName("registers a product from the form and shows it in the table")
    void registersAProduct() throws Exception {
        Parent root = open();

        register(root, "Champú Nutritivo", "Cabello", "28.000", "10", "5");

        assertThat(names(root)).containsExactly("Champú Nutritivo");
        assertThat(context.products().listAll()).singleElement().satisfies(product -> {
            assertThat(product.getPrice()).isEqualByComparingTo("28000");
            assertThat(product.getStock()).isEqualTo(5);
            assertThat(product.getCategory().getName()).isEqualTo("Cabello");
        });
        assertThat(((TextField) root.lookup("#nameField")).getText()).as("the form is ready for the next one").isEmpty();
        assertThat(((TextField) root.lookup("#initialStockField")).getText()).isEqualTo("0");
        assertThat(dialogs).isEmpty();
    }

    @Test
    @DisplayName("shows a repeated name under the name field and registers nothing")
    void repeatedName() throws Exception {
        context.products().register(new ProductData("Champú Nutritivo", HAIR, 28000, 10), 5);
        Parent root = open();

        register(root, "Champú Nutritivo", "Cabello", "30000", "10", "5");

        assertThat(message(root, "#nameMessage")).isEqualTo("Ya existe un producto llamado Champú Nutritivo.");
        assertThat(marked(root, "#nameField")).isTrue();
        assertThat(names(root)).hasSize(1);
        assertThat(dialogs).as("a field error does not open a dialog").isEmpty();
    }

    @Test
    @DisplayName("shows a price that is zero or less under the price field")
    void priceMustBePositive() throws Exception {
        Parent root = open();

        register(root, "Esmalte", "Uñas", "0", "5", "3");

        assertThat(message(root, "#priceMessage")).isEqualTo("El campo precio debe ser mayor que cero.");
        assertThat(marked(root, "#priceField")).isTrue();
        assertThat(names(root)).isEmpty();
    }

    @Test
    @DisplayName("shows a price that is not a number under the price field")
    void priceMustBeANumber() throws Exception {
        Parent root = open();

        register(root, "Esmalte", "Uñas", "barato", "5", "3");

        assertThat(message(root, "#priceMessage")).contains("campo precio", "número");
        assertThat(marked(root, "#priceField")).isTrue();
    }

    @Test
    @DisplayName("asks for the name first, then the category")
    void asksInOrder() throws Exception {
        Parent root = open();

        register(root, "", null, "5000", "5", "3");
        assertThat(message(root, "#nameMessage")).isEqualTo("El campo nombre es obligatorio.");

        register(root, "Esmalte", null, "5000", "5", "3");
        assertThat(message(root, "#categoryMessage")).isEqualTo("El campo categoría es obligatorio.");
        assertThat(marked(root, "#nameField")).as("the name error went away").isFalse();
    }

    @Test
    @DisplayName("shows a negative minimum and a negative initial stock under their own fields")
    void stockErrors() throws Exception {
        Parent root = open();

        register(root, "Esmalte", "Uñas", "5000", "-1", "3");
        assertThat(message(root, "#minimumMessage")).isEqualTo("El campo stock mínimo no puede ser negativo.");
        assertThat(marked(root, "#initialStockField")).isFalse();

        register(root, "Esmalte", "Uñas", "5000", "5", "-3");
        assertThat(message(root, "#initialStockMessage")).isEqualTo("El campo stock no puede ser negativo.");
        assertThat(marked(root, "#minimumField")).isFalse();
    }

    @Test
    @DisplayName("Cancelar empties the form and removes the errors")
    void cancelClearsTheForm() throws Exception {
        Parent root = open();
        register(root, "", "Uñas", "5000", "5", "3");
        assertThat(marked(root, "#nameField")).isTrue();

        press(root, "#cancelButton");

        assertThat(marked(root, "#nameField")).isFalse();
        assertThat(((TextField) root.lookup("#priceField")).getText()).isEmpty();
    }

    @Test
    @DisplayName("edits a product: the form is filled, the stock is left alone and the table shows the change")
    void editsAProduct() throws Exception {
        context.products().register(new ProductData("Champú Nutritivo", HAIR, 28000, 10), 5);
        Parent root = open();

        fire(iconButton(root, "Editar Champú Nutritivo"));

        assertThat(((Label) root.lookup("#formTitle")).getText()).isEqualTo("Editar producto");
        assertThat(((TextField) root.lookup("#nameField")).getText()).isEqualTo("Champú Nutritivo");
        assertThat(((TextField) root.lookup("#priceField")).getText()).isEqualTo("28000");
        assertThat(root.lookup("#initialStockBox").isVisible()).as("the stock is changed with the adjustment").isFalse();

        fill(root, "#priceField", "30.000");
        press(root, "#saveButton");

        assertThat(context.products().listAll()).singleElement().satisfies(product -> {
            assertThat(product.getPrice()).isEqualByComparingTo("30000");
            assertThat(product.getStock()).isEqualTo(5);
        });
        assertThat(((Label) root.lookup("#formTitle")).getText()).isEqualTo("Registrar producto");
    }

    @Test
    @DisplayName("a product can keep its own name but not take the name of another one")
    void editingNames() throws Exception {
        context.products().register(new ProductData("Champú Nutritivo", HAIR, 28000, 10), 5);
        context.products().register(new ProductData("Esmalte Rosa Nude", NAILS, 18000, 8), 3);
        Parent root = open();
        fire(iconButton(root, "Editar Esmalte Rosa Nude"));

        fill(root, "#nameField", "Champú Nutritivo");
        press(root, "#saveButton");
        assertThat(message(root, "#nameMessage")).isEqualTo("Ya existe un producto llamado Champú Nutritivo.");

        fill(root, "#nameField", "Esmalte Rosa Nude");
        press(root, "#saveButton");
        assertThat(marked(root, "#nameField")).isFalse();
        assertThat(((Label) root.lookup("#formTitle")).getText()).isEqualTo("Registrar producto");
    }

    @Test
    @DisplayName("adds and removes units and updates the warning")
    void adjustsTheStock() throws Exception {
        context.products().register(new ProductData("Champú Nutritivo", HAIR, 28000, 10), 5);
        Parent root = open();
        assertThat(root.lookup("#lowStockBanner").isVisible()).isTrue();

        pick(root, "Champú Nutritivo");
        fill(root, "#quantityField", "10");
        press(root, "#adjustButton");

        assertThat(context.products().listAll().get(0).getStock()).isEqualTo(15);
        assertThat(((TextField) root.lookup("#quantityField")).getText()).isEqualTo("1");
        assertThat(root.lookup("#lowStockBanner").isVisible()).as("15 is above the minimum of 10").isFalse();

        fill(root, "#quantityField", "-4");
        press(root, "#adjustButton");
        assertThat(context.products().listAll().get(0).getStock()).isEqualTo(11);
    }

    @Test
    @DisplayName("shows each adjustment mistake under its own field")
    void adjustmentErrors() throws Exception {
        context.products().register(new ProductData("Champú Nutritivo", HAIR, 28000, 10), 5);
        Parent root = open();

        press(root, "#adjustButton");
        assertThat(message(root, "#adjustProductMessage")).isEqualTo("El campo producto es obligatorio.");

        pick(root, "Champú Nutritivo");
        fill(root, "#quantityField", "0");
        press(root, "#adjustButton");
        assertThat(message(root, "#quantityMessage")).isEqualTo("El ajuste de stock no puede ser cero.");

        fill(root, "#quantityField", "mucho");
        press(root, "#adjustButton");
        assertThat(message(root, "#quantityMessage")).isEqualTo("El campo cantidad debe ser un número entero.");

        fill(root, "#quantityField", "-99");
        press(root, "#adjustButton");
        assertThat(message(root, "#quantityMessage")).contains("Stock insuficiente de Champú Nutritivo");
        assertThat(context.products().listAll().get(0).getStock()).as("nothing changed").isEqualTo(5);
        assertThat(dialogs).isEmpty();
    }

    @Test
    @DisplayName("the plus and minus buttons move the quantity one unit and can go below zero")
    void stepper() throws Exception {
        Parent root = open();
        TextField quantity = (TextField) root.lookup("#quantityField");

        press(root, "#plusButton");
        assertThat(quantity.getText()).isEqualTo("2");
        press(root, "#minusButton");
        press(root, "#minusButton");
        press(root, "#minusButton");

        assertThat(quantity.getText()).isEqualTo("-1");
    }

    @Test
    @DisplayName("choosing a row in the table picks that product to adjust")
    void selectionPicksTheProduct() throws Exception {
        registerSomeProducts();
        Parent root = open();
        @SuppressWarnings("unchecked")
        TableView<Product> table = (TableView<Product>) root.lookup("#productsTable");
        @SuppressWarnings("unchecked")
        ComboBox<Product> box = (ComboBox<Product>) root.lookup("#adjustProductBox");

        FxTest.onFxThread(() -> {
            table.getSelectionModel().select(0);
            return null;
        });

        assertThat(box.getValue().getName()).isEqualTo(table.getItems().get(0).getName());
    }

    @Test
    @DisplayName("deactivating asks first, hides the product from the adjustment and can be undone")
    void deactivatesAndReactivates() throws Exception {
        context.products().register(new ProductData("Champú Nutritivo", HAIR, 28000, 10), 5);
        ViewLoader.Loaded<ProductsController> screen = openScreen();
        Parent root = screen.root();

        fire(iconButton(root, "Desactivar Champú Nutritivo"));
        assertThat(context.products().listAll().get(0).isActive()).as("the question was not accepted").isTrue();

        acceptQuestions(screen);
        fire(iconButton(root, "Desactivar Champú Nutritivo"));
        assertThat(context.products().listAll().get(0).isActive()).isFalse();
        assertThat(names(root)).as("it stays in the list, as inactive").containsExactly("Champú Nutritivo");
        @SuppressWarnings("unchecked")
        ComboBox<Product> box = (ComboBox<Product>) root.lookup("#adjustProductBox");
        assertThat(box.getItems()).as("it cannot be adjusted while inactive").isEmpty();

        fire(iconButton(root, "Reactivar Champú Nutritivo"));
        assertThat(context.products().listAll().get(0).isActive()).isTrue();
    }
}
