package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.util.DateFormats;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.BackgroundTask;
import com.maisonglow.view.LoadingOverlay;
import com.maisonglow.view.ViewLifecycle;
import com.maisonglow.view.orders.OrderDetail;
import com.maisonglow.view.orders.OrderFilter;
import com.maisonglow.view.orders.OrderState;
import com.maisonglow.view.orders.OrderSummary;
import com.maisonglow.view.orders.OrdersGateway;
import com.maisonglow.view.orders.PaymentMethod;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Controller of {@code OrdersView.fxml}: the orders of the shop for the administrator (task I-R04). It lists them by
 * state, shows the detail of the chosen one and lets the administrator confirm the payment, cancel it or see the receipt
 * of the sale. An order waiting for payment is never shown as paid. Every call to the service runs in the background
 * with a loading veil, and an answer that arrives after the screen was discarded is ignored. The service checks the
 * permissions again; this screen only hides what the state does not allow.
 */
public final class OrdersController implements ViewLifecycle {

    private final OrdersGateway orders;
    private final Clock clock;
    private final ObservableList<OrderSummary> all = FXCollections.observableArrayList();
    private final FilteredList<OrderSummary> shown = new FilteredList<>(all);
    private Executor executor = ForkJoinPool.commonPool();
    private OrderFilter filter = OrderFilter.initial();
    private OrderDetail open;
    private AlertPresenter alerts;
    private LoadingOverlay loading;
    private boolean disposed;

    @FXML
    private ToggleGroup filters;
    @FXML
    private TextField searchField;
    @FXML
    private StackPane tableHost;
    @FXML
    private TableView<OrderSummary> ordersTable;
    @FXML
    private TableColumn<OrderSummary, Integer> numberColumn;
    @FXML
    private TableColumn<OrderSummary, String> customerColumn;
    @FXML
    private TableColumn<OrderSummary, LocalDateTime> dateColumn;
    @FXML
    private TableColumn<OrderSummary, BigDecimal> totalColumn;
    @FXML
    private TableColumn<OrderSummary, OrderState> stateColumn;
    @FXML
    private Label emptyLabel;
    @FXML
    private Label detailEmpty;
    @FXML
    private VBox detailBox;
    @FXML
    private Label detailTitle;
    @FXML
    private Label detailState;
    @FXML
    private Label detailCustomer;
    @FXML
    private HBox unpaidNote;
    @FXML
    private VBox linesBox;
    @FXML
    private Label totalLabel;
    @FXML
    private Label reservationLabel;
    @FXML
    private VBox paymentBox;
    @FXML
    private ComboBox<PaymentMethod> methodBox;
    @FXML
    private Label methodMessage;
    @FXML
    private Button payButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Button receiptButton;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public OrdersController(AppContext context) {
        Objects.requireNonNull(context, "context");
        this.orders = context.ordersGateway();
        this.clock = context.clock();
    }

