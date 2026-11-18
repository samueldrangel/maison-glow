package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.AppointmentStatus;
import com.maisonglow.model.Commerce.Order;
import com.maisonglow.model.Commerce.OrderStatus;
import com.maisonglow.model.Customer;
import com.maisonglow.service.CustomerPortalService;
import com.maisonglow.service.CustomerStatistics;
import com.maisonglow.service.OrderService;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.util.DateFormats;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.BackgroundTask;
import com.maisonglow.view.ViewLifecycle;
import com.maisonglow.view.appointment.AppointmentLook;
import com.maisonglow.view.orders.OrderState;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Controller of {@code CustomerHomeView.fxml}: the first screen of a customer (task N-R01). It greets them by name and shows
 * what is theirs: how many appointments and purchases they have, the orders they still have to pay, the next appointment
 * and the latest orders. Everything comes from the account that signed in through {@link CustomerPortalService} and
 * {@link OrderService}, which take the customer from the session: nothing here chooses a customer. The data is read in the
 * background and an answer that arrives after the session closed is ignored. A card with nothing to show says so and says
 * where to start.
 */
public final class CustomerHomeController implements ViewLifecycle {

    private static final int RECENT_ORDERS = 3;

    /** What the screen shows, read in one go in the background. */
    private record Snapshot(Customer customer, CustomerStatistics summary, Optional<Appointment> next, List<Order> orders) { }

    private final CustomerPortalService portal;
    private final OrderService orders;
    private final Clock clock;
    private Executor executor = ForkJoinPool.commonPool();
    private AlertPresenter alerts;
    private boolean disposed;

    @FXML
    private Label greetingLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label summaryLabel;
    @FXML
    private Label appointmentsValue;
    @FXML
    private Label appointmentsNote;
    @FXML
    private Label purchasesValue;
    @FXML
    private Label purchasesNote;
    @FXML
    private Label pendingOrdersValue;
    @FXML
    private Label nextState;
    @FXML
    private VBox nextBox;
    @FXML
    private Label nextEmpty;
    @FXML
    private Label ordersCount;
    @FXML
    private VBox ordersBox;
    @FXML
    private Label ordersEmpty;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public CustomerHomeController(AppContext context) {
        Objects.requireNonNull(context, "context");
        this.portal = context.portal();
        this.orders = context.orders();
        this.clock = context.clock();
    }

    /**
     * Replaces where the data is read, which the tests use to hold an answer back.
     *
     * @param newExecutor runs the reading
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
        alerts = new AlertPresenter(greetingLabel);
        LocalDateTime now = LocalDateTime.now(clock);
        greetingLabel.setText(HomeController.greeting(now.getHour()));
        String date = DateFormats.longDate(now.toLocalDate());
        dateLabel.setText(date.substring(0, 1).toUpperCase() + date.substring(1));
        summaryLabel.setText("Estamos preparando tu información…");
        showNext(Optional.empty());
        showOrders(List.of());
        BackgroundTask.run(executor, this::read, this::show, error -> alerts.showError(error), () -> !disposed);
    }

    private Snapshot read() throws BusinessRuleException, DataAccessException {
        try {
            LocalDateTime now = LocalDateTime.now(clock);
            Optional<Appointment> next = portal.myAppointments().stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.PENDING
                    || appointment.getStatus() == AppointmentStatus.CONFIRMED)
                .filter(appointment -> !appointment.getStart().isBefore(now))
                .min(Comparator.comparing(Appointment::getStart));
            List<Order> recent = orders.mine().stream().sorted(Comparator.comparing(Order::createdAt).reversed())
                .limit(RECENT_ORDERS).toList();
            return new Snapshot(portal.profile(), portal.summary(), next, recent);
        } catch (com.maisonglow.exception.ValidationException e) {
            throw new BusinessRuleException(e.getMessage(), e);
        }
    }

    private void show(Snapshot snapshot) {
        LocalDateTime now = LocalDateTime.now(clock);
        greetingLabel.setText(HomeController.greeting(now.getHour()) + ", " + firstName(snapshot.customer().getName()));
        CustomerStatistics summary = snapshot.summary();
        appointmentsValue.setText(String.valueOf(summary.appointments()));
        appointmentsNote.setText(summary.completedAppointments() + (summary.completedAppointments() == 1 ? " atendida" : " atendidas"));
        purchasesValue.setText(String.valueOf(summary.purchases()));
        purchasesNote.setText(CurrencyFormatter.format(summary.totalSpent()) + " en total");
        long unpaid = snapshot.orders().stream().filter(order -> order.status() == OrderStatus.PENDING_PAYMENT).count();
        pendingOrdersValue.setText(String.valueOf(unpaid));
        showNext(snapshot.next());
        showOrders(snapshot.orders());
        summaryLabel.setText(summary(snapshot.next(), unpaid));
    }

    /** @return the sentence of the header: what comes next for the customer */
    static String summary(Optional<Appointment> next, long unpaidOrders) {
        String appointment = next.map(a -> "Tu próxima cita es el " + DateFormats.date(a.getStart().toLocalDate()) + " a las "
            + DateFormats.time(a.getStart().toLocalTime()) + ".").orElse("No tienes citas próximas.");
        if (unpaidOrders == 0) {
            return appointment;
        }
        return appointment + (unpaidOrders == 1 ? " Tienes 1 pedido por pagar en el local." : " Tienes " + unpaidOrders + " pedidos por pagar en el local.");
    }

