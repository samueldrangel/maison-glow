package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.model.Commerce.Order;
import com.maisonglow.model.Commerce.OrderStatus;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Sale;
import com.maisonglow.service.SaleService;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.AppWindow;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLifecycle;
import com.maisonglow.view.ViewLoader;
import com.maisonglow.view.shop.ShopFixture;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Shopping in the customer area: add to the cart, confirm the order and see the purchases")
class CustomerShoppingViewTest {

    private static boolean toolkit;

    @TempDir
    Path data;

    private ShopFixture shop;
    private AppContext context;
    private final List<String> dialogs = new ArrayList<>();
    private final List<Runnable> held = new ArrayList<>();

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @BeforeEach
    void prepare() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        shop = new ShopFixture(data);
        context = shop.context;
    }

    private AlertPresenter presenter(boolean confirm) {
        return new AlertPresenter(() -> null, alert -> {
            dialogs.add(alert.getContentText());
            return alert.getButtonTypes().stream().filter(button -> confirm && "Confirmar".equals(button.getText())).findFirst()
                .or(() -> confirm ? Optional.empty() : Optional.empty());
        });
    }

    private <C> ViewLoader.Loaded<C> open(String fxml, java.util.function.Consumer<C> setUp) throws Exception {
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<C> loaded = ViewLoader.loadWithController(
                CustomerShoppingViewTest.class.getResource("/com/maisonglow/view/" + fxml), context);
            setUp.accept(loaded.controller());
            new Scene(loaded.root(), 1100, 700);
            loaded.root().applyCss();
            loaded.root().layout();
            return loaded;
        });
    }

    private ViewLoader.Loaded<CartController> openCart(boolean confirm) throws Exception {
        return open("CartView.fxml", (CartController controller) -> {
            controller.useExecutor(Runnable::run);
            controller.useAlerts(presenter(confirm));
        });
    }

    private ViewLoader.Loaded<ShopController> openShop() throws Exception {
        return open("ShopView.fxml", (ShopController controller) -> {
            controller.useExecutor(Runnable::run);
            controller.useAlerts(presenter(true));
        });
    }

    private ViewLoader.Loaded<MyPurchasesController> openPurchases(boolean confirm) throws Exception {
        return open("MyPurchasesView.fxml", (MyPurchasesController controller) -> {
            controller.useExecutor(Runnable::run);
            controller.useAlerts(presenter(confirm));
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

    private static List<String> texts(Parent root, String id) throws Exception {
        settle();
        return FxTest.onFxThread(() -> {
            List<String> found = new ArrayList<>();
            collect(root.lookup("#" + id), found);
            return found;
        });
    }

    private static String text(Parent root, String id) throws Exception {
        settle();
        return FxTest.onFxThread(() -> ((Label) root.lookup("#" + id)).getText());
    }

    private static boolean shown(Parent root, String id) throws Exception {
        settle();
        return FxTest.onFxThread(() -> root.lookup("#" + id).isVisible());
    }

    private static void press(Parent root, String id) throws Exception {
        FxTest.onFxThread(() -> {
            ((Button) root.lookup("#" + id)).fire();
            return null;
        });
        settle();
    }

    private static void pressButton(Parent root, String label, int index) throws Exception {
        settle();
        FxTest.onFxThread(() -> {
            root.lookupAll(".button").stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> label.equals(button.getText())).skip(index).findFirst().orElseThrow().fire();
            return null;
        });
        settle();
    }

    private List<Order> myOrders() throws Exception {
        return context.orders().mine();
    }

    private void fillCart() throws Exception {
        context.cart().add(context.catalog().findPublished(shop.serum.getId()), 2);
    }

    // ---- Tienda: agregar al carrito ----

    @Test
    @DisplayName("from the detail of a product the customer adds units to the cart, up to what is available")
    void addsFromTheShop() throws Exception {
        var screen = openShop();
        Parent root = screen.root();
        settle();
        List<Node> cards = FxTest.onFxThread(() -> new ArrayList<>(((Pane) root.lookup("#productsPane")).getChildren()));
        FxTest.onFxThread(() -> { cards.get(1).getOnMouseClicked().handle(null); return null; });
        settle();

        assertThat(text(root, "detailName")).isEqualTo("Sérum Vitamina C");
        press(root, "plusButton");
        press(root, "plusButton");
        assertThat(text(root, "quantityLabel")).isEqualTo("3");
        press(root, "addButton");

        assertThat(context.cart().quantityOf(shop.serum.getId())).isEqualTo(3);
        assertThat(text(root, "inCartLabel")).isEqualTo("En tu carrito: 3");
        assertThat(text(root, "quantityLabel")).isEqualTo("1");
    }

    @Test
    @DisplayName("the quantity cannot go above what is left, and when the cart holds everything adding is off")
    void quantityStopsAtTheAvailable() throws Exception {
        Parent root = openShop().root();
        settle();
        List<Node> cards = FxTest.onFxThread(() -> new ArrayList<>(((Pane) root.lookup("#productsPane")).getChildren()));
        FxTest.onFxThread(() -> { cards.get(0).getOnMouseClicked().handle(null); return null; });
        settle();
        assertThat(text(root, "detailName")).isEqualTo("Esmalte Rosa Nude");

        press(root, "plusButton");
        press(root, "plusButton");
        press(root, "plusButton");
        assertThat(text(root, "quantityLabel")).isEqualTo("2");
        press(root, "addButton");

        assertThat(context.cart().quantityOf(shop.esmalte.getId())).isEqualTo(2);
        assertThat(FxTest.onFxThread(() -> root.lookup("#addButton").isDisabled())).isTrue();
    }

    // ---- Carrito ----

    @Test
    @DisplayName("an empty cart says so and offers nothing to confirm")
    void emptyCart() throws Exception {
        Parent root = openCart(true).root();

        assertThat(shown(root, "emptyLabel")).isTrue();
        assertThat(text(root, "totalLabel")).isEqualTo("$ 0");
        assertThat(FxTest.onFxThread(() -> root.lookup("#confirmButton").isDisabled())).isTrue();
    }

    @Test
    @DisplayName("the cart shows its lines and total, changes the quantity with + and − and takes a product out")
    void editsTheCart() throws Exception {
        fillCart();
        Parent root = openCart(true).root();

        assertThat(texts(root, "linesBox")).contains("Sérum Vitamina C", "$ 48.000 c/u", "2", "$ 96.000");
        assertThat(text(root, "totalLabel")).isEqualTo("$ 96.000");
        assertThat(text(root, "countLabel")).isEqualTo("2 unidades");

        pressButton(root, "+", 0);
        assertThat(context.cart().quantityOf(shop.serum.getId())).isEqualTo(3);
        assertThat(text(root, "totalLabel")).isEqualTo("$ 144.000");
        pressButton(root, "−", 0);
        assertThat(context.cart().quantityOf(shop.serum.getId())).isEqualTo(2);

        pressButton(root, "Quitar", 0);
        assertThat(context.cart().isEmpty()).isTrue();
        assertThat(shown(root, "emptyLabel")).isTrue();
    }

    @Test
    @DisplayName("confirming places one pending order with the quantities, clears the cart and says where to pay it")
    void confirmsTheOrder() throws Exception {
        fillCart();
        Parent root = openCart(true).root();

        press(root, "confirmButton");

        List<Order> orders = myOrders();
        assertThat(orders).hasSize(1);
        assertThat(orders.getFirst().status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(orders.getFirst().total()).isEqualByComparingTo("96000");
        assertThat(orders.getFirst().lines()).singleElement().satisfies(line -> assertThat(line.quantity()).isEqualTo(2));
        assertThat(context.cart().isEmpty()).isTrue();
        assertThat(shown(root, "resultBox")).isTrue();
        assertThat(text(root, "resultTitle")).isEqualTo("Pedido n.º " + orders.getFirst().id() + " creado");
        assertThat(text(root, "resultText")).contains("Págalo en el local antes de las").contains("Mis compras");
        assertThat(context.catalog().findPublished(shop.serum.getId()).availableStock()).isEqualTo(8);
    }

    @Test
    @DisplayName("a second click while the order is being placed does nothing: there is never a double order")
    void doubleClickPlacesOneOrder() throws Exception {
        fillCart();
        var screen = open("CartView.fxml", (CartController controller) -> {
            controller.useExecutor(held::add);
            controller.useAlerts(presenter(true));
        });

        press(screen.root(), "confirmButton");
        press(screen.root(), "confirmButton");

        assertThat(held).hasSize(1);
        assertThat(FxTest.onFxThread(() -> screen.root().lookup("#confirmButton").isDisabled())).isTrue();
        assertThat(myOrders()).isEmpty();
    }

    @Test
    @DisplayName("retrying the same cart with the same key gives back the same order, never a second one")
    void retryWithTheSameKey() throws Exception {
        fillCart();
        String key = context.cart().requestKey();
        Map<Integer, Integer> quantities = context.cart().quantities();

        Order first = context.orders().place(key, quantities);
        Order again = context.orders().place(key, quantities);

        assertThat(again.id()).isEqualTo(first.id());
        assertThat(myOrders()).hasSize(1);
    }

    @Test
    @DisplayName("if a price changed the order is NOT placed: the change is explained, the cart is updated and the next confirmation places it")
    void priceChangeIsExplained() throws Exception {
        fillCart();
        shop.asAdmin();
        context.products().update(shop.serum.getId(), new com.maisonglow.service.ProductData("Sérum Vitamina C", 1, 50000, 2));
        shop.asCustomer("ana");
        Parent root = openCart(true).root();

        press(root, "confirmButton");

        assertThat(myOrders()).isEmpty();
        assertThat(shown(root, "changesBox")).isTrue();
        assertThat(text(root, "changesText")).contains("cambió de $ 48.000 a $ 50.000").contains("vuelve a confirmar");
        assertThat(text(root, "totalLabel")).isEqualTo("$ 100.000");

        press(root, "confirmButton");

        assertThat(myOrders()).singleElement().satisfies(order -> assertThat(order.total()).isEqualByComparingTo("100000"));
    }

    @Test
    @DisplayName("if there are fewer units than the cart holds, or a product was withdrawn, it is explained and nothing is placed")
    void stockAndWithdrawalAreExplained() throws Exception {
        context.cart().add(context.catalog().findPublished(shop.serum.getId()), 5);
        context.cart().add(context.catalog().findPublished(shop.esmalte.getId()), 1);
        shop.asCustomer("laura");
        context.orders().place(UUID.randomUUID().toString(), Map.of(shop.serum.getId(), 8));
        shop.asAdmin();
        context.catalog().setPublished(shop.esmalte.getId(), false);
        shop.asCustomer("ana");
        Parent root = openCart(true).root();

        press(root, "confirmButton");

        assertThat(myOrders()).isEmpty();
        assertThat(text(root, "changesText")).contains("solo quedan 2").contains("Esmalte Rosa Nude ya no está disponible");
        assertThat(context.cart().quantityOf(shop.serum.getId())).isEqualTo(2);
        assertThat(context.cart().quantityOf(shop.esmalte.getId())).isZero();
    }

    @Test
    @DisplayName("closing the session empties the cart, so the next account never sees it")
    void logoutEmptiesTheCart() throws Exception {
        fillCart();
        AppWindow window = FxTest.onFxThread(() -> new AppWindow(new Stage(), context, stage -> { }));

        FxTest.onFxThread(() -> { window.logout(); return null; });

        assertThat(context.cart().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("an answer that arrives after the session closed is ignored by the cart")
    void cartLateAnswerIsIgnored() throws Exception {
        fillCart();
        ViewLoader.Loaded<CartController> loaded = FxTest.onFxThread(() -> {
            ViewLoader.Loaded<CartController> opened = ViewLoader.loadWithController(
                CustomerShoppingViewTest.class.getResource("/com/maisonglow/view/CartView.fxml"), context);
            opened.controller().useExecutor(held::add);
            opened.controller().useAlerts(presenter(true));
            new Scene(opened.root(), 1100, 700);
            opened.root().applyCss();
            opened.root().layout();
            return opened;
        });
        press(loaded.root(), "confirmButton");
        FxTest.onFxThread(() -> { ((ViewLifecycle) loaded.controller()).dispose(); return null; });
        held.forEach(Runnable::run);
        settle();

        assertThat(myOrders()).isEmpty();
        assertThat(texts(loaded.root(), "linesBox")).isEmpty();
    }

    // ---- Mis compras ----

    @Test
    @DisplayName("with nothing bought the screen says where to start, twice")
    void noPurchases() throws Exception {
        Parent root = openPurchases(true).root();

        assertThat(shown(root, "ordersEmpty")).isTrue();
        assertThat(text(root, "ordersEmpty")).contains("Tienda");
        assertThat(shown(root, "storeEmpty")).isTrue();
    }

    @Test
    @DisplayName("a pending order says it is not paid yet, where and until when to pay it, and never looks paid")
    void pendingOrderCard() throws Exception {
        fillCart();
        context.orders().place(context.cart().requestKey(), context.cart().quantities());
        Parent root = openPurchases(true).root();

        List<String> card = texts(root, "ordersBox");
        assertThat(card).contains("Pendiente de pago", "$ 96.000", "2 × Sérum Vitamina C · $ 96.000");
        assertThat(card).anyMatch(text -> text.startsWith("Aún no está pagado: págalo en el local antes de las"));
        assertThat(card).doesNotContain("Pagado");
        assertThat(shown(root, "ordersEmpty")).isFalse();
    }

    @Test
    @DisplayName("only the customer's own orders show, newest first")
    void onlyOwnOrdersNewestFirst() throws Exception {
        Order first = context.orders().place(UUID.randomUUID().toString(), Map.of(shop.serum.getId(), 1));
        shop.advance(1);
        Order second = context.orders().place(UUID.randomUUID().toString(), Map.of(shop.serum.getId(), 2));
        shop.asCustomer("laura");
        context.orders().place(UUID.randomUUID().toString(), Map.of(shop.serum.getId(), 3));
        shop.asCustomer("ana");

        Parent root = openPurchases(true).root();

        List<String> texts = texts(root, "ordersBox");
        assertThat(texts.stream().filter(text -> text.startsWith("Pedido n.º")).toList())
            .containsExactly("Pedido n.º " + second.id(), "Pedido n.º " + first.id());
        assertThat(texts).noneMatch(text -> text.contains("144.000"));
    }

    @Test
    @DisplayName("cancelling a pending order asks first, releases it and it shows as cancelled")
    void cancelsAPendingOrder() throws Exception {
        Order order = context.orders().place(UUID.randomUUID().toString(), Map.of(shop.serum.getId(), 2));
        Parent root = openPurchases(true).root();

        pressButton(root, "Cancelar pedido", 0);

        assertThat(dialogs).anyMatch(text -> text.contains("Se libera la reserva"));
        assertThat(context.orders().find(order.id()).status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(texts(root, "ordersBox")).contains("Cancelado", "Cancelado: la reserva se liberó.");
    }

    @Test
    @DisplayName("answering no to the cancellation keeps the order pending")
    void declinedCancellation() throws Exception {
        Order order = context.orders().place(UUID.randomUUID().toString(), Map.of(shop.serum.getId(), 2));
        Parent root = openPurchases(false).root();

        pressButton(root, "Cancelar pedido", 0);

        assertThat(context.orders().find(order.id()).status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    @DisplayName("an order paid at the shop shows as paid with its sale and offers the receipt; it cannot be cancelled")
    void paidOrder() throws Exception {
        Order order = context.orders().place(UUID.randomUUID().toString(), Map.of(shop.serum.getId(), 2));
        shop.asAdmin();
        context.orders().confirmPayment(order.id(), "CASH");
        shop.asCustomer("ana");
        Parent root = openPurchases(true).root();

        List<String> card = texts(root, "ordersBox");
        assertThat(card).contains("Pagado");
        assertThat(card).anyMatch(text -> text.startsWith("Pagado en el local · Compra n.º"));
        assertThat(card).doesNotContain("Pendiente de pago");
        assertThat(FxTest.onFxThread(() -> root.lookupAll(".button").stream().filter(Button.class::isInstance)
            .map(button -> ((Button) button).getText()).toList())).contains("Ver comprobante").doesNotContain("Cancelar pedido");

        pressButton(root, "Ver comprobante", 0);

        assertThat(dialogs).anyMatch(text -> text.contains("Sérum Vitamina C") && text.contains("Maison Glow"));
        assertThat(texts(root, "storeBox")).isEmpty();
    }

    @Test
    @DisplayName("an order not paid in 30 minutes shows as expired when the screen is read again")
    void expiredOrder() throws Exception {
        context.orders().place(UUID.randomUUID().toString(), Map.of(shop.serum.getId(), 2));
        shop.advance(31);

        Parent root = openPurchases(true).root();

        assertThat(texts(root, "ordersBox")).contains("Vencido", "Venció: no se pagó a tiempo y la reserva se liberó.");
    }

    @Test
    @DisplayName("purchases made directly at the shop show in their own section, without repeating the ones from orders")
    void storePurchases() throws Exception {
        Order order = context.orders().place(UUID.randomUUID().toString(), Map.of(shop.serum.getId(), 1));
        shop.asAdmin();
        context.orders().confirmPayment(order.id(), "CARD");
        Customer ana = context.customers().searchByName("Ana").getFirst();
        Sale sale = new Sale(ana);
        SaleService.addLine(sale, shop.esmalte, 1);
        context.sales().register(sale);
        shop.asCustomer("ana");

        Parent root = openPurchases(true).root();

        List<String> store = texts(root, "storeBox");
        assertThat(store).hasSize(2);
        assertThat(store.getFirst()).startsWith("Compra n.º").contains("/");
        assertThat(store.get(1)).isEqualTo("$ 18.000");
        assertThat(shown(root, "storeEmpty")).isFalse();
    }

    @Test
    @DisplayName("an answer that arrives after the session closed is ignored by Mis compras")
    void purchasesLateAnswerIsIgnored() throws Exception {
        context.orders().place(UUID.randomUUID().toString(), Map.of(shop.serum.getId(), 2));
        ViewLoader.Loaded<MyPurchasesController> loaded = FxTest.onFxThread(() -> {
            ViewLoader.Loaded<MyPurchasesController> opened = ViewLoader.loadWithController(
                CustomerShoppingViewTest.class.getResource("/com/maisonglow/view/MyPurchasesView.fxml"), context);
            new Scene(opened.root(), 1000, 700);
            opened.root().applyCss();
            opened.root().layout();
            ((ViewLifecycle) opened.controller()).dispose();
            return opened;
        });
        Thread.sleep(300);
        settle();

        assertThat(texts(loaded.root(), "ordersBox")).isEmpty();
    }
}
