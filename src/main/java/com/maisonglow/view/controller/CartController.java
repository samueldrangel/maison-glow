package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Commerce.CatalogProduct;
import com.maisonglow.model.Commerce.Order;
import com.maisonglow.service.CatalogService;
import com.maisonglow.service.OrderService;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.util.DateFormats;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.BackgroundTask;
import com.maisonglow.view.LoadingOverlay;
import com.maisonglow.view.ViewLifecycle;
import com.maisonglow.view.shop.Cart;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Controller of {@code CartView.fxml}: the cart of the customer and the confirmation of their order (task N-R03). The cart is
 * the one of the session ({@link Cart}); confirming first asks the shop how it is now, and if a price changed, the units fell or
 * a product was withdrawn, it explains it and does NOT place the order, so the customer decides with the real figures; only a
 * second confirmation of the same cart places it. The order is placed with {@link OrderService#place}, which takes the customer
 * from the session and sets the prices itself: only the quantities travel. The request key stays the same while the cart
 * does not change, so a double click or a retry gives back the same order and never two. Everything slow runs in the
 * background with a loading veil, and an answer that arrives after the session closed is ignored.
 */
public final class CartController implements ViewLifecycle {

    private final Cart cart;
    private final CatalogService catalog;
    private final OrderService orders;
    private Executor executor = ForkJoinPool.commonPool();
    private AlertPresenter alerts;
    private LoadingOverlay loading;
    private boolean busy;
    private boolean disposed;

    @FXML
    private StackPane host;
    @FXML
    private VBox linesBox;
    @FXML
    private Label emptyLabel;
    @FXML
    private Label totalLabel;
    @FXML
    private Label countLabel;
    @FXML
    private HBox changesBox;
    @FXML
    private Label changesText;
    @FXML
    private Button confirmButton;
    @FXML
    private VBox resultBox;
    @FXML
    private Label resultTitle;
    @FXML
    private Label resultText;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public CartController(AppContext context) {
        Objects.requireNonNull(context, "context");
        this.cart = context.cart();
        this.catalog = context.catalog();
        this.orders = context.orders();
    }

    /**
     * Replaces where the calls run, which the tests use to hold an answer back.
     *
     * @param newExecutor runs the call
     */
    public void useExecutor(Executor newExecutor) {
        this.executor = Objects.requireNonNull(newExecutor, "newExecutor");
    }

    /**
     * Replaces the presenter of dialogs, which the tests use so that no real window is opened.
     *
     * @param presenter the presenter to use from now on
     */
    public void useAlerts(AlertPresenter presenter) {
        this.alerts = Objects.requireNonNull(presenter, "presenter");
    }

    @FXML
    private void initialize() {
        alerts = new AlertPresenter(confirmButton);
        loading = LoadingOverlay.over(host);
        confirmButton.setOnAction(event -> confirm());
        hide(changesBox);
        hide(resultBox);
        render();
    }

    private void render() {
        List<Cart.Line> lines = cart.lines();
        linesBox.getChildren().setAll(lines.stream().map(this::row).toList());
        emptyLabel.setVisible(lines.isEmpty());
        emptyLabel.setManaged(lines.isEmpty());
        totalLabel.setText(CurrencyFormatter.format(cart.total()));
        int units = cart.itemCount();
        countLabel.setText(units + (units == 1 ? " unidad" : " unidades"));
        confirmButton.setDisable(lines.isEmpty() || busy);
    }

    private Node row(Cart.Line line) {
        Label name = new Label(line.name());
        name.getStyleClass().add("home-row-title");
        Label price = new Label(CurrencyFormatter.format(line.unitPrice()) + " c/u");
        price.getStyleClass().add("home-row-detail");
        Button minus = new Button("−");
        minus.getStyleClass().add("icon-button");
        minus.setOnAction(event -> change(line, line.quantity() - 1));
        Label quantity = new Label(String.valueOf(line.quantity()));
        quantity.getStyleClass().add("stat-value");
        quantity.setMinWidth(28);
        quantity.setAlignment(Pos.CENTER);
        Button plus = new Button("+");
        plus.getStyleClass().add("icon-button");
        plus.setDisable(line.quantity() >= line.available());
        plus.setOnAction(event -> change(line, line.quantity() + 1));
        Label subtotal = new Label(CurrencyFormatter.format(line.subtotal()));
        subtotal.getStyleClass().add("shop-price");
        subtotal.setMinWidth(Region.USE_PREF_SIZE);
        Button remove = new Button("Quitar");
        remove.getStyleClass().addAll("button-secondary", "button-compact");
        remove.setOnAction(event -> change(line, 0));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(12, new VBox(2, name, price), spacer, minus, quantity, plus, subtotal, remove);
        row.getStyleClass().add("home-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void change(Cart.Line line, int quantity) {
        if (busy) {
            return;
        }
        try {
            cart.setQuantity(line.productId(), quantity);
            hide(changesBox);
            hide(resultBox);
            render();
        } catch (ValidationException e) {
            alerts.showError(e);
        }
    }

    /**
     * Asks the shop how it is now. If something changed the customer is told and the order waits for a new confirmation;
     * if nothing changed, or they already saw the changes, the order is placed.
     */
    private void confirm() {
        if (busy || cart.isEmpty()) {
            return;
        }
        busy = true;
        confirmButton.setDisable(true);
        loading.show("Revisando tu pedido…");
        BackgroundTask.run(executor, this::readShop, current -> {
            List<String> changes = cart.reconcile(current);
            if (!changes.isEmpty()) {
                showChanges(changes);
                free();
                return;
            }
            place();
        }, this::fail, () -> !disposed);
    }

    private Map<Integer, CatalogProduct> readShop() throws BusinessRuleException, DataAccessException, ValidationException {
        Map<Integer, CatalogProduct> current = new HashMap<>();
        catalog.listPublished("").forEach(product -> current.put(product.id(), product));
        return current;
    }

    private void place() {
        String key = cart.requestKey();
        Map<Integer, Integer> quantities = cart.quantities();
        loading.show("Creando tu pedido…");
        BackgroundTask.run(executor, () -> orders.place(key, quantities), this::placed, error -> {
            free();
            alerts.showError(error);
            if (error instanceof BusinessRuleException) {
                refreshAfterError();
            }
        }, () -> !disposed);
    }

    /** After the service refuses the order (no stock, a product withdrawn), shows the cart as the shop has it now. */
    private void refreshAfterError() {
        BackgroundTask.run(executor, this::readShop, current -> {
            List<String> changes = cart.reconcile(current);
            if (!changes.isEmpty()) {
                showChanges(changes);
            }
        }, error -> { }, () -> !disposed);
    }

    private void placed(Order order) {
        cart.clear();
        hide(changesBox);
        resultTitle.setText("Pedido n.º " + order.id() + " creado");
        resultText.setText("Págalo en el local antes de las " + DateFormats.time(order.expiresAt().toLocalTime())
            + " y recógelo allí. Si no lo pagas a tiempo, la reserva se libera sola. Lo ves en «Mis compras».");
        resultBox.setVisible(true);
        resultBox.setManaged(true);
        free();
        alerts.toast("Pedido creado.");
    }

    private void showChanges(List<String> changes) {
        changesText.setText(String.join(" ", changes) + " Revisa tu carrito y vuelve a confirmar.");
        changesBox.setVisible(true);
        changesBox.setManaged(true);
        render();
    }

    private void fail(Exception error) {
        free();
        alerts.showError(error);
    }

    private void free() {
        busy = false;
        loading.hide();
        render();
    }

    private static void hide(Node node) {
        node.setVisible(false);
        node.setManaged(false);
    }

    /** Closing the session: an answer that arrives later is ignored, and the lines shown are dropped. */
    @Override
    public void dispose() {
        disposed = true;
        linesBox.getChildren().clear();
    }
}
