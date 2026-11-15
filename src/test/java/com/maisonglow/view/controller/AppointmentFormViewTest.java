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
import com.maisonglow.util.DateFormats;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
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
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Agenda form tests")
class AppointmentFormViewTest {

    private static boolean toolkit;

    @TempDir
    Path data;

    private AppContext context;
    private LocalDate tomorrow;
    private Customer ana;
    private Customer laura;
    private Treatment manicure;
    private Treatment cut;
    private Treatment facial;
    private Professional camila;
    private Professional daniela;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @BeforeEach
    void prepareData() throws Exception {
        AppContext.seed(data);
        context = new AppContext(data);
        tomorrow = LocalDate.now().plusDays(1);
        ana = context.customers().register(new ContactData("1001", "Ana García", "3001112233", "ana@correo.com"));
        laura = context.customers().register(new ContactData("1002", "Laura Méndez", "3124445566", "laura@correo.com"));
        manicure = context.treatments().register(new TreatmentData("Manicura gel", "", 45000, 50));
        cut = context.treatments().register(new TreatmentData("Corte y peinado", "", 55000, 60));
        facial = context.treatments().register(new TreatmentData("Limpieza facial", "", 38000, 45));
        context.treatments().deactivate(facial.getId());
        camila = context.professionals().register(new ContactData("2001", "Camila Ruiz", "3001112233", "c@correo.com"),
            "Estilista", new WorkSchedule(LocalTime.of(8, 0), LocalTime.of(17, 0)));
        daniela = context.professionals().register(new ContactData("2002", "Daniela Soto", "3124445566", "d@correo.com"),
            "Manicurista", new WorkSchedule(LocalTime.of(9, 0), LocalTime.of(18, 0)));
        context.professionals().assignTreatment(camila.getId(), cut.getId());
        context.professionals().assignTreatment(camila.getId(), manicure.getId());
        context.professionals().assignTreatment(daniela.getId(), manicure.getId());
        camila = context.professionals().findById(camila.getId());
        daniela = context.professionals().findById(daniela.getId());
    }

    private Parent open() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<AppointmentsController> loaded = ViewLoader.loadWithController(
                AppointmentFormViewTest.class.getResource("/com/maisonglow/view/AppointmentsView.fxml"), context);
            loaded.controller().useAlerts(new AlertPresenter(() -> null, alert -> alert.getButtonTypes().stream()
                .filter(button -> "Confirmar".equals(button.getText())).findFirst()));
            Parent root = loaded.root();
            new Scene(root, 1300, 800);
            root.applyCss();
            root.layout();
            return root;
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

    /** Picks a start time of the list, as a click on it would. */
    @SuppressWarnings("unchecked")
    private static void pickTime(Parent root, String time) throws Exception {
        ComboBox<LocalTime> box = (ComboBox<LocalTime>) root.lookup("#timeBox");
        FxTest.onFxThread(() -> {
            box.getSelectionModel().select(box.getItems().stream().filter(t -> DateFormats.time(t).equals(time)).findFirst().orElseThrow());
            return null;
        });
    }

