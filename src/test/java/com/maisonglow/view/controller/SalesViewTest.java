package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Sellable;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.ProductData;
import com.maisonglow.service.TreatmentData;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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

@DisplayName("Sales view tests")
class SalesViewTest {

    private static final int HAIR = 1;

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
        context.customers().register(new ContactData("1001", "Ana Pérez", "3001112233", "ana@correo.com"));
        context.products().register(new ProductData("Champú Nutritivo", HAIR, 28000, 10), 5);
        context.products().register(new ProductData("Mascarilla Reparadora", HAIR, 35000, 4), 0);
        context.treatments().register(new TreatmentData("Corte de cabello", "Corte y peinado", 45000, 60));
    }

    private Parent open() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        return FxTest.onFxThread(() -> {
            Parent root = ViewLoader.load(SalesViewTest.class.getResource("/com/maisonglow/view/SalesView.fxml"), context);
            new Scene(root, 1100, 700);
            root.applyCss();
            root.layout();
            return root;
        });
    }

    @SuppressWarnings("unchecked")
    private static List<String> catalog(Parent root) throws Exception {
        TableView<Sellable> table = (TableView<Sellable>) root.lookup("#catalogTable");
        return FxTest.onFxThread(() -> table.getItems().stream().map(Sellable::getName).toList());
    }

    private static void choose(Parent root, String chipId) throws Exception {
        FxTest.onFxThread(() -> {
            ((ToggleButton) root.lookup("#" + chipId)).fire();
            return null;
        });
    }

    @Test
    @DisplayName("offers the registered customers and starts with the products of the catalog")
    void listsCustomersAndProducts() throws Exception {
        Parent root = open();

        @SuppressWarnings("unchecked")
        ComboBox<Customer> customers = (ComboBox<Customer>) root.lookup("#customerBox");
        assertThat(customers.getItems()).extracting(Customer::getName).containsExactly("Ana Pérez");
        assertThat(catalog(root)).containsExactlyInAnyOrder("Champú Nutritivo", "Mascarilla Reparadora");
    }

    @Test
    @DisplayName("the treatments chip shows the treatments and the products chip brings the products back")
    void switchesBetweenProductsAndTreatments() throws Exception {
        Parent root = open();

        choose(root, "treatmentsChip");
        assertThat(catalog(root)).containsExactly("Corte de cabello");

        choose(root, "productsChip");
        assertThat(catalog(root)).hasSize(2);
    }

    @Test
    @DisplayName("one chip always stays chosen")
    void keepsOneChipChosen() throws Exception {
        Parent root = open();

        choose(root, "productsChip");

        assertThat(((ToggleButton) root.lookup("#productsChip")).isSelected()).isTrue();
    }

    @Test
    @DisplayName("the search ignores case and accents")
    void searches() throws Exception {
        Parent root = open();

        FxTest.onFxThread(() -> {
            ((TextField) root.lookup("#searchField")).setText("CHAMPU");
            return null;
        });

        assertThat(catalog(root)).containsExactly("Champú Nutritivo");
    }

    @Test
    @DisplayName("a product with no units cannot be added")
    void outOfStockCannotBeAdded() throws Exception {
        Parent root = open();

        List<Button> adders = FxTest.onFxThread(() -> root.lookupAll(".icon-button").stream().map(Button.class::cast).toList());

        assertThat(adders).extracting(Button::isDisabled).containsExactlyInAnyOrder(false, true);
    }
}
