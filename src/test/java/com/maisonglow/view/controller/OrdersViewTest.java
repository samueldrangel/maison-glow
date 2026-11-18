package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLoader;
import com.maisonglow.view.orders.FakeOrders;
import com.maisonglow.view.orders.OrderDetail;
import com.maisonglow.view.orders.OrderState;
import com.maisonglow.view.orders.OrderSummary;
import com.maisonglow.view.orders.PaymentMethod;
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
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Orders view tests")
class OrdersViewTest {

    private static boolean toolkit;

    @TempDir
    Path data;

    private AppContext context;
    private FakeOrders orders;
    private LocalDateTime now;
    private final List<String> dialogs = new ArrayList<>();
    private final List<Runnable> held = new ArrayList<>();

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @BeforeEach
    void prepare() {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        now = LocalDateTime.now();
        OrderDetail.Line mask = new OrderDetail.Line("Mascarilla Reparadora", 2, new BigDecimal("35000"));
        OrderDetail.Line serum = new OrderDetail.Line("Sérum Vitamina C", 1, new BigDecimal("48000"));
        orders = new FakeOrders()
            .order(3, "Ana García", OrderState.PENDING_PAYMENT, now.minusMinutes(10), now.plusMinutes(20), mask, serum)
            .order(2, "Laura Pérez", OrderState.PAID, now.minusHours(2), null, serum)
            .order(1, "Camila Ruiz", OrderState.CANCELLED, now.minusHours(5), null, mask)
            .order(4, "Jose Chinchia", OrderState.EXPIRED, now.minusHours(9), null, mask);
        context = new AppContext(data, Clock.systemDefaultZone(), AiConfig.simulated());
        context.useOrdersGateway(orders);
    }