    private static String firstName(String fullName) {
        String trimmed = fullName == null ? "" : fullName.trim();
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }

    private void showNext(Optional<Appointment> next) {
        nextEmpty.setVisible(next.isEmpty());
        nextEmpty.setManaged(next.isEmpty());
        nextState.setVisible(next.isPresent());
        nextState.setManaged(next.isPresent());
        nextBox.setVisible(next.isPresent());
        nextBox.setManaged(next.isPresent());
        nextBox.getChildren().clear();
        next.ifPresent(appointment -> {
            AppointmentLook look = AppointmentLook.of(appointment.getStatus());
            nextState.setText(look.label());
            nextState.getStyleClass().removeAll("chip-gold", "chip-success", "chip-rose", "chip-neutral", "chip-warning");
            nextState.getStyleClass().add(look.chipStyle());
            Label what = new Label(appointment.getTreatment().getName());
            what.getStyleClass().add("home-row-title");
            Label when = new Label(DateFormats.dateTime(appointment.getStart()) + " · " + appointment.getProfessional().getName());
            when.getStyleClass().add("home-row-detail");
            nextBox.getChildren().addAll(what, when);
        });
    }

    private void showOrders(List<Order> recent) {
        ordersEmpty.setVisible(recent.isEmpty());
        ordersEmpty.setManaged(recent.isEmpty());
        ordersCount.setText(recent.size() + (recent.size() == 1 ? " pedido" : " pedidos"));
        ordersBox.getChildren().setAll(recent.stream().map(this::orderRow).toList());
    }

    private Node orderRow(Order order) {
        OrderState state = OrderState.valueOf(order.status().name());
        Label title = new Label("Pedido n.º " + order.id() + " · " + CurrencyFormatter.format(order.total()));
        title.getStyleClass().add("home-row-title");
        String detail = order.status() == OrderStatus.PENDING_PAYMENT
            ? "Págalo en el local antes de las " + DateFormats.time(order.expiresAt().toLocalTime())
            : DateFormats.dateTime(order.createdAt());
        Label what = new Label(detail);
        what.getStyleClass().add("home-row-detail");
        Label chip = new Label(state.label());
        chip.getStyleClass().add(state.chipStyle());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(12, new VBox(2, title, what), spacer, chip);
        row.getStyleClass().add("home-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** Closing the session: an answer that arrives later is ignored and what was shown is dropped. */
    @Override
    public void dispose() {
        disposed = true;
        ordersBox.getChildren().clear();
        nextBox.getChildren().clear();
    }
}
