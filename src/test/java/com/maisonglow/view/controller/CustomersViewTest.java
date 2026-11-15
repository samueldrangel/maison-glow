package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Sale;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.ProductData;
import com.maisonglow.service.SaleService;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
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

    @SuppressWarnings("unchecked")
    private static void select(Parent root, String name) throws Exception {
        TableView<Customer> table = (TableView<Customer>) root.lookup("#customersTable");
        FxTest.onFxThread(() -> {
            table.getSelectionModel().select(table.getItems().stream().filter(c -> name.equals(c.getName())).findFirst().orElseThrow());
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

    @SuppressWarnings("unchecked")
    private static List<String> chips(Parent root) throws Exception {
        return FxTest.onFxThread(() -> ((HBox) root.lookup("#statsChips")).getChildren().stream()
            .map(node -> ((Label) node).getText()).toList());
    }

    /** The labels of each purchase row of the panel, one list per row. */
    private static List<List<String>> purchaseRows(Parent root) throws Exception {
        return FxTest.onFxThread(() -> ((VBox) root.lookup("#purchasesBox")).getChildren().stream()
            .map(row -> row.lookupAll(".label").stream().map(node -> ((Label) node).getText()).toList()).toList());
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
    @DisplayName("selecting a customer brings in a side panel with its purchases, appointments and no-shows")
    void showsStatisticsInAPanel() throws Exception {
        Customer ana = context.customers().findByDocument("1001").orElseThrow();
        var shampoo = context.products().register(new ProductData("Champú Nutritivo", 1, 28000, 10), 5);
        Sale sale = new Sale(ana);
        SaleService.addLine(sale, shampoo, 2);
        context.sales().register(sale);
        Parent root = open();
        assertThat(root.lookup("#statsPanel").isManaged()).isFalse();

        select(root, "Ana García");

        assertThat(panelManaged(root, "statsPanel", true)).isTrue();
        assertThat(text(root, "statsName")).isEqualTo("Ana García");
        assertThat(text(root, "statsInitials")).isEqualTo("AG");
        assertThat(text(root, "purchasesValue")).isEqualTo("1");
        assertThat(text(root, "spentValue")).isEqualTo(CurrencyFormatter.format(new BigDecimal("56000")));
        assertThat(text(root, "appointmentsValue")).isEqualTo("0");
        assertThat(text(root, "averageValue")).isEqualTo(CurrencyFormatter.format(new BigDecimal("56000")));
        select(root, "Laura Méndez");
        assertThat(text(root, "statsName")).isEqualTo("Laura Méndez");
        assertThat(text(root, "purchasesValue")).isEqualTo("0");
    }

    @Test
    @DisplayName("Nuevo cliente and the pencil take the statistics out to make room for the form")
    void formTakesTheSideFromTheStatistics() throws Exception {
        Parent root = open();
        select(root, "Ana García");
        assertThat(panelManaged(root, "statsPanel", true)).isTrue();

        press(root, "newButton");

        assertThat(panelManaged(root, "formPanel", true)).isTrue();
        assertThat(panelManaged(root, "statsPanel", false)).isFalse();
    }

    @Test
    @DisplayName("selecting a customer while the form is open closes the form and shows the statistics")
    void statisticsTakeTheSideFromTheForm() throws Exception {
        Parent root = open();
        press(root, "newButton");
        assertThat(panelManaged(root, "formPanel", true)).isTrue();

        select(root, "Laura Méndez");

        assertThat(panelManaged(root, "statsPanel", true)).isTrue();
        assertThat(panelManaged(root, "formPanel", false)).isFalse();
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

    @Test
    @DisplayName("the profile tells who the customer is: document, contact, since when, and that the customer is new")
    void profileShowsWhoTheCustomerIs() throws Exception {
        Parent root = open();

        select(root, "Ana García");

        assertThat(text(root, "statsDocument")).isEqualTo("Documento 1001");
        assertThat(text(root, "statsPhone")).isEqualTo("3001112233");
        assertThat(text(root, "statsEmail")).isEqualTo("ana@correo.com");
        assertThat(text(root, "statsSince")).startsWith("Cliente desde ");
        assertThat(chips(root)).containsExactly("Nuevo", "Sin inasistencias");
    }

    @Test
    @DisplayName("a customer who missed appointments gets a warning chip and a zero attendance; without any there is none to show")
    void profileWarnsAboutNoShows() throws Exception {
        Parent root = open();

        select(root, "Laura Méndez");

        assertThat(chips(root)).containsExactly("Nuevo", "1 inasistencia");
        assertThat(text(root, "attendanceValue")).isEqualTo("0 %");
        assertThat(((ProgressBar) root.lookup("#attendanceBar")).getProgress()).isZero();

        select(root, "Ana García");
        assertThat(text(root, "attendanceValue")).isEqualTo("Sin citas todavía");
    }

    @Test
    @DisplayName("the panel lists the latest purchases of the customer, newest first and at most three")
    void listsTheLatestPurchases() throws Exception {
        Customer ana = context.customers().findByDocument("1001").orElseThrow();
        var shampoo = context.products().register(new ProductData("Champú Nutritivo", 1, 28000, 10), 20);
        for (int units = 1; units <= 4; units++) {
            Sale sale = new Sale(ana);
            SaleService.addLine(sale, shampoo, units);
            context.sales().register(sale);
        }
        Parent root = open();

        select(root, "Ana García");

        assertThat(purchaseRows(root)).hasSize(3);
        assertThat(purchaseRows(root).get(0)).contains("Champú Nutritivo ×4", CurrencyFormatter.format(new BigDecimal("112000")));
    }

    @Test
    @DisplayName("a customer who has not bought yet gets a note instead of an empty card")
    void saysWhenThereAreNoPurchases() throws Exception {
        Parent root = open();

        select(root, "Ana García");

        assertThat(FxTest.onFxThread(() -> ((Label) ((VBox) root.lookup("#purchasesBox")).getChildren().get(0)).getText()))
            .isEqualTo("Aún no ha comprado.");
    }

    @Test
    @DisplayName("the note about the document shows only while editing")
    void noteAboutTheDocumentShowsWhileEditing() throws Exception {
        Parent root = open();
        assertThat(root.lookup("#editNote").isVisible()).isFalse();

        click(root, "Editar Ana García");
        assertThat(root.lookup("#editNote").isVisible()).isTrue();

        press(root, "cancelButton");
        assertThat(root.lookup("#editNote").isVisible()).isFalse();
    }
}