    private Parent open(boolean confirm) throws Exception {
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<OrdersController> loaded = ViewLoader.loadWithController(
                OrdersViewTest.class.getResource("/com/maisonglow/view/OrdersView.fxml"), context);
            loaded.controller().useExecutor(Runnable::run);
            loaded.controller().useAlerts(new AlertPresenter(() -> null, alert -> {
                dialogs.add(alert.getContentText());
                return alert.getButtonTypes().stream().filter(button -> confirm && "Confirmar".equals(button.getText())).findFirst()
                    .or(() -> alert.getButtonTypes().stream().findFirst().filter(button -> !alert.getContentText().startsWith("¿")));
            }));
            new Scene(loaded.root(), 1100, 700);
            loaded.root().applyCss();
            loaded.root().layout();
            return loaded.root();
        });
    }

    @SuppressWarnings("unchecked")
    private static TableView<OrderSummary> table(Parent root) {
        return (TableView<OrderSummary>) root.lookup("#ordersTable");
    }

    private static void settle() throws Exception {
        for (int round = 0; round < 3; round++) {
            FxTest.onFxThread(() -> null);
            Thread.sleep(60);
        }
    }

    private static List<Integer> numbers(Parent root) throws Exception {
        settle();
        return FxTest.onFxThread(() -> table(root).getItems().stream().map(OrderSummary::id).toList());
    }

    private static void chip(Parent root, String text) throws Exception {
        FxTest.onFxThread(() -> {
            root.lookupAll(".filter-chip").stream().map(ToggleButton.class::cast)
                .filter(chip -> text.equals(chip.getText())).findFirst().orElseThrow().fire();
            return null;
        });
    }

    private static void select(Parent root, int id) throws Exception {
        settle();
        FxTest.onFxThread(() -> {
            TableView<OrderSummary> table = table(root);
            table.getSelectionModel().select(table.getItems().stream().filter(order -> order.id() == id).findFirst().orElseThrow());
            return null;
        });
        settle();
    }

    private static String text(Parent root, String id) throws Exception {
        return FxTest.onFxThread(() -> ((Label) root.lookup("#" + id)).getText());
    }

    private static boolean shown(Parent root, String id) throws Exception {
        return FxTest.onFxThread(() -> root.lookup("#" + id).isVisible());
    }

    private static void press(Parent root, String id) throws Exception {
        FxTest.onFxThread(() -> {
            ((Button) root.lookup("#" + id)).fire();
            return null;
        });
        settle();
    }

    private static void pay(Parent root, PaymentMethod method) throws Exception {
        FxTest.onFxThread(() -> {
            @SuppressWarnings("unchecked")
            ComboBox<PaymentMethod> box = (ComboBox<PaymentMethod>) root.lookup("#methodBox");
            box.setValue(method);
            return null;
        });
    }

    @Test
    @DisplayName("opens with the orders waiting for payment and nothing selected")
    void opensWithThePending() throws Exception {
        Parent root = open(true);

        assertThat(numbers(root)).containsExactly(3);
        assertThat(shown(root, "detailBox")).isFalse();
        assertThat(shown(root, "detailEmpty")).isTrue();
    }

    @Test
    @DisplayName("each chip shows its group; the list of closed orders brings the cancelled and the expired")
    void chipsFilterByState() throws Exception {
        Parent root = open(true);

        chip(root, "Pagados");
        assertThat(numbers(root)).containsExactly(2);
        chip(root, "Cancelados y vencidos");
        assertThat(numbers(root)).containsExactlyInAnyOrder(1, 4);
        chip(root, "Todos");
        assertThat(numbers(root)).containsExactlyInAnyOrder(1, 2, 3, 4);
    }

    @Test
    @DisplayName("the search finds by customer, ignoring accents and case, and by number; with no match it says so")
    void searches() throws Exception {
        Parent root = open(true);
        chip(root, "Todos");

        FxTest.onFxThread(() -> { ((TextField) root.lookup("#searchField")).setText("garcia"); return null; });
        assertThat(numbers(root)).containsExactly(3);
        FxTest.onFxThread(() -> { ((TextField) root.lookup("#searchField")).setText("4"); return null; });
        assertThat(numbers(root)).containsExactly(4);
        FxTest.onFxThread(() -> { ((TextField) root.lookup("#searchField")).setText("zzz"); return null; });
        assertThat(numbers(root)).isEmpty();
        assertThat(text(root, "emptyLabel")).contains("coincide");
    }

    @Test
    @DisplayName("a pending order never looks paid: it warns that the stock is reserved and not sold, with the lines, total and time left")
    void pendingOrderDetail() throws Exception {
        Parent root = open(true);
        select(root, 3);

        assertThat(text(root, "detailTitle")).isEqualTo("Pedido n.º 3");
        assertThat(text(root, "detailState")).isEqualTo("Pendiente de pago");
        assertThat(shown(root, "unpaidNote")).isTrue();
        assertThat(FxTest.onFxThread(() -> ((Label) root.lookup("#unpaidNote .banner-text")).getText())).contains("no vendido");
        assertThat(FxTest.onFxThread(() -> ((javafx.scene.layout.VBox) root.lookup("#linesBox")).getChildren().size())).isEqualTo(2);
        assertThat(text(root, "totalLabel")).contains("118.000");
        assertThat(text(root, "reservationLabel")).contains("Stock reservado hasta las").contains("min");
        assertThat(shown(root, "payButton")).isTrue();
        assertThat(shown(root, "cancelButton")).isTrue();
        assertThat(shown(root, "receiptButton")).isFalse();
    }

    @Test
    @DisplayName("a paid order shows how it was paid and its sale, has no payment buttons and offers the receipt")
    void paidOrderDetail() throws Exception {
        Parent root = open(true);
        chip(root, "Pagados");
        select(root, 2);

        assertThat(text(root, "detailState")).isEqualTo("Pagado");
        assertThat(shown(root, "unpaidNote")).isFalse();
        assertThat(shown(root, "paymentBox")).isFalse();
        assertThat(shown(root, "payButton")).isFalse();
        assertThat(shown(root, "cancelButton")).isFalse();
        assertThat(text(root, "reservationLabel")).contains("efectivo").contains("Venta n.º 102").contains("ya se descontó");

        press(root, "receiptButton");

        assertThat(dialogs).contains("COMPROBANTE DE VENTA 102");
    }

    @Test
    @DisplayName("a cancelled or expired order says its stock was released and offers nothing to do")
    void closedOrderDetail() throws Exception {
        Parent root = open(true);
        chip(root, "Cancelados y vencidos");
        select(root, 4);

        assertThat(text(root, "reservationLabel")).contains("Venció").contains("liberó");
        assertThat(shown(root, "payButton")).isFalse();
        assertThat(shown(root, "receiptButton")).isFalse();
        select(root, 1);
        assertThat(text(root, "reservationLabel")).contains("Cancelado").contains("liberó");
    }

    @Test
    @DisplayName("paying without choosing how asks for it under the field and calls nothing")
    void payNeedsAMethod() throws Exception {
        Parent root = open(true);
        select(root, 3);

        press(root, "payButton");

        assertThat(text(root, "methodMessage")).contains("Elige");
        assertThat(shown(root, "methodMessage")).isTrue();
        assertThat(orders.calls).noneMatch(call -> call.startsWith("pay"));
    }

    @Test
    @DisplayName("confirming the payment calls the service once, becomes a sale and leaves the pending list")
    void confirmsThePayment() throws Exception {
        Parent root = open(true);
        select(root, 3);
        pay(root, PaymentMethod.CARD);

        press(root, "payButton");

        assertThat(orders.calls).filteredOn(call -> call.startsWith("pay")).containsExactly("pay:3:CARD");
        assertThat(numbers(root)).isEmpty();
        chip(root, "Pagados");
        assertThat(numbers(root)).containsExactlyInAnyOrder(2, 3);
        assertThat(dialogs).anyMatch(text -> text.contains("pedido n.º 3") && text.contains("descontará"));
    }

    @Test
    @DisplayName("answering no to the confirmation changes nothing")
    void declinedPaymentChangesNothing() throws Exception {
        Parent root = open(false);
        select(root, 3);
        pay(root, PaymentMethod.CASH);

        press(root, "payButton");

        assertThat(orders.calls).noneMatch(call -> call.startsWith("pay"));
        assertThat(numbers(root)).containsExactly(3);
    }

    @Test
    @DisplayName("cancelling releases the order once and it moves to the closed list")
    void cancelsTheOrder() throws Exception {
        Parent root = open(true);
        select(root, 3);

        press(root, "cancelButton");

        assertThat(orders.calls).filteredOn(call -> call.startsWith("cancel")).containsExactly("cancel:3");
        chip(root, "Cancelados y vencidos");
        assertThat(numbers(root)).containsExactlyInAnyOrder(1, 3, 4);
    }

    @Test
    @DisplayName("a rule broken by the service is shown to the administrator and the list is not changed")
    void serviceErrorIsShown() throws Exception {
        Parent root = open(true);
        select(root, 3);
        pay(root, PaymentMethod.CASH);
        // Somebody else paid it a moment earlier.
        orders.confirmPayment(3, PaymentMethod.CASH);

        press(root, "payButton");

        assertThat(dialogs).anyMatch(text -> text.contains("ya no está pendiente de pago"));
        assertThat(FxTest.onFxThread(() -> root.lookup("#payButton").isDisabled())).isFalse();
    }

    @Test
    @DisplayName("an answer that arrives after the screen was discarded is ignored")
    void lateAnswerIsIgnoredAfterDispose() throws Exception {
        var loaded = FxTest.onFxThread(() -> {
            ViewLoader.Loaded<OrdersController> opened = ViewLoader.loadWithController(
                OrdersViewTest.class.getResource("/com/maisonglow/view/OrdersView.fxml"), context);
            opened.controller().useExecutor(held::add);
            new Scene(opened.root(), 1100, 700);
            opened.root().applyCss();
            opened.root().layout();
            return opened;
        });
        held.clear();
        FxTest.onFxThread(() -> { loaded.controller().dispose(); return null; });
        FxTest.onFxThread(() -> { loaded.controller().refresh(); return null; });
        held.forEach(Runnable::run);
        settle();

        assertThat(FxTest.onFxThread(() -> table(loaded.root()).getItems())).isEmpty();
        assertThat(Optional.of(held)).isPresent();
    }
}