    /**
     * Replaces where the calls to the service run, which the tests use to hold an answer back.
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
        alerts = new AlertPresenter(ordersTable);
        loading = LoadingOverlay.over(tableHost);
        setUpTable();
        setUpFilters();
        methodBox.getItems().setAll(PaymentMethod.values());
        methodBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(PaymentMethod method) {
                return method == null ? "" : method.label();
            }

            @Override
            public PaymentMethod fromString(String text) {
                return null;
            }
        });
        methodBox.valueProperty().addListener((observable, before, now) -> hide(methodMessage));
        payButton.setOnAction(event -> confirmPayment());
        cancelButton.setOnAction(event -> cancelOrder());
        receiptButton.setOnAction(event -> showReceipt());
        showDetail(null);
        refresh();
    }

    private void setUpTable() {
        numberColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().id()));
        customerColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().customerName()));
        dateColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().createdAt()));
        totalColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().total()));
        stateColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().state()));
        dateColumn.setCellFactory(column -> text(DateFormats::dateTime));
        totalColumn.setCellFactory(column -> text(CurrencyFormatter::format));
        stateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(OrderState state, boolean empty) {
                super.updateItem(state, empty);
                if (empty || state == null) {
                    setGraphic(null);
                    return;
                }
                Label chip = new Label(state.label());
                chip.getStyleClass().add(state.chipStyle());
                setGraphic(chip);
            }
        });
        ordersTable.setItems(shown);
        ordersTable.getSelectionModel().selectedItemProperty().addListener((observable, before, row) -> {
            if (row != null) {
                loadDetail(row.id());
            }
        });
    }

    private static <T> TableCell<OrderSummary, T> text(java.util.function.Function<T, String> format) {
        return new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : format.apply(item));
            }
        };
    }

    private void setUpFilters() {
        List<Toggle> chips = filters.getToggles();
        OrderFilter.Group[] groups = OrderFilter.Group.values();
        for (int index = 0; index < chips.size() && index < groups.length; index++) {
            chips.get(index).setUserData(groups[index]);
        }
        filters.selectedToggleProperty().addListener((observable, before, chip) -> {
            if (chip == null && before != null) {
                before.setSelected(true);
            } else if (chip != null) {
                apply(filter.withGroup((OrderFilter.Group) chip.getUserData()));
            }
        });
        searchField.textProperty().addListener((observable, before, text) -> apply(filter.withText(text)));
        apply(filter);
    }

    private void apply(OrderFilter newFilter) {
        filter = newFilter;
        shown.setPredicate(filter::matches);
        emptyLabel.setText(filter.text().isEmpty() ? "No hay pedidos en esta lista." : "Ningún pedido coincide con la búsqueda.");
    }

    /** Reads the orders again, keeping the one that was open if it is still there. */
    void refresh() {
        int keep = open == null ? 0 : open.summary().id();
        busy("Cargando pedidos…");
        BackgroundTask.run(executor, orders::list, list -> {
            free();
            all.setAll(list);
            if (keep != 0) {
                list.stream().filter(order -> order.id() == keep).findFirst()
                    .ifPresentOrElse(order -> ordersTable.getSelectionModel().select(order), () -> showDetail(null));
            }
        }, this::fail, () -> !disposed);
    }

    private void loadDetail(int orderId) {
        if (open != null && open.summary().id() == orderId) {
            return;
        }
        BackgroundTask.run(executor, () -> orders.detail(orderId), this::showDetail, this::fail, () -> !disposed);
    }

    private void showDetail(OrderDetail detail) {
        open = detail;
        boolean none = detail == null;
        detailEmpty.setVisible(none);
        detailEmpty.setManaged(none);
        detailBox.setVisible(!none);
        detailBox.setManaged(!none);
        hide(methodMessage);
        if (none) {
            return;
        }
        OrderSummary summary = detail.summary();
        detailTitle.setText("Pedido n.º " + summary.id());
        detailState.setText(summary.state().label());
        detailState.getStyleClass().removeAll("chip-gold", "chip-success", "chip-neutral", "chip-warning");
        detailState.getStyleClass().add(summary.state().chipStyle());
        detailCustomer.setText(summary.customerName() + " · " + DateFormats.dateTime(summary.createdAt()));
        boolean isOpen = summary.state().isOpen();
        unpaidNote.setVisible(isOpen);
        unpaidNote.setManaged(isOpen);
        linesBox.getChildren().setAll(detail.lines().stream().map(OrdersController::lineRow).toList());
        totalLabel.setText(CurrencyFormatter.format(summary.total()));
        reservationLabel.setText(reservationText(detail));
        paymentBox.setVisible(isOpen);
        paymentBox.setManaged(isOpen);
        methodBox.setValue(null);
        payButton.setVisible(isOpen);
        payButton.setManaged(isOpen);
        cancelButton.setVisible(isOpen);
        cancelButton.setManaged(isOpen);
        receiptButton.setVisible(summary.state().hasReceipt());
        receiptButton.setManaged(summary.state().hasReceipt());
    }

