package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Commerce.Order;
import com.maisonglow.model.Commerce.OrderStatus;
import com.maisonglow.model.Sale;
import com.maisonglow.service.CustomerPortalService;
import com.maisonglow.service.OrderService;
import com.maisonglow.service.SaleService;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.util.DateFormats;
import com.maisonglow.util.InvoiceFormatter;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.BackgroundTask;
import com.maisonglow.view.LoadingOverlay;
import com.maisonglow.view.RefreshableView;
import com.maisonglow.view.ViewLifecycle;
import com.maisonglow.view.orders.OrderState;
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

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * Controller of {@code MyPurchasesView.fxml}: the purchases of a customer (task N-R03). It shows their orders from the shop,
 * newest first, each with its state, what it holds and what is left to do: an order waiting for payment is never shown as paid
 * and says where and until when to pay it, and can be cancelled by its owner; a paid one offers its receipt. Below, the
 * purchases made directly at the shop. Everything comes from the account of the session through {@link OrderService} and
 * {@link CustomerPortalService}; the service refuses another customer's order. The data is read in the background, the screen
 * reads it again every time it is shown, and an answer that arrives after the session closed is ignored.
 */
public final class MyPurchasesController implements ViewLifecycle, RefreshableView {

    private static final String BUSINESS = "Maison Glow";

    /** What the screen shows, read in one go. */
    private record Loaded(List<Order> orders, List<Sale> storePurchases) { }

    private final OrderService orders;
    private final CustomerPortalService portal;
    private Executor executor = ForkJoinPool.commonPool();
    private AlertPresenter alerts;
    private LoadingOverlay loading;
    private boolean disposed;

    @FXML
    private StackPane host;
    @FXML
    private VBox ordersBox;
    @FXML
    private Label ordersEmpty;
    @FXML
    private VBox storeBox;
    @FXML
    private Label storeEmpty;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public MyPurchasesController(AppContext context) {
        Objects.requireNonNull(context, "context");
        this.orders = context.orders();
        this.portal = context.portal();
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
        alerts = new AlertPresenter(ordersBox);
        loading = LoadingOverlay.over(host);
        ordersBox.getChildren().clear();
        refresh();
    }

    /** Reads the purchases again; it is called when the screen is shown, so an order that expired meanwhile shows as expired. */
    @Override
    public void refresh() {
        loading.show("Cargando tus compras…");
        BackgroundTask.run(executor, this::read, loaded -> {
            loading.hide();
            show(loaded);
        }, error -> {
            loading.hide();
            alerts.showError(error);
        }, () -> !disposed);
    }

    private Loaded read() throws BusinessRuleException, DataAccessException {
        try {
            List<Order> mine = orders.mine().stream().sorted(Comparator.comparing(Order::createdAt).reversed()).toList();
            Set<Integer> fromOrders = mine.stream().map(Order::saleId).filter(Objects::nonNull).collect(Collectors.toSet());
            List<Sale> store = portal.myPurchases().stream().filter(sale -> !fromOrders.contains(sale.getId()))
                .sorted(Comparator.comparing(Sale::getSoldAt).reversed()).toList();
            return new Loaded(mine, store);
        } catch (ValidationException e) {
            throw new BusinessRuleException(e.getMessage(), e);
        }
    }

    private void show(Loaded loaded) {
        ordersBox.getChildren().setAll(loaded.orders().stream().map(this::orderCard).toList());
        ordersEmpty.setVisible(loaded.orders().isEmpty());
        ordersEmpty.setManaged(loaded.orders().isEmpty());
        storeBox.getChildren().setAll(loaded.storePurchases().stream().map(MyPurchasesController::saleRow).toList());
        storeEmpty.setVisible(loaded.storePurchases().isEmpty());
        storeEmpty.setManaged(loaded.storePurchases().isEmpty());
    }

    private Node orderCard(Order order) {
        OrderState state = OrderState.valueOf(order.status().name());
        Label number = new Label("Pedido n.º " + order.id());
        number.getStyleClass().add("stat-value");
        Label chip = new Label(state.label());
        chip.getStyleClass().add(state.chipStyle());
        Label total = new Label(CurrencyFormatter.format(order.total()));
        total.getStyleClass().add("shop-price");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(8, number, chip, spacer, total);
        header.setAlignment(Pos.CENTER_LEFT);
        Label date = new Label(DateFormats.dateTime(order.createdAt()));
        date.getStyleClass().add("stat-caption");
        VBox card = new VBox(6, header, date);
        card.getStyleClass().addAll("card", "form-card");
        order.lines().forEach(line -> {
            Label label = new Label(line.quantity() + " × " + line.name() + " · " + CurrencyFormatter.format(line.unitPrice().multiply(java.math.BigDecimal.valueOf(line.quantity()))));
            label.getStyleClass().add("form-hint");
            card.getChildren().add(label);
        });
        Label note = new Label(noteOf(order));
        note.getStyleClass().add("form-hint");
        note.setWrapText(true);
        card.getChildren().add(note);
        if (order.status() == OrderStatus.PENDING_PAYMENT) {
            Button cancel = new Button("Cancelar pedido");
            cancel.getStyleClass().addAll("button-secondary", "button-compact");
            cancel.setOnAction(event -> cancel(order));
            card.getChildren().add(cancel);
        } else if (order.status() == OrderStatus.PAID) {
            Button receipt = new Button("Ver comprobante");
            receipt.getStyleClass().addAll("button-secondary", "button-compact");
            receipt.setOnAction(event -> showReceipt(order));
            card.getChildren().add(receipt);
        }
        return card;
    }

    /** What is left to do with an order, or what happened to it; a pending one never reads as paid. */
    private static String noteOf(Order order) {
        return switch (order.status()) {
            case PENDING_PAYMENT -> "Aún no está pagado: págalo en el local antes de las "
                + DateFormats.time(order.expiresAt().toLocalTime()) + ". Hasta entonces tus productos están reservados.";
            case PAID -> "Pagado en el local" + (order.saleId() == null ? "." : " · Compra n.º " + order.saleId() + ".");
            case CANCELLED -> "Cancelado: la reserva se liberó.";
            case EXPIRED -> "Venció: no se pagó a tiempo y la reserva se liberó.";
        };
    }

    private static Node saleRow(Sale sale) {
        Label number = new Label("Compra n.º " + sale.getId() + " · " + DateFormats.dateTime(sale.getSoldAt()));
        number.getStyleClass().add("home-row-title");
        Label total = new Label(CurrencyFormatter.format(SaleService.calculateTotal(sale)));
        total.getStyleClass().add("shop-price");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(12, number, spacer, total);
        row.getStyleClass().add("home-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void cancel(Order order) {
        if (!alerts.confirm("¿Cancelar el pedido n.º " + order.id() + "? Se libera la reserva de tus productos.")) {
            return;
        }
        loading.show("Cancelando el pedido…");
        BackgroundTask.run(executor, () -> orders.cancel(order.id()), cancelled -> {
            loading.hide();
            alerts.toast("Pedido cancelado.");
            refresh();
        }, error -> {
            loading.hide();
            alerts.showError(error);
            refresh();
        }, () -> !disposed);
    }

    private void showReceipt(Order order) {
        BackgroundTask.run(executor, () -> new InvoiceFormatter(BUSINESS).format(orders.receipt(order.id())),
            alerts::showInfo, error -> alerts.showError(error), () -> !disposed);
    }

    /** Closing the session: an answer that arrives later is ignored and the purchases shown are dropped. */
    @Override
    public void dispose() {
        disposed = true;
        ordersBox.getChildren().clear();
        storeBox.getChildren().clear();
    }
}
