package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.AppointmentStatus;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.TreatmentData;
import com.maisonglow.service.WorkSchedule;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Agenda view tests")
class AppointmentsViewTest {

    private static boolean toolkit;

    @TempDir
    Path data;

    private AppContext context;
    private LocalDate tomorrow;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @BeforeEach
    void prepareData() throws Exception {
        AppContext.seed(data);
        context = new AppContext(data);
        tomorrow = LocalDate.now().plusDays(1);
        Customer ana = context.customers().register(new ContactData("1001", "Ana García", "3001112233", "ana@correo.com"));
        Customer laura = context.customers().register(new ContactData("1002", "Laura Méndez", "3124445566", "laura@correo.com"));
        Treatment manicure = context.treatments().register(new TreatmentData("Manicura gel", "", 45000, 50));
        Treatment cut = context.treatments().register(new TreatmentData("Corte y peinado", "", 55000, 60));
        Professional camila = context.professionals().register(new ContactData("2001", "Camila Ruiz", "3001112233", "c@correo.com"),
            "Estilista", new WorkSchedule(LocalTime.of(8, 0), LocalTime.of(17, 0)));
        Professional daniela = context.professionals().register(new ContactData("2002", "Daniela Soto", "3124445566", "d@correo.com"),
            "Manicurista", new WorkSchedule(LocalTime.of(9, 0), LocalTime.of(18, 0)));
        context.professionals().assignTreatment(camila.getId(), cut.getId());
        context.professionals().assignTreatment(camila.getId(), manicure.getId());
        context.professionals().assignTreatment(daniela.getId(), manicure.getId());
        camila = context.professionals().findById(camila.getId());
        daniela = context.professionals().findById(daniela.getId());
        context.appointments().book(ana, camila, cut, tomorrow.atTime(10, 0), 16500);
        context.appointments().book(laura, daniela, manicure, tomorrow.atTime(9, 30), 13500);
        context.appointments().book(ana, camila, manicure, tomorrow.atTime(14, 0), 13500);
    }

    private Parent open() throws Exception {
        return open(true);
    }