    private static Node lineRow(OrderDetail.Line line) {
        Label label = new Label(line.quantity() + " × " + line.name() + " · " + CurrencyFormatter.format(line.subtotal()));
        label.getStyleClass().add("form-hint");
        label.setWrapText(true);
        return label;
    }

    /** Says what happens to the stock of the order: reserved until when, released, or sold with its payment method. */
    private String reservationText(OrderDetail detail) {
        OrderSummary summary = detail.summary();
        return switch (summary.state()) {
            case PENDING_PAYMENT -> {
                if (summary.reservedUntil() == null) {
                    yield "Stock reservado.";
                }
                long minutes = Math.max(0, Duration.between(LocalDateTime.now(clock), summary.reservedUntil()).toMinutes());
                yield "Stock reservado hasta las " + DateFormats.time(summary.reservedUntil().toLocalTime())
                    + (minutes == 1 ? " (queda 1 min)" : " (quedan " + minutes + " min)");
            }
            case PAID -> "Pagado" + detail.paymentMethod().map(method -> " con " + method.label().toLowerCase()).orElse("")
                + detail.saleId().map(id -> " · Venta n.º " + id).orElse("") + ". El stock ya se descontó.";
            case CANCELLED -> "Cancelado: el stock reservado se liberó.";
            case EXPIRED -> "Venció la reserva: el stock se liberó.";
        };
    }

    private void confirmPayment() {
        if (open == null || !open.summary().state().isOpen()) {
            return;
        }
        PaymentMethod method = methodBox.getValue();
        if (method == null) {
            methodMessage.setText("Elige cómo pagó el cliente.");
            methodMessage.setVisible(true);
            methodMessage.setManaged(true);
            return;
        }
        OrderSummary summary = open.summary();
        if (!alerts.confirm("¿Confirmar el pago del pedido n.º " + summary.id() + " por "
            + CurrencyFormatter.format(summary.total()) + " (" + method.label().toLowerCase() + ")? "
            + "Se generará la venta y se descontará el stock.")) {
            return;
        }
        busy("Confirmando el pago…");
        BackgroundTask.run(executor, () -> orders.confirmPayment(summary.id(), method), saleId -> {
            free();
            alerts.toast("Pago confirmado. Venta n.º " + saleId + " generada.");
            open = null;
            refresh();
        }, this::fail, () -> !disposed);
    }

    private void cancelOrder() {
        if (open == null || !open.summary().state().isOpen()) {
            return;
        }
        int id = open.summary().id();
        if (!alerts.confirm("¿Cancelar el pedido n.º " + id + "? Se libera el stock reservado.")) {
            return;
        }
        busy("Cancelando el pedido…");
        BackgroundTask.run(executor, () -> {
            orders.cancel(id);
            return id;
        }, done -> {
            free();
            alerts.toast("Pedido cancelado.");
            open = null;
            refresh();
        }, this::fail, () -> !disposed);
    }

    private void showReceipt() {
        if (open == null || !open.summary().state().hasReceipt()) {
            return;
        }
        int id = open.summary().id();
        BackgroundTask.run(executor, () -> orders.receipt(id), alerts::showInfo, this::fail, () -> !disposed);
    }

    private void fail(Exception error) {
        free();
        alerts.showError(error);
    }

    private void busy(String message) {
        loading.show(message);
        payButton.setDisable(true);
        cancelButton.setDisable(true);
    }

    private void free() {
        loading.hide();
        payButton.setDisable(false);
        cancelButton.setDisable(false);
    }

    private static void hide(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }

    /** Closing the session: an answer that arrives later is ignored and the orders of the account are dropped. */
    @Override
    public void dispose() {
        disposed = true;
        all.clear();
        open = null;
    }
}
