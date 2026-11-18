package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.AppointmentStatus;
import com.maisonglow.model.Commerce.ProfessionalOption;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.TreatmentData;
import com.maisonglow.service.WorkSchedule;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLifecycle;
import com.maisonglow.view.ViewLoader;
import com.maisonglow.view.appointment.SlotPicker;
import com.maisonglow.view.shop.ShopFixture;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Mis citas: reservar, reprogramar y cancelar, sobre los servicios reales")
class MyAppointmentsViewTest {

    private static boolean toolkit;

    @TempDir
    Path data;

    private ShopFixture shop;
    private AppContext context;
    private Treatment cut;
    private Professional camila;
    private LocalDate day;
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
        shop.asAdmin();
        cut = context.treatments().register(new TreatmentData("Corte y peinado", "", 55000, 60));
        context.treatments().register(new TreatmentData("Manicure", "", 30000, 45));
        camila = context.professionals().register(new ContactData("2001", "Camila Ruiz", "3001112233", "c@correo.com"),
            "Estilista", new WorkSchedule(LocalTime.of(8, 0), LocalTime.of(18, 0)));
        context.professionals().assignTreatment(camila.getId(), cut.getId());
        context.professionals().register(new ContactData("2002", "Pedro Gómez", "3002223344", "p@correo.com"),
            "Manicurista", new WorkSchedule(LocalTime.of(8, 0), LocalTime.of(18, 0)));
        day = LocalDate.now(shop.clock).plusDays(3);
        shop.asCustomer("ana");
    }

    private ViewLoader.Loaded<MyAppointmentsController> open(boolean confirm) throws Exception {
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<MyAppointmentsController> loaded = ViewLoader.loadWithController(
                MyAppointmentsViewTest.class.getResource("/com/maisonglow/view/MyAppointmentsView.fxml"), context);
            loaded.controller().useExecutor(Runnable::run);
            loaded.controller().useAlerts(new AlertPresenter(() -> null, alert -> {
                dialogs.add(alert.getContentText());
                return alert.getButtonTypes().stream().filter(button -> confirm && "Confirmar".equals(button.getText())).findFirst();
            }));
            new Scene(loaded.root(), 1100, 720);
            loaded.root().applyCss();
            loaded.root().layout();
            return loaded;
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

    private static boolean disabled(Parent root, String id) throws Exception {
        settle();
        return FxTest.onFxThread(() -> root.lookup("#" + id).isDisabled());
    }

    private static boolean shown(Parent root, String id) throws Exception {
        settle();
        return FxTest.onFxThread(() -> root.lookup("#" + id).isVisible());
    }

    @SuppressWarnings("unchecked")
    private static void choose(Parent root, String comboId, int index) throws Exception {
        settle();
        FxTest.onFxThread(() -> {
            ComboBox<Object> box = (ComboBox<Object>) root.lookup("#" + comboId);
            box.setValue(box.getItems().get(index));
            return null;
        });
        settle();
    }

    private static void pickDate(Parent root, String pickerId, LocalDate date) throws Exception {
        FxTest.onFxThread(() -> {
            ((DatePicker) root.lookup("#" + pickerId)).setValue(date);
            return null;
        });
        settle();
    }

    private static List<String> hours(Parent root, String hostId) throws Exception {
        settle();
        return FxTest.onFxThread(() -> root.lookup("#" + hostId).lookupAll(".toggle-button").stream()
            .filter(ToggleButton.class::isInstance).map(node -> ((ToggleButton) node).getText()).toList());
    }

    private static void pickHour(Parent root, String hostId, String hour) throws Exception {
        settle();
        FxTest.onFxThread(() -> {
            root.lookup("#" + hostId).lookupAll(".toggle-button").stream().map(ToggleButton.class::cast)
                .filter(button -> hour.equals(button.getText())).findFirst().orElseThrow().fire();
            return null;
        });
        settle();
    }

    private static void press(Parent root, String id) throws Exception {
        settle();
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

    /** Fills the booking form up to the hour; leaves the "Reservar cita" button ready. */
    private void fillBooking(Parent root, String hour) throws Exception {
        choose(root, "treatmentBox", 0);
        choose(root, "professionalBox", 0);
        pickDate(root, "datePicker", day);
        pickHour(root, "slotsHost", hour);
    }

    private List<Appointment> mine() throws Exception {
        return context.portal().myAppointments();
    }

    private Appointment book(String user, int hour) throws Exception {
        shop.asCustomer(user);
        return context.portal().book(camila.getId(), cut.getId(), day.atTime(hour, 0));
    }

    // ---- Reservar ----

    @Test
    @DisplayName("opens with the active treatments and everything else waiting for the previous choice")
    void opensWaiting() throws Exception {
        Parent root = open(true).root();
        settle();

        assertThat(FxTest.onFxThread(() -> ((ComboBox<?>) root.lookup("#treatmentBox")).getItems().size())).isEqualTo(2);
        assertThat(disabled(root, "professionalBox")).isTrue();
        assertThat(disabled(root, "datePicker")).isTrue();
        assertThat(disabled(root, "bookButton")).isFalse();
        assertThat(texts(root, "slotsHost")).containsExactly("Elige un tratamiento para empezar.");
        // Only the treatment and the professional: there is no customer to choose.
        assertThat(FxTest.onFxThread(() -> root.lookupAll(".combo-box").size())).isEqualTo(2);
    }

    @Test
    @DisplayName("choosing a treatment shows its duration and price and only the professionals that offer it")
    void chooseTreatment() throws Exception {
        Parent root = open(true).root();

        choose(root, "treatmentBox", 0);

        assertThat(text(root, "treatmentSummary")).isEqualTo("60 min · $ 55.000");
        assertThat(FxTest.onFxThread(() -> ((ComboBox<?>) root.lookup("#professionalBox")).getItems().stream()
            .map(option -> ((ProfessionalOption) option).name()).toList())).containsExactly("Camila Ruiz");
        assertThat(disabled(root, "professionalBox")).isFalse();
        assertThat(disabled(root, "datePicker")).isTrue();
        assertThat(texts(root, "slotsHost")).containsExactly("Elige un profesional.");
    }

    @Test
    @DisplayName("a treatment nobody offers says so and offers no professional")
    void treatmentWithoutProfessionals() throws Exception {
        Parent root = open(true).root();

        choose(root, "treatmentBox", 1);

        assertThat(disabled(root, "professionalBox")).isTrue();
        assertThat(texts(root, "slotsHost")).containsExactly("Por ahora ningún profesional ofrece este tratamiento.");
    }

    @Test
    @DisplayName("only the free hours are offered: inside the schedule and not overlapping another appointment")
    void onlyFreeHours() throws Exception {
        book("laura", 10);
        shop.asCustomer("ana");
        Parent root = open(true).root();
        choose(root, "treatmentBox", 0);
        choose(root, "professionalBox", 0);

        pickDate(root, "datePicker", day);

        List<String> offered = hours(root, "slotsHost");
        assertThat(offered).contains("08:00", "09:00", "11:00", "17:00");
        assertThat(offered).doesNotContain("07:00", "07:30", "09:30", "10:00", "10:30", "17:30", "18:00");
    }

    @Test
    @DisplayName("pressing Reservar with nothing chosen says what is missing under each field and books nothing")
    void messagesUnderEachField() throws Exception {
        Parent root = open(true).root();
        settle();

        press(root, "bookButton");

        assertThat(text(root, "treatmentMessage")).isEqualTo("Elige un tratamiento.");
        assertThat(text(root, "professionalMessage")).isEqualTo("Elige un profesional.");
        assertThat(text(root, "dateMessage")).isEqualTo("Elige la fecha.");
        assertThat(text(root, "slotMessage")).isEqualTo("Elige un horario.");
        assertThat(shown(root, "treatmentMessage")).isTrue();

        choose(root, "treatmentBox", 0);
        assertThat(shown(root, "treatmentMessage")).isFalse();
        assertThat(shown(root, "dateMessage")).isTrue();
    }

    @Test
    @DisplayName("past days cannot be chosen")
    void pastDaysAreOff() throws Exception {
        Parent root = open(true).root();
        settle();

        boolean off = FxTest.onFxThread(() -> {
            DatePicker picker = (DatePicker) root.lookup("#datePicker");
            DateCell cell = picker.getDayCellFactory().call(picker);
            cell.updateItem(LocalDate.now(shop.clock).minusDays(1), false);
            DateCell future = picker.getDayCellFactory().call(picker);
            future.updateItem(LocalDate.now(shop.clock).plusDays(1), false);
            return cell.isDisabled() && !future.isDisabled();
        });

        assertThat(off).isTrue();
    }

    @Test
    @DisplayName("booking creates a pending appointment of the customer of the session, with no deposit paid, and shows it")
    void books() throws Exception {
        Parent root = open(true).root();
        fillBooking(root, "09:00");
        assertThat(disabled(root, "bookButton")).isFalse();

        press(root, "bookButton");

        List<Appointment> appointments = mine();
        assertThat(appointments).singleElement().satisfies(appointment -> {
            assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PENDING);
            assertThat(appointment.getCustomer().getName()).isEqualTo("Ana García");
            assertThat(appointment.getStart()).isEqualTo(day.atTime(9, 0));
            assertThat(appointment.getDeposit()).isEqualByComparingTo("0");
        });
        List<String> card = texts(root, "appointmentsBox");
        assertThat(card).contains("Corte y peinado", "Pendiente");
        assertThat(card).anyMatch(row -> row.contains("Camila Ruiz"));
        assertThat(card).anyMatch(row -> row.startsWith("Anticipo que se pide: $") && row.contains("registrado: $ 0"));
        assertThat(shown(root, "appointmentsEmpty")).isFalse();
        assertThat(disabled(root, "bookButton")).isTrue();
        assertThat(texts(root, "slotsHost")).containsExactly("Elige un tratamiento para empezar.");
    }

    @Test
    @DisplayName("an hour taken by someone else while the customer was choosing is refused with the reason and the hours are read again")
    void takenMeanwhile() throws Exception {
        Parent root = open(true).root();
        fillBooking(root, "09:00");
        book("laura", 9);
        shop.asCustomer("ana");

        press(root, "bookButton");

        assertThat(mine()).isEmpty();
        assertThat(dialogs).isNotEmpty();
        assertThat(hours(root, "slotsHost")).doesNotContain("09:00");
    }

    // ---- Mis citas ----

    @Test
    @DisplayName("with no appointments the list says where to start; with some, only the customer's own show, the next one first")
    void listsOnlyMine() throws Exception {
        Parent empty = open(true).root();
        assertThat(shown(empty, "appointmentsEmpty")).isTrue();

        Appointment later = book("ana", 15);
        Appointment sooner = book("ana", 9);
        book("laura", 12);
        shop.asCustomer("ana");
        Parent root = open(true).root();

        List<String> texts = texts(root, "appointmentsBox");
        assertThat(texts.stream().filter(text -> text.contains("Camila Ruiz")).toList())
            .hasSize(2).first().asString().contains("09:00");
        assertThat(texts.stream().filter(text -> text.contains("Camila Ruiz")).toList().get(1)).contains("15:00");
        assertThat(texts).noneMatch(text -> text.contains("12:00 ·"));
        assertThat(sooner.getId()).isNotEqualTo(later.getId());
    }

    @Test
    @DisplayName("cancelling asks first and turns the appointment into cancelled, with no more buttons")
    void cancels() throws Exception {
        Appointment appointment = book("ana", 9);
        Parent root = open(true).root();

        pressButton(root, "Cancelar", 0);

        assertThat(dialogs).anyMatch(text -> text.contains("¿Cancelar tu cita de Corte y peinado"));
        assertThat(context.portal().myAppointments()).singleElement()
            .satisfies(cancelled -> assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED));
        assertThat(texts(root, "appointmentsBox")).contains("Cancelada");
        assertThat(FxTest.onFxThread(() -> root.lookupAll(".button").stream().filter(Button.class::isInstance)
            .map(button -> ((Button) button).getText()).toList())).doesNotContain("Reprogramar");
        assertThat(appointment.getId()).isPositive();
    }

    @Test
    @DisplayName("answering no to the cancellation changes nothing")
    void declinedCancellation() throws Exception {
        book("ana", 9);
        Parent root = open(false).root();

        pressButton(root, "Cancelar", 0);

        assertThat(mine()).singleElement().satisfies(a -> assertThat(a.getStatus()).isEqualTo(AppointmentStatus.PENDING));
    }

    @Test
    @DisplayName("the service refuses to change an appointment too close to its start, and the reason is shown")
    void tooLateToChange() throws Exception {
        Appointment appointment = book("ana", 9);
        Parent root = open(true).root();
        long minutes = java.time.Duration.between(LocalDateTime.now(shop.clock), appointment.getStart()).toMinutes() - 30;
        shop.advance(minutes);
        // A session lasts 8 hours: after two days the customer has signed in again.
        shop.asCustomer("ana");

        pressButton(root, "Cancelar", 0);

        assertThat(dialogs).anyMatch(text -> text.contains("plazo"));
        assertThat(mine()).singleElement().satisfies(a -> assertThat(a.getStatus()).isEqualTo(AppointmentStatus.PENDING));
    }

    @Test
    @DisplayName("rescheduling opens the same hours picker for that appointment, moves it and closes")
    void reschedules() throws Exception {
        Appointment appointment = book("ana", 9);
        Parent root = open(true).root();
        assertThat(shown(root, "rescheduleCard")).isFalse();

        pressButton(root, "Reprogramar", 0);
        assertThat(shown(root, "rescheduleCard")).isTrue();
        assertThat(text(root, "rescheduleTitle")).isEqualTo("Reprogramar: Corte y peinado");
        assertThat(disabled(root, "rescheduleButton")).isTrue();
        pickDate(root, "rescheduleDate", day.plusDays(1));
        assertThat(hours(root, "rescheduleSlotsHost")).contains("08:00", "14:00").doesNotContain("07:00");
        pickHour(root, "rescheduleSlotsHost", "14:00");
        press(root, "rescheduleButton");

        assertThat(mine()).singleElement().satisfies(moved -> {
            assertThat(moved.getId()).isEqualTo(appointment.getId());
            assertThat(moved.getStart()).isEqualTo(day.plusDays(1).atTime(14, 0));
        });
        assertThat(shown(root, "rescheduleCard")).isFalse();
        assertThat(texts(root, "appointmentsBox")).anyMatch(row -> row.contains("14:00"));
    }

    @Test
    @DisplayName("closing the reschedule card puts nothing off")
    void closesTheRescheduleCard() throws Exception {
        book("ana", 9);
        Parent root = open(true).root();
        pressButton(root, "Reprogramar", 0);

        press(root, "rescheduleClose");

        assertThat(shown(root, "rescheduleCard")).isFalse();
        assertThat(mine()).singleElement().satisfies(a -> assertThat(a.getStart()).isEqualTo(day.atTime(9, 0)));
    }

    // ---- Piezas ----

    @Test
    @DisplayName("the hours asked about run every half hour from 07:00 to 19:30")
    void candidateHours() {
        List<LocalTime> hours = SlotPicker.candidates();

        assertThat(hours.getFirst()).isEqualTo(LocalTime.of(7, 0));
        assertThat(hours.getLast()).isEqualTo(LocalTime.of(19, 30));
        assertThat(hours).hasSize(26).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("an answer that arrives after the session closed is ignored and nothing is shown")
    void lateAnswerIsIgnored() throws Exception {
        book("ana", 9);
        ViewLoader.Loaded<MyAppointmentsController> loaded = FxTest.onFxThread(() -> {
            ViewLoader.Loaded<MyAppointmentsController> opened = ViewLoader.loadWithController(
                MyAppointmentsViewTest.class.getResource("/com/maisonglow/view/MyAppointmentsView.fxml"), context);
            new Scene(opened.root(), 1100, 720);
            opened.root().applyCss();
            opened.root().layout();
            ((ViewLifecycle) opened.controller()).dispose();
            return opened;
        });
        Thread.sleep(300);
        settle();

        assertThat(texts(loaded.root(), "appointmentsBox")).isEmpty();
        assertThat(Optional.of(held)).isPresent();
    }
}