    /** The lines of the list of start times, as the user reads them; " |off" marks the ones that cannot be chosen. */
    @SuppressWarnings("unchecked")
    private static List<String> slotLines(Parent root) throws Exception {
        ComboBox<LocalTime> box = (ComboBox<LocalTime>) root.lookup("#timeBox");
        return FxTest.onFxThread(() -> {
            ListView<LocalTime> view = new ListView<>(box.getItems());
            return IntStream.range(0, box.getItems().size()).mapToObj(index -> {
                ListCell<LocalTime> cell = box.getCellFactory().call(view);
                cell.updateListView(view);
                cell.updateIndex(index);
                return cell.getText() + (cell.isDisable() ? " |off" : "");
            }).toList();
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> void choose(Parent root, String id, Predicate<T> which) throws Exception {
        ComboBox<T> box = (ComboBox<T>) root.lookup("#" + id);
        FxTest.onFxThread(() -> {
            box.getSelectionModel().select(box.getItems().stream().filter(which).findFirst().orElseThrow());
            return null;
        });
    }

    private static void chooseDate(Parent root, LocalDate day) throws Exception {
        FxTest.onFxThread(() -> {
            ((DatePicker) root.lookup("#dateField")).setValue(day);
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> items(Parent root, String id) throws Exception {
        ComboBox<T> box = (ComboBox<T>) root.lookup("#" + id);
        return FxTest.onFxThread(() -> List.copyOf(box.getItems()));
    }

    private static String text(Parent root, String id) throws Exception {
        return FxTest.onFxThread(() -> ((Label) root.lookup("#" + id)).getText());
    }

    private static String message(Parent root, String id) throws Exception {
        Label label = (Label) root.lookup("#" + id);
        return FxTest.onFxThread(() -> label.isVisible() ? label.getText() : "");
    }

    /** The animation runs on its own; this waits for the panel to reach the state the test expects. */
    private static boolean panelManaged(Parent root, boolean expected) throws Exception {
        ScrollPane panel = (ScrollPane) root.lookup("#formPanel");
        Supplier<Boolean> reading = panel::isManaged;
        Predicate<Boolean> done = managed -> managed == expected;
        boolean value = FxTest.onFxThread(reading);
        for (int attempt = 0; attempt < 80 && !done.test(value); attempt++) {
            Thread.sleep(25);
            value = FxTest.onFxThread(reading);
        }
        return value;
    }

    /** Fills the whole form for a booking tomorrow at the given hour. */
    private void fillBooking(Parent root, Customer customer, Treatment treatment, Professional professional, String time) throws Exception {
        choose(root, "customerBox", (Customer c) -> c.getDocument().equals(customer.getDocument()));
        choose(root, "treatmentBox", (Treatment t) -> t.getId() == treatment.getId());
        choose(root, "professionalBox", (Professional p) -> p.getDocument().equals(professional.getDocument()));
        chooseDate(root, tomorrow);
        pickTime(root, time);
    }

    private List<Appointment> stored() throws Exception {
        return context.appointments().listByDate(tomorrow).stream().sorted(Comparator.comparing(Appointment::getStart)).toList();
    }

    @SuppressWarnings("unchecked")
    private static List<Appointment> rows(Parent root) throws Exception {
        TableView<Appointment> table = (TableView<Appointment>) root.lookup("#appointmentsTable");
        return FxTest.onFxThread(() -> List.copyOf(table.getItems()));
    }

    @Test
    @DisplayName("Nueva cita opens the panel on the day being shown, with the customers and the active treatments")
    void opensEmpty() throws Exception {
        Parent root = open();
        assertThat(root.lookup("#formPanel").isManaged()).isFalse();

        press(root, "newButton");

        assertThat(panelManaged(root, true)).isTrue();
        assertThat(text(root, "formTitle")).isEqualTo("Agendar cita");
        assertThat(this.<Customer>items(root, "customerBox")).extracting(Customer::getName).containsExactly("Ana García", "Laura Méndez");
        assertThat(this.<Treatment>items(root, "treatmentBox")).extracting(Treatment::getName).containsExactlyInAnyOrder("Manicura gel", "Corte y peinado");
        assertThat(FxTest.onFxThread(() -> ((DatePicker) root.lookup("#dateField")).getValue())).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("the professionals offered are only the ones who offer the chosen treatment")
    void filtersProfessionalsByTreatment() throws Exception {
        Parent root = open();
        press(root, "newButton");
        assertThat(this.<Professional>items(root, "professionalBox")).isEmpty();

        choose(root, "treatmentBox", (Treatment t) -> t.getId() == cut.getId());
        assertThat(this.<Professional>items(root, "professionalBox")).extracting(Professional::getName).containsExactly("Camila Ruiz");

        choose(root, "treatmentBox", (Treatment t) -> t.getId() == manicure.getId());
        assertThat(this.<Professional>items(root, "professionalBox")).extracting(Professional::getName)
            .containsExactlyInAnyOrder("Camila Ruiz", "Daniela Soto");
    }

    @Test
    @DisplayName("the summary shows when it ends, that the professional is free, the deposit and the reminder")
    void summaryShowsWhatTheRulesSet() throws Exception {
        Parent root = open();
        press(root, "newButton");

        fillBooking(root, ana, manicure, camila, "10:00");

        assertThat(text(root, "endValue")).isEqualTo("10:50");
        assertThat(text(root, "availabilityValue")).isEqualTo("Disponible");
        assertThat(text(root, "depositValue")).isEqualTo(CurrencyFormatter.format(new BigDecimal("13500")));
        assertThat(text(root, "reminderValue")).isEqualTo(DateFormats.dateTime(tomorrow.atTime(10, 0).minusHours(24)));
        assertThat(text(root, "summaryNote")).contains("30 %");
    }

    @Test
    @DisplayName("the summary warns when the chosen time clashes with another appointment, and is fine on a free one")
    void summaryWarnsAboutAvailability() throws Exception {
        context.appointments().book(laura, camila, cut, tomorrow.atTime(10, 0), 16500);
        Parent root = open();
        press(root, "newButton");

        fillBooking(root, ana, manicure, camila, "10:30");
        assertThat(text(root, "availabilityValue")).isEqualTo("Ya tiene una cita");

        pickTime(root, "13:00");
        assertThat(text(root, "availabilityValue")).isEqualTo("Disponible");
    }

    @Test
    @DisplayName("the list offers a start time every 15 minutes inside the working day, and marks the ones that clash")
    void listsTheStartTimes() throws Exception {
        context.appointments().book(laura, camila, cut, tomorrow.atTime(10, 0), 16500);
        Parent root = open();
        press(root, "newButton");
        assertThat(slotLines(root)).isEmpty();

        choose(root, "treatmentBox", (Treatment t) -> t.getId() == manicure.getId());
        choose(root, "professionalBox", (Professional p) -> p.getId() == camila.getId());
        chooseDate(root, tomorrow);

        List<String> lines = slotLines(root);
        assertThat(lines).startsWith("08:00", "08:15").endsWith("15:45", "16:00");
        assertThat(lines).doesNotContain("16:15", "07:45");
        assertThat(lines).contains("09:15 · ocupado |off", "10:00 · ocupado |off", "10:45 · ocupado |off")
            .contains("09:00", "11:00");
    }

    @Test
    @DisplayName("the list follows the professional: other working hours and other appointments give another list")
    void listFollowsTheProfessional() throws Exception {
        Parent root = open();
        press(root, "newButton");
        choose(root, "treatmentBox", (Treatment t) -> t.getId() == manicure.getId());
        chooseDate(root, tomorrow);

        choose(root, "professionalBox", (Professional p) -> p.getId() == daniela.getId());

        List<String> lines = slotLines(root);
        assertThat(lines.get(0)).isEqualTo("09:00");
        assertThat(lines.get(lines.size() - 1)).isEqualTo("17:00");
    }

    @Test
    @DisplayName("a time that stops being free is dropped from the choice when the professional changes")
    void choiceIsDroppedWhenItIsNoLongerFree() throws Exception {
        context.appointments().book(laura, daniela, manicure, tomorrow.atTime(9, 0), 13500);
        Parent root = open();
        press(root, "newButton");
        fillBooking(root, ana, manicure, camila, "09:00");
        assertThat(FxTest.onFxThread(() -> ((ComboBox<?>) root.lookup("#timeBox")).getValue())).isNotNull();

        choose(root, "professionalBox", (Professional p) -> p.getId() == daniela.getId());

        assertThat(FxTest.onFxThread(() -> ((ComboBox<?>) root.lookup("#timeBox")).getValue())).isNull();
        assertThat(text(root, "availabilityValue")).isEqualTo("—");
    }
    @Test
    @DisplayName("a customer with many no-shows is asked for the whole price, and the summary says why")
    void penalisedCustomerPaysInFull() throws Exception {
        for (int missed = 0; missed < 3; missed++) {
            context.customers().registerNoShow(laura.getId());
        }
        Parent root = open();
        press(root, "newButton");

        fillBooking(root, laura, manicure, camila, "10:00");

        assertThat(text(root, "depositValue")).isEqualTo(CurrencyFormatter.format(new BigDecimal("45000")));
        assertThat(text(root, "summaryNote")).contains("100 %").contains("3 inasistencias");
    }

    @Test
    @DisplayName("booking saves a pending appointment with the deposit of the rules, closes the panel and shows it")
    void books() throws Exception {
        Parent root = open();
        press(root, "newButton");
        fillBooking(root, ana, manicure, camila, "10:00");

        press(root, "saveButton");

        assertThat(stored()).hasSize(1);
        Appointment saved = stored().get(0);
        assertThat(saved.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(saved.getDeposit()).isEqualByComparingTo("13500");
        assertThat(saved.getReminderAt()).isEqualTo(tomorrow.atTime(10, 0).minusHours(24));
        assertThat(panelManaged(root, false)).isFalse();
        assertThat(rows(root)).hasSize(1);
    }

    @Test
    @DisplayName("each missing choice is reported under its own field and nothing is booked")
    void reportsWhatIsMissing() throws Exception {
        Parent root = open();
        press(root, "newButton");

        press(root, "saveButton");
        assertThat(message(root, "customerMessage")).isNotEmpty();

        choose(root, "customerBox", (Customer c) -> c.getId() == ana.getId());
        press(root, "saveButton");
        assertThat(message(root, "treatmentMessage")).isNotEmpty();

        choose(root, "treatmentBox", (Treatment t) -> t.getId() == manicure.getId());
        press(root, "saveButton");
        assertThat(message(root, "professionalMessage")).isNotEmpty();

        choose(root, "professionalBox", (Professional p) -> p.getId() == camila.getId());
        chooseDate(root, tomorrow);
        press(root, "saveButton");
        assertThat(message(root, "timeMessage")).contains("Elige la hora");
        assertThat(stored()).isEmpty();
    }
    @Test
    @DisplayName("booking over another appointment shows the rejection under the hour and books nothing")
    void rejectsACrossedAppointment() throws Exception {
        context.appointments().book(laura, camila, cut, tomorrow.atTime(10, 0), 16500);
        Parent root = open();
        press(root, "newButton");
        fillBooking(root, ana, manicure, camila, "10:30");

        press(root, "saveButton");

        assertThat(message(root, "timeMessage")).contains("ya tiene una cita");
        assertThat(stored()).hasSize(1);
        assertThat(panelManaged(root, true)).isTrue();
    }

    @Test
    @DisplayName("today the times that come before the minimum notice are marked, and booking one is rejected")
    void rejectsATimeThatIsTooSoon() throws Exception {
        Parent root = open();
        press(root, "newButton");
        choose(root, "customerBox", (Customer c) -> c.getId() == ana.getId());
        choose(root, "treatmentBox", (Treatment t) -> t.getId() == manicure.getId());
        choose(root, "professionalBox", (Professional p) -> p.getId() == camila.getId());
        chooseDate(root, LocalDate.now());
        String tooSoon = slotLines(root).stream().filter(line -> line.contains("muy pronto")).findFirst().orElse(null);
        assumeTrue(tooSoon != null, "every time of today is still ahead of the notice");
        assertThat(tooSoon).endsWith("|off");

        pickTime(root, tooSoon.substring(0, 5));
        assertThat(text(root, "availabilityValue")).isEqualTo("Muy pronto");
        press(root, "saveButton");

        assertThat(message(root, "timeMessage")).isNotEmpty();
        assertThat(context.appointments().listByDate(LocalDate.now())).isEmpty();
    }
    @Test
    @DisplayName("Cancelar closes the panel without booking")
    void cancelCloses() throws Exception {
        Parent root = open();
        press(root, "newButton");
        fillBooking(root, ana, manicure, camila, "10:00");

        press(root, "cancelButton");

        assertThat(panelManaged(root, false)).isFalse();
        assertThat(stored()).isEmpty();
    }

    @Test
    @DisplayName("Reprogramar opens the panel on the chosen appointment, with who and what fixed, and moves it")
    void reschedules() throws Exception {
        Appointment booked = context.appointments().book(ana, camila, cut, tomorrow.atTime(10, 0), 16500);
        Parent root = open();
        press(root, "nextButton");
        FxTest.onFxThread(() -> {
            ((TableView<?>) root.lookup("#appointmentsTable")).getSelectionModel().select(0);
            return null;
        });

        press(root, "rescheduleButton");

        assertThat(panelManaged(root, true)).isTrue();
        assertThat(text(root, "formTitle")).isEqualTo("Reprogramar cita");
        assertThat(root.lookup("#customerBox").isDisabled()).isTrue();
        assertThat(root.lookup("#professionalBox").isDisabled()).isTrue();
        assertThat(FxTest.onFxThread(() -> ((ComboBox<?>) root.lookup("#timeBox")).getValue())).isEqualTo(LocalTime.of(10, 0));
        assertThat(slotLines(root)).doesNotContain("10:00 · ocupado |off");

        pickTime(root, "11:30");
        press(root, "saveButton");

        Appointment moved = context.appointments().listByDate(tomorrow).get(0);
        assertThat(moved.getId()).isEqualTo(booked.getId());
        assertThat(moved.getStart()).isEqualTo(tomorrow.atTime(11, 30));
        assertThat(moved.getDeposit()).isEqualByComparingTo("16500");
        assertThat(panelManaged(root, false)).isFalse();
    }

    @Test
    @DisplayName("moving an appointment onto another one is rejected and leaves it where it was")
    void rejectsAClashingMove() throws Exception {
        Appointment first = context.appointments().book(ana, camila, cut, tomorrow.atTime(10, 0), 16500);
        context.appointments().book(laura, camila, manicure, tomorrow.atTime(14, 0), 13500);
        Parent root = open();
        press(root, "nextButton");
        FxTest.onFxThread(() -> {
            ((TableView<?>) root.lookup("#appointmentsTable")).getSelectionModel().select(0);
            return null;
        });
        press(root, "rescheduleButton");

        pickTime(root, "14:15");
        press(root, "saveButton");

        assertThat(message(root, "timeMessage")).contains("ya tiene una cita");
        assertThat(context.appointments().listByDate(tomorrow).stream().filter(a -> a.getId() == first.getId()).findFirst().orElseThrow()
            .getStart()).isEqualTo(tomorrow.atTime(10, 0));
    }

    @Test
    @DisplayName("Reprogramar is off for an appointment that no longer holds the schedule")
    void rescheduleIsOffForACancelledAppointment() throws Exception {
        Appointment booked = context.appointments().book(ana, camila, cut, tomorrow.atTime(10, 0), 16500);
        context.appointments().cancel(booked.getId());
        Parent root = open();
        press(root, "nextButton");

        FxTest.onFxThread(() -> {
            ((TableView<?>) root.lookup("#appointmentsTable")).getSelectionModel().select(0);
            return null;
        });

        assertThat(FxTest.onFxThread(() -> root.lookup("#rescheduleButton").isDisabled())).isTrue();
    }
}
