package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.model.Customer;
import com.maisonglow.service.ContactData;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

@DisplayName("Customers view tests")
class CustomersViewTest {

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
        context.customers().register(new ContactData("1001", "Ana García", "3001112233", "ana@correo.com"));
        Customer laura = context.customers().register(new ContactData("1002", "Laura Méndez", "3124445566", "laura@correo.com"));
        context.customers().registerNoShow(laura.getId());
    }

    private Parent open() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        return FxTest.onFxThread(() -> {
            Parent root = ViewLoader.load(CustomersViewTest.class.getResource("/com/maisonglow/view/CustomersView.fxml"), context);
            new Scene(root, 1100, 700);
            root.applyCss();
            root.layout();
            return root;
        });
    }

    @SuppressWarnings("unchecked")
    private static List<String> names(Parent root) throws Exception {
        TableView<Customer> table = (TableView<Customer>) root.lookup("#customersTable");
        return FxTest.onFxThread(() -> table.getItems().stream().map(Customer::getName).toList());
    }

    private static String count(Parent root) throws Exception {
        return FxTest.onFxThread(() -> ((Label) root.lookup("#countLabel")).getText());
    }

    private static void type(Parent root, String text) throws Exception {
        FxTest.onFxThread(() -> {
            ((TextField) root.lookup("#searchField")).setText(text);
            return null;
        });
    }

    private static void choose(Parent root, String chipId) throws Exception {
        FxTest.onFxThread(() -> {
            ((ToggleButton) root.lookup("#" + chipId)).fire();
            return null;
        });
    }

    @Test
    @DisplayName("lists every customer and counts them")
    void listsCustomers() throws Exception {
        Parent root = open();

        assertThat(names(root)).containsExactlyInAnyOrder("Ana García", "Laura Méndez");
        assertThat(count(root)).isEqualTo("Mostrando 2 de 2 clientes");
    }

    @Test
    @DisplayName("the search finds by name, document or phone and the count follows")
    void searches() throws Exception {
        Parent root = open();

        type(root, "GARCIA");
        assertThat(names(root)).containsExactly("Ana García");
        assertThat(count(root)).isEqualTo("Mostrando 1 de 2 clientes");

        type(root, "312444");
        assertThat(names(root)).containsExactly("Laura Méndez");

        type(root, "1001");
        assertThat(names(root)).containsExactly("Ana García");
    }

    @Test
    @DisplayName("the chips keep who missed appointments or registered recently, and one always stays chosen")
    void filtersWithChips() throws Exception {
        Parent root = open();

        choose(root, "noShowsChip");
        assertThat(names(root)).containsExactly("Laura Méndez");

        choose(root, "newChip");
        assertThat(names(root)).containsExactlyInAnyOrder("Ana García", "Laura Méndez");

        choose(root, "newChip");
        assertThat(((ToggleButton) root.lookup("#newChip")).isSelected()).isTrue();
    }

    @Test
    @DisplayName("a customer with no-shows is marked in the table")
    void marksNoShows() throws Exception {
        Parent root = open();

        assertThat(root.lookupAll(".row-noshow")).hasSize(1);
        assertThat(root.lookupAll(".chip-warning")).extracting(node -> ((Label) node).getText()).containsExactly("1");
    }
}
