package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.service.ProductData;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Products view tests")
class ProductsViewTest {

    private static final int HAIR = 1;
    private static final int NAILS = 3;

    private static boolean toolkit;

    @TempDir
    Path data;

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
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        return FxTest.onFxThread(() -> {
            Parent root = ViewLoader.load(ProductsViewTest.class.getResource("/com/maisonglow/view/ProductsView.fxml"), context);
            new Scene(root, 1100, 700);
            root.applyCss();
            root.layout();
            return root;
        });
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
}
