package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.model.Customer;
import com.maisonglow.model.SaleLine;
import com.maisonglow.model.Sale;
import com.maisonglow.model.Sellable;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.ProductData;
import com.maisonglow.service.SimulatedEmailNotifier;
import com.maisonglow.service.TreatmentData;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.util.InvoiceFormatter;
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

import java.math.BigDecimal;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
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
    private final List<String> invoices = new ArrayList<>();
    private SimulatedEmailNotifier mail;

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
        return open(false);
    }

    /** Opens the screen; the invoice is recorded instead of shown, and the user answers whether to send it. */
    private Parent open(boolean sendInvoice) throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        mail = new SimulatedEmailNotifier(new InvoiceFormatter("Maison Glow"), new PrintStream(java.io.OutputStream.nullOutputStream()));
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<SalesController> loaded = ViewLoader.loadWithController(
                SalesViewTest.class.getResource("/com/maisonglow/view/SalesView.fxml"), context);
            loaded.controller().useInvoice(invoice -> invoices.add(invoice) && sendInvoice, mail);
            Parent root = loaded.root();
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
            root.applyCss();
            root.layout();
            return null;
        });
    }

    /** Presses the icon button of a table that has that description, as a user would click it. */
    private static void click(Parent root, String tableId, String description) throws Exception {
        FxTest.onFxThread(() -> {
            root.lookup("#" + tableId).lookupAll(".icon-button").stream().map(Button.class::cast)
                .filter(button -> description.equals(button.getAccessibleText())).findFirst().orElseThrow().fire();
            root.applyCss();
            root.layout();
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private static List<SaleLine> cartLines(Parent root) throws Exception {
        TableView<SaleLine> table = (TableView<SaleLine>) root.lookup("#cartTable");
        return FxTest.onFxThread(() -> List.copyOf(table.getItems()));
    }

    private static String total(Parent root) throws Exception {
        return FxTest.onFxThread(() -> ((Label) root.lookup("#totalLabel")).getText());
    }

    private static String money(String amount) {
        return CurrencyFormatter.format(new BigDecimal(amount));
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

    @Test
    @DisplayName("adding the same product twice makes one line of two units and the total follows")
    void addingTwiceMergesTheLine() throws Exception {
        Parent root = open();

        click(root, "catalogTable", "Agregar Champú Nutritivo");
        click(root, "catalogTable", "Agregar Champú Nutritivo");

        assertThat(cartLines(root)).hasSize(1);
        assertThat(cartLines(root).get(0).getQuantity()).isEqualTo(2);
        assertThat(total(root)).isEqualTo(money("56000"));
    }

    @Test
    @DisplayName("products and treatments go to the same cart")
    void mixesProductsAndTreatments() throws Exception {
        Parent root = open();

        click(root, "catalogTable", "Agregar Champú Nutritivo");
        choose(root, "treatmentsChip");
        click(root, "catalogTable", "Agregar Corte de cabello");

        assertThat(cartLines(root)).extracting(line -> line.getItem().getName())
            .containsExactly("Champú Nutritivo", "Corte de cabello");
        assertThat(total(root)).isEqualTo(money("73000"));
    }

    @Test
    @DisplayName("plus and minus change the quantity of a line, and a line stops at one unit")
    void changesTheQuantity() throws Exception {
        Parent root = open();
        click(root, "catalogTable", "Agregar Champú Nutritivo");

        click(root, "cartTable", "Agregar una unidad de Champú Nutritivo");
        click(root, "cartTable", "Agregar una unidad de Champú Nutritivo");
        click(root, "cartTable", "Quitar una unidad de Champú Nutritivo");

        assertThat(cartLines(root).get(0).getQuantity()).isEqualTo(2);
        assertThat(total(root)).isEqualTo(money("56000"));
    }

    @Test
    @DisplayName("the trash button takes the line out and the total goes back to zero")
    void removesALine() throws Exception {
        Parent root = open();
        click(root, "catalogTable", "Agregar Champú Nutritivo");

        click(root, "cartTable", "Quitar Champú Nutritivo de la venta");

        assertThat(cartLines(root)).isEmpty();
        assertThat(total(root)).isEqualTo(money("0"));
    }

    @Test
    @DisplayName("Vaciar is disabled while the cart is empty and empties it otherwise")
    void clearsTheCart() throws Exception {
        Parent root = open();
        Button clear = (Button) root.lookup("#clearButton");
        assertThat(clear.isDisabled()).isTrue();

        click(root, "catalogTable", "Agregar Champú Nutritivo");
        assertThat(clear.isDisabled()).isFalse();
        FxTest.onFxThread(() -> {
            clear.fire();
            return null;
        });

        assertThat(cartLines(root)).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static void pickCustomer(Parent root) throws Exception {
        FxTest.onFxThread(() -> {
            ((ComboBox<Customer>) root.lookup("#customerBox")).getSelectionModel().selectFirst();
            return null;
        });
    }

    private static void charge(Parent root) throws Exception {
        FxTest.onFxThread(() -> {
            ((Button) root.lookup("#chargeButton")).fire();
            return null;
        });
    }

    private static String message(Parent root, String id) throws Exception {
        Label label = (Label) root.lookup("#" + id);
        return FxTest.onFxThread(() -> label.isVisible() ? label.getText() : "");
    }

    @Test
    @DisplayName("charging without a customer or without lines is rejected next to what is missing")
    void rejectsAnIncompleteSale() throws Exception {
        Parent root = open();
        click(root, "catalogTable", "Agregar Champú Nutritivo");

        charge(root);
        assertThat(message(root, "customerMessage")).contains("cliente");

        pickCustomer(root);
        click(root, "cartTable", "Quitar Champú Nutritivo de la venta");
        charge(root);
        assertThat(message(root, "cartMessage")).contains("al menos una línea");
        assertThat(invoices).isEmpty();
    }

    @Test
    @DisplayName("selling more than the stock shows the error and changes nothing")
    void rejectsMoreThanTheStock() throws Exception {
        Parent root = open();
        pickCustomer(root);
        click(root, "catalogTable", "Agregar Champú Nutritivo");
        for (int unit = 0; unit < 5; unit++) {
            click(root, "cartTable", "Agregar una unidad de Champú Nutritivo");
        }

        charge(root);

        assertThat(message(root, "cartMessage")).contains("Stock insuficiente");
        assertThat(cartLines(root).get(0).getQuantity()).isEqualTo(6);
        assertThat(context.products().findById(1).getStock()).isEqualTo(5);
        assertThat(context.sales().listByCustomer(1)).isEmpty();
    }

    @Test
    @DisplayName("a valid sale discounts the stock, empties the cart, shows the invoice and sends it by e-mail on request")
    void registersAndSendsTheInvoice() throws Exception {
        Parent root = open(true);
        pickCustomer(root);
        click(root, "catalogTable", "Agregar Champú Nutritivo");
        click(root, "cartTable", "Agregar una unidad de Champú Nutritivo");

        charge(root);

        List<Sale> sold = context.sales().listByCustomer(1);
        assertThat(sold).hasSize(1);
        assertThat(sold.get(0).getTotal()).isEqualByComparingTo("56000");
        assertThat(context.products().findById(1).getStock()).isEqualTo(3);
        assertThat(cartLines(root)).isEmpty();
        assertThat(invoices).singleElement().asString().contains("2 x Champú Nutritivo", "Ana Pérez");
        assertThat(mail.getSentMessages()).singleElement().asString().contains("Para: ana@correo.com");
    }

    @Test
    @DisplayName("the invoice is not sent when the user only closes it")
    void doesNotSendWhenClosed() throws Exception {
        Parent root = open(false);
        pickCustomer(root);
        click(root, "catalogTable", "Agregar Champú Nutritivo");

        charge(root);

        assertThat(invoices).hasSize(1);
        assertThat(mail.getSentMessages()).isEmpty();
    }
}