    /** Opens the agenda; a confirmation is answered as the user would, with "Confirmar" or by cancelling. */
    private Parent open(boolean confirm) throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<AppointmentsController> loaded = ViewLoader.loadWithController(
                AppointmentsViewTest.class.getResource("/com/maisonglow/view/AppointmentsView.fxml"), context);
            loaded.controller().useAlerts(new AlertPresenter(() -> null, alert -> alert.getButtonTypes().stream()
                .filter(button -> confirm && "Confirmar".equals(button.getText())).findFirst()));
            Parent root = loaded.root();
            new Scene(root, 1100, 700);
            root.applyCss();
            root.layout();
            return root;
        });
    }

    private static void goTo(Parent root, LocalDate day) throws Exception {
        FxTest.onFxThread(() -> {
            ((DatePicker) root.lookup("#dayPicker")).setValue(day);
            root.applyCss();
            root.layout();
            return null;
        });
    }

    private static void press(Parent root, String id) throws Exception {
        FxTest.onFxThread(() -> {
            ((Button) root.lookup("#" + id)).fire();
            root.applyCss();
            root.layout();
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private static List<Appointment> rows(Parent root) throws Exception {
        TableView<Appointment> table = (TableView<Appointment>) root.lookup("#appointmentsTable");
        return FxTest.onFxThread(() -> List.copyOf(table.getItems()));
    }

    private static String text(Parent root, String id) throws Exception {
        return FxTest.onFxThread(() -> ((Label) root.lookup("#" + id)).getText());
    }

    private static LocalDate day(Parent root) throws Exception {
        return FxTest.onFxThread(() -> ((DatePicker) root.lookup("#dayPicker")).getValue());
    }

    /** Chooses the row of the day at that position, as a click on the table would. */
    @SuppressWarnings("unchecked")
    private static void select(Parent root, int index) throws Exception {
        TableView<Appointment> table = (TableView<Appointment>) root.lookup("#appointmentsTable");
        FxTest.onFxThread(() -> {
            table.getSelectionModel().select(index);
            return null;
        });
    }

    private static boolean disabled(Parent root, String id) throws Exception {
        return FxTest.onFxThread(() -> root.lookup("#" + id).isDisabled());
    }

    private Appointment stored(int index) throws Exception {
        return context.appointments().listByDate(tomorrow).stream().sorted(java.util.Comparator.comparing(Appointment::getStart)).toList().get(index);
    }

    @Test
    @DisplayName("opens on today, with a capitalised title and nothing booked")
    void opensOnToday() throws Exception {
        Parent root = open();

        assertThat(day(root)).isEqualTo(LocalDate.now());
        assertThat(rows(root)).isEmpty();
        assertThat(text(root, "dayTitle")).matches("^\\p{Lu}.*");
        assertThat(text(root, "countLabel")).isEqualTo("0 citas, 0 por atender");
    }

    @Test
    @DisplayName("shows the appointments of the day in order of time, with the count of the ones still to attend")
    void listsTheDayInOrder() throws Exception {
        Parent root = open();

        goTo(root, tomorrow);

        assertThat(rows(root)).extracting(a -> a.getCustomer().getName() + " " + a.getStart().toLocalTime())
            .containsExactly("Laura Méndez 09:30", "Ana García 10:00", "Ana García 14:00");
        assertThat(text(root, "countLabel")).isEqualTo("3 citas, 3 por atender");
    }

    @Test
    @DisplayName("each row shows the time range, the treatment, the professional, the state and the deposit")
    void showsTheDetails() throws Exception {
        Parent root = open();

        goTo(root, tomorrow);
        List<String> cells = FxTest.onFxThread(() -> root.lookupAll(".table-cell").stream()
            .filter(node -> node instanceof javafx.scene.control.Labeled)
            .map(node -> ((javafx.scene.control.Labeled) node).getText()).filter(text -> text != null).toList());
        List<String> chips = FxTest.onFxThread(() -> root.lookupAll(".chip-gold").stream().map(node -> ((Label) node).getText()).toList());

        assertThat(cells).contains("09:30 - 10:20", "10:00 - 11:00", "Manicura gel", "Camila Ruiz",
            CurrencyFormatter.format(new BigDecimal("16500")));
        assertThat(chips).containsExactly("Pendiente", "Pendiente", "Pendiente");
    }

    @Test
    @DisplayName("the arrows move one day at a time and Hoy comes back")
    void movesBetweenDays() throws Exception {
        Parent root = open();

        press(root, "nextButton");
        assertThat(day(root)).isEqualTo(LocalDate.now().plusDays(1));
        assertThat(rows(root)).hasSize(3);

        press(root, "nextButton");
        press(root, "previousButton");
        assertThat(day(root)).isEqualTo(LocalDate.now().plusDays(1));

        press(root, "todayButton");
        assertThat(day(root)).isEqualTo(LocalDate.now());
        assertThat(rows(root)).isEmpty();
    }

    @Test
    @DisplayName("clearing the date keeps the day that was showing")
    void clearingTheDateKeepsTheDay() throws Exception {
        Parent root = open();
        goTo(root, tomorrow);

        FxTest.onFxThread(() -> {
            ((DatePicker) root.lookup("#dayPicker")).setValue(null);
            return null;
        });

        assertThat(day(root)).isEqualTo(tomorrow);
        assertThat(rows(root)).hasSize(3);
    }

    @Test
    @DisplayName("the state buttons are off until an appointment is chosen")
    void buttonsStartOff() throws Exception {
        Parent root = open();
        goTo(root, tomorrow);

        for (String id : List.of("confirmButton", "completeButton", "noShowButton", "cancelAppointmentButton")) {
            assertThat(disabled(root, id)).as(id).isTrue();
        }

        select(root, 0);
        for (String id : List.of("confirmButton", "completeButton", "noShowButton", "cancelAppointmentButton")) {
            assertThat(disabled(root, id)).as(id).isFalse();
        }
    }

    @Test
    @DisplayName("Confirmar moves a pending appointment to confirmed and can no longer be pressed")
    void confirms() throws Exception {
        Parent root = open();
        goTo(root, tomorrow);
        select(root, 0);

        press(root, "confirmButton");

        assertThat(stored(0).getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(FxTest.onFxThread(() -> root.lookupAll(".chip-success").stream().map(node -> ((Label) node).getText()).toList()))
            .containsExactly("Confirmada");
        assertThat(disabled(root, "confirmButton")).isTrue();
        assertThat(disabled(root, "completeButton")).isFalse();
    }

    @Test
    @DisplayName("Completar closes the appointment and no state button is left on")
    void completes() throws Exception {
        Parent root = open();
        goTo(root, tomorrow);
        select(root, 0);

        press(root, "completeButton");

        assertThat(stored(0).getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(text(root, "countLabel")).isEqualTo("3 citas, 2 por atender");
        for (String id : List.of("confirmButton", "completeButton", "noShowButton", "cancelAppointmentButton")) {
            assertThat(disabled(root, id)).as(id).isTrue();
        }
    }

    @Test
    @DisplayName("Inasistencia asks first, then marks the appointment and adds one to the customer")
    void marksANoShow() throws Exception {
        Parent root = open(true);
        goTo(root, tomorrow);
        select(root, 0);

        press(root, "noShowButton");

        assertThat(stored(0).getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);
        assertThat(context.customers().findByDocument("1002").orElseThrow().getNoShowCount()).isEqualTo(1);
        assertThat(FxTest.onFxThread(() -> root.lookupAll(".chip-warning").stream().map(node -> ((Label) node).getText()).toList()))
            .containsExactly("Inasistencia");
        assertThat(FxTest.onFxThread(() -> root.lookupAll(".row-noshow").size())).isEqualTo(1);
    }

    @Test
    @DisplayName("cancelling asks first and frees the schedule of the professional")
    void cancelsAndFreesTheSchedule() throws Exception {
        Parent root = open(true);
        goTo(root, tomorrow);
        select(root, 1);
        Appointment cut = stored(1);
        assertThat(context.appointments().checkAvailability(cut.getProfessional(), cut.getStart(), cut.getEnd())).isFalse();

        press(root, "cancelAppointmentButton");

        assertThat(stored(1).getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(context.appointments().checkAvailability(cut.getProfessional(), cut.getStart(), cut.getEnd())).isTrue();
        assertThat(text(root, "countLabel")).isEqualTo("3 citas, 2 por atender");
    }

    @Test
    @DisplayName("answering no to the confirmation changes nothing")
    void declinedConfirmationChangesNothing() throws Exception {
        Parent root = open(false);
        goTo(root, tomorrow);
        select(root, 0);

        press(root, "cancelAppointmentButton");
        press(root, "noShowButton");

        assertThat(stored(0).getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(context.customers().findByDocument("1002").orElseThrow().getNoShowCount()).isZero();
    }

    @Test
    @DisplayName("a cancelled appointment goes quiet in the table and offers no more changes")
    void cancelledGoesQuiet() throws Exception {
        Parent root = open(true);
        goTo(root, tomorrow);
        select(root, 0);

        press(root, "cancelAppointmentButton");

        assertThat(FxTest.onFxThread(() -> root.lookupAll(".row-muted").size())).isEqualTo(1);
        assertThat(disabled(root, "confirmButton")).isTrue();
        assertThat(disabled(root, "cancelAppointmentButton")).isTrue();
    }
}
