package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.AppointmentStatus;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Product;
import com.maisonglow.model.Sale;
import com.maisonglow.service.SaleService;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.util.DateFormats;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.appointment.AppointmentLook;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Controller of {@code HomeView.fxml}: what is happening today. It only reads what the other screens already
 * keep (today's appointments, today's sales and the products below their minimum) and shows it; it creates no data.
 */
public final class HomeController {

    private final AppContext context;
    private AlertPresenter alerts;

    @FXML
    private Label greetingLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label todayValue;
    @FXML
    private Label todayNote;
    @FXML
    private Label pendingValue;
    @FXML
    private Label salesValue;
    @FXML
    private Label salesNote;
    @FXML
    private Label lowStockValue;
    @FXML
    private VBox agendaBox;
    @FXML
    private Label agendaEmpty;
    @FXML
    private VBox restockBox;
    @FXML
    private Label restockEmpty;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public HomeController(AppContext context) {
        this.context = Objects.requireNonNull(context, "context");
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
        LocalDateTime now = LocalDateTime.now(context.clock());
        greetingLabel.setText(greeting(now.getHour()));
        String date = DateFormats.longDate(now.toLocalDate());
        dateLabel.setText(date.substring(0, 1).toUpperCase() + date.substring(1));
        try {
            showAppointments(now.toLocalDate());
            showSales(now.toLocalDate());
            showStock();
        } catch (ValidationException | DataAccessException e) {
            alerts.showError(e);
        }
    }

    /** @return the greeting that fits the hour of the day */
    static String greeting(int hour) {
        if (hour < 12) {
            return "Buenos días";
        }
        return hour < 19 ? "Buenas tardes" : "Buenas noches";
    }

    private void showAppointments(LocalDate today) throws ValidationException, DataAccessException {
        List<Appointment> ofToday = context.appointments().listByDate(today).stream()
            .sorted(Comparator.comparing(Appointment::getStart)).toList();
        long toAttend = ofToday.stream().filter(HomeController::isOpen).count();
        todayValue.setText(String.valueOf(ofToday.size()));
        todayNote.setText(toAttend + " por atender");
        pendingValue.setText(String.valueOf(ofToday.stream().filter(a -> a.getStatus() == AppointmentStatus.PENDING).count()));
        agendaBox.getChildren().setAll(ofToday.stream().map(HomeController::appointmentRow).toList());
        agendaEmpty.setVisible(ofToday.isEmpty());
        agendaEmpty.setManaged(ofToday.isEmpty());
    }

    private static boolean isOpen(Appointment appointment) {
        return appointment.getStatus() == AppointmentStatus.PENDING || appointment.getStatus() == AppointmentStatus.CONFIRMED;
    }

    private static Node appointmentRow(Appointment appointment) {
        Label time = new Label(DateFormats.time(appointment.getStart().toLocalTime()));
        time.getStyleClass().add("stat-value");
        time.setMinWidth(52);
        Label what = new Label(appointment.getCustomer().getName() + " · " + appointment.getTreatment().getName());
        what.getStyleClass().add("form-hint");
        Label who = new Label(appointment.getProfessional().getName());
        who.getStyleClass().add("stat-caption");
        VBox text = new VBox(2, what, who);
        AppointmentLook look = AppointmentLook.of(appointment.getStatus());
        Label chip = new Label(look.label());
        chip.getStyleClass().add(look.chipStyle());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(12, time, text, spacer, chip);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void showSales(LocalDate today) throws ValidationException, DataAccessException {
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (Customer customer : context.customers().listAll()) {
            for (Sale sale : context.sales().listByCustomer(customer.getId())) {
                if (sale.getSoldAt().toLocalDate().equals(today)) {
                    total = total.add(SaleService.calculateTotal(sale));
                    count++;
                }
            }
        }
        salesValue.setText(CurrencyFormatter.format(total));
        salesNote.setText(count + (count == 1 ? " venta" : " ventas"));
    }

    private void showStock() throws DataAccessException {
        List<Product> low = context.products().listLowStock();
        lowStockValue.setText(String.valueOf(low.size()));
        restockBox.getChildren().setAll(low.stream().map(product -> {
            Label row = new Label(product.getName() + " · quedan " + product.getStock() + " de " + product.getMinimumStock());
            row.getStyleClass().add("form-hint");
            return (Node) row;
        }).toList());
        restockEmpty.setVisible(low.isEmpty());
        restockEmpty.setManaged(low.isEmpty());
    }
}
