package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.model.Customer;
import com.maisonglow.service.ContactData;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
import java.util.function.Predicate;
import java.util.function.Supplier;

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
        return open(true);
    }

    /** Opens the screen; a confirmation is answered as the user would, with "Confirmar" or by cancelling. */
    private Parent open(boolean confirm) throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<CustomersController> loaded = ViewLoader.loadWithController(
                CustomersViewTest.class.getResource("/com/maisonglow/view/CustomersView.fxml"), context);
            loaded.controller().useAlerts(new AlertPresenter(() -> null, alert -> alert.getButtonTypes().stream()
                .filter(button -> confirm && "Confirmar".equals(button.getText())).findFirst()));
            Parent root = loaded.root();
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

    private static void fill(Parent root, String document, String name, String phone, String email) throws Exception {
        FxTest.onFxThread(() -> {
            ((TextField) root.lookup("#documentField")).setText(document);
            ((TextField) root.lookup("#nameField")).setText(name);
            ((TextField) root.lookup("#phoneField")).setText(phone);
            ((TextField) root.lookup("#emailField")).setText(email);
            return null;
        });
    }

    private static void press(Parent root, String id) throws Exception {
        FxTest.onFxThread(() -> {
            ((Button) root.lookup("#" + id)).fire();
            return null;
        });
    }

    private static String message(Parent root, String id) throws Exception {
        Label label = (Label) root.lookup("#" + id);
        return FxTest.onFxThread(() -> label.isVisible() ? label.getText() : "");
    }

    /** Presses the icon button of the table that has that description, as a user would click it. */
    private static void click(Parent root, String description) throws Exception {
        FxTest.onFxThread(() -> {
            root.lookup("#customersTable").lookupAll(".icon-button").stream().map(Button.class::cast)
                .filter(button -> description.equals(button.getAccessibleText())).findFirst().orElseThrow().fire();
            root.applyCss();
            root.layout();
            return null;
        });
    }

    private static String text(Parent root, String id) throws Exception {
        return FxTest.onFxThread(() -> ((Label) root.lookup("#" + id)).getText());
    }

    /** The animation runs on its own; this waits for the panel to reach the state the test expects. */
    private static boolean panelManaged(Parent root, String panelId, boolean expected) throws Exception {
        ScrollPane panel = (ScrollPane) root.lookup("#" + panelId);
        Supplier<Boolean> reading = () -> panel.isManaged();
        Predicate<Boolean> done = managed -> managed == expected;
        boolean value = FxTest.onFxThread(reading);
        for (int attempt = 0; attempt < 80 && !done.test(value); attempt++) {
            Thread.sleep(25);
            value = FxTest.onFxThread(reading);
        }
        return value;
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

    @Test
    @DisplayName("registering a customer adds it to the table and clears the form")
    void registers() throws Exception {
        Parent root = open();

        fill(root, "1003", "Paula Andrés", "3207778899", "paula@correo.com");
        press(root, "saveButton");

        assertThat(names(root)).contains("Paula Andrés");
        assertThat(count(root)).isEqualTo("Mostrando 3 de 3 clientes");
        assertThat(((TextField) root.lookup("#nameField")).getText()).isEmpty();
        assertThat(context.customers().findByDocument("1003")).isPresent();
    }

    @Test
    @DisplayName("a repeated document is rejected under the document field")
    void rejectsRepeatedDocument() throws Exception {
        Parent root = open();

        fill(root, "1001", "Otra Persona", "3001110000", "otra@correo.com");
        press(root, "saveButton");

        assertThat(message(root, "documentMessage")).contains("1001");
        assertThat(names(root)).hasSize(2);
    }

    @Test
    @DisplayName("each wrong field is reported under its own field")
    void reportsEachField() throws Exception {
        Parent root = open();

        fill(root, "", "Paula Andrés", "3207778899", "paula@correo.com");
        press(root, "saveButton");
        assertThat(message(root, "documentMessage")).isNotEmpty();

        fill(root, "1003", "", "3207778899", "paula@correo.com");
        press(root, "saveButton");
        assertThat(message(root, "nameMessage")).isNotEmpty();

        fill(root, "1003", "Paula Andrés", "12", "paula@correo.com");
        press(root, "saveButton");
        assertThat(message(root, "phoneMessage")).contains("entre 7 y 12");

        fill(root, "1003", "Paula Andrés", "3207778899", "sin-arroba");
        press(root, "saveButton");
        assertThat(message(root, "emailMessage")).contains("formato");
        assertThat(names(root)).hasSize(2);
    }

    @Test
    @DisplayName("editing fills the form, keeps the document fixed and saves the changes")
    void edits() throws Exception {
        Parent root = open();

        click(root, "Editar Ana García");
        assertThat(panelManaged(root, "formPanel", true)).isTrue();
        assertThat(((TextField) root.lookup("#documentField")).isDisabled()).isTrue();
        assertThat(((TextField) root.lookup("#nameField")).getText()).isEqualTo("Ana García");
        assertThat(text(root, "formTitle")).isEqualTo("Editar cliente");
        fill(root, "1001", "Ana María García", "3001112233", "ana@correo.com");
        press(root, "saveButton");

        assertThat(names(root)).contains("Ana María García").doesNotContain("Ana García");
        assertThat(context.customers().findByDocument("1001").orElseThrow().getName()).isEqualTo("Ana María García");
        assertThat(((TextField) root.lookup("#documentField")).isDisabled()).isFalse();
        assertThat(text(root, "formTitle")).isEqualTo("Registrar cliente");
        assertThat(panelManaged(root, "formPanel", false)).isFalse();
    }

    @Test
    @DisplayName("deleting asks first and removes the customer when confirmed")
    void deletesWhenConfirmed() throws Exception {
        Parent root = open(true);

        click(root, "Eliminar Ana García");

        assertThat(names(root)).containsExactly("Laura Méndez");
        assertThat(context.customers().findByDocument("1001")).isEmpty();
    }

    @Test
    @DisplayName("cancelling the confirmation keeps the customer")
    void keepsWhenCancelled() throws Exception {
        Parent root = open(false);

        click(root, "Eliminar Ana García");

        assertThat(names(root)).hasSize(2);
        assertThat(context.customers().findByDocument("1001")).isPresent();
    }

    @Test
    @DisplayName("the form is hidden at first; Nuevo cliente brings it in and Cancelar takes it out and empties it")
    void newOpensAndCancelCloses() throws Exception {
        Parent root = open();
        assertThat(root.lookup("#formPanel").isManaged()).isFalse();

        press(root, "newButton");
        assertThat(panelManaged(root, "formPanel", true)).isTrue();
        assertThat(text(root, "formTitle")).isEqualTo("Registrar cliente");
        fill(root, "1003", "Paula Andrés", "3207778899", "paula@correo.com");

        press(root, "cancelButton");
        assertThat(panelManaged(root, "formPanel", false)).isFalse();
        assertThat(((TextField) root.lookup("#documentField")).getText()).isEmpty();
        assertThat(((TextField) root.lookup("#emailField")).getText()).isEmpty();
    }

    @Test
    @DisplayName("a rejected save keeps the form open to fix the mistake")
    void rejectedSaveKeepsItOpen() throws Exception {
        Parent root = open();
        press(root, "newButton");
        fill(root, "1001", "Otra Persona", "3001110000", "otra@correo.com");

        press(root, "saveButton");

        assertThat(message(root, "documentMessage")).isNotEmpty();
        assertThat(panelManaged(root, "formPanel", true)).isTrue();
    }
}
