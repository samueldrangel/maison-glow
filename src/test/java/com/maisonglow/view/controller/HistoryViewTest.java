package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.Commerce.ChangeKind;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.TreatmentData;
import com.maisonglow.service.WorkSchedule;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("History view tests, over the real history service")
class HistoryViewTest {

    private static final char[] PASSWORD = "role-backend-fixture".toCharArray();

    private static boolean toolkit;

    @TempDir
    Path data;

    @TempDir
    Path files;

    private final Now clock = new Now();
    private AppContext context;
    private Customer ana;
    private Customer laura;
    private Appointment visit;
    private Path photo;
    private final List<String> dialogs = new ArrayList<>();
    private final List<Runnable> held = new ArrayList<>();

    /** A clock that the test moves: the history only accepts a service once the appointment has taken place. */
    private static final class Now extends Clock {
        Instant now = Instant.now();

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @BeforeEach
    void prepare() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        context = new AppContext(data, clock, AiConfig.simulated());
        context.auth().createFirstAdmin("admin", PASSWORD);
        context.auth().login("admin", PASSWORD);
        context.auth().registerCustomer(new ContactData("1001", "Ana García", "3001112233", "ana@correo.com"), "ana", PASSWORD);
        context.auth().registerCustomer(new ContactData("1002", "Laura Pérez", "3002223344", "laura@correo.com"), "laura", PASSWORD);
        Treatment cut = context.treatments().register(new TreatmentData("Corte y peinado", "", 55000, 60));
        Professional camila = context.professionals().register(new ContactData("2001", "Camila Ruiz", "3001112233", "c@correo.com"),
            "Estilista", new WorkSchedule(LocalTime.of(8, 0), LocalTime.of(18, 0)));
        context.professionals().assignTreatment(camila.getId(), cut.getId());
        ana = context.customers().searchByName("Ana").getFirst();
        laura = context.customers().searchByName("Laura").getFirst();
        photo = image("foto.png");

        context.auth().login("ana", PASSWORD);
        LocalDateTime slot = LocalDateTime.now(clock).plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        visit = context.portal().book(camila.getId(), cut.getId(), slot);
        context.changes().allowStaffAccess(true);
        context.changes().addOwn(ChangeKind.PERSONAL, "Corte nuevo en casa", LocalDate.now(clock), photo, null);
        clock.now = visit.getEnd().toInstant(ZoneOffset.UTC);
        context.auth().login("admin", PASSWORD);
        context.appointments().complete(visit.getId());
    }

    private Path image(String name) throws Exception {
        BufferedImage image = new BufferedImage(120, 90, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 120; x++) {
            for (int y = 0; y < 90; y++) {
                image.setRGB(x, y, 0xc99269);
            }
        }
        Path file = files.resolve(name);
        ImageIO.write(image, "png", file.toFile());
        return file;
    }

    private ViewLoader.Loaded<HistoryController> open(boolean confirm, java.util.concurrent.Executor executor) throws Exception {
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<HistoryController> loaded = ViewLoader.loadWithController(
                HistoryViewTest.class.getResource("/com/maisonglow/view/HistoryView.fxml"), context);
            loaded.controller().useExecutor(executor);
            loaded.controller().useAlerts(new AlertPresenter(() -> null, alert -> {
                dialogs.add(alert.getContentText());
                return alert.getButtonTypes().stream().filter(button -> confirm && "Confirmar".equals(button.getText())).findFirst();
            }));
            new Scene(loaded.root(), 1100, 700);
            loaded.root().applyCss();
            loaded.root().layout();
            return loaded;
        });
    }

    private ViewLoader.Loaded<HistoryController> open() throws Exception {
        return open(true, Runnable::run);
    }

    private static void settle() throws Exception {
        for (int round = 0; round < 3; round++) {
            FxTest.onFxThread(() -> null);
            Thread.sleep(60);
        }
    }

    @SuppressWarnings("unchecked")
    private static void choose(Parent root, Customer customer) throws Exception {
        FxTest.onFxThread(() -> {
            ComboBox<Customer> box = (ComboBox<Customer>) root.lookup("#customerBox");
            box.setValue(box.getItems().stream().filter(item -> item.getId() == customer.getId()).findFirst().orElseThrow());
            return null;
        });
        settle();
    }

    private static List<String> texts(Parent root, String id) throws Exception {
        return FxTest.onFxThread(() -> {
            List<String> found = new ArrayList<>();
            collect(root.lookup("#" + id), found);
            return found;
        });
    }

    private static void collect(Node node, List<String> found) {
        if (node instanceof Label label && label.getText() != null) {
            found.add(label.getText());
        }
        if (node instanceof Pane pane) {
            pane.getChildren().forEach(child -> collect(child, found));
        }
    }

    private static boolean shown(Parent root, String id) throws Exception {
        return FxTest.onFxThread(() -> root.lookup("#" + id).isVisible());
    }

    private static boolean disabled(Parent root, String id) throws Exception {
        return FxTest.onFxThread(() -> root.lookup("#" + id).isDisabled());
    }

    private static long photos(Parent root) throws Exception {
        return FxTest.onFxThread(() -> root.lookup("#timelineBox").lookupAll(".image-view").stream()
            .filter(node -> node instanceof ImageView view && view.getImage() != null).count());
    }

    private static void press(Parent root, String id) throws Exception {
        FxTest.onFxThread(() -> {
            ((Button) root.lookup("#" + id)).fire();
            return null;
        });
        settle();
    }

    @SuppressWarnings("unchecked")
    private static void pickAppointment(Parent root) throws Exception {
        FxTest.onFxThread(() -> {
            ComboBox<Appointment> box = (ComboBox<Appointment>) root.lookup("#appointmentBox");
            box.setValue(box.getItems().getFirst());
            return null;
        });
    }

    @Test
    @DisplayName("opens asking to choose a customer, with every customer to choose from and the registration off")
    void opensIdle() throws Exception {
        Parent root = open().root();

        assertThat(texts(root, "timelineBox")).containsExactly("Elige un cliente para ver su historial.");
        assertThat(FxTest.onFxThread(() -> ((ComboBox<?>) root.lookup("#customerBox")).getItems().size())).isEqualTo(2);
        assertThat(shown(root, "authorizationBanner")).isFalse();
        assertThat(disabled(root, "registerCard")).isTrue();
    }

    @Test
    @DisplayName("a customer who did not authorize the business shows no history and explains why")
    void unauthorizedCustomer() throws Exception {
        Parent root = open().root();

        choose(root, laura);

        assertThat(shown(root, "authorizationBanner")).isTrue();
        assertThat(texts(root, "authorizationText")).singleElement().asString().contains("no ha autorizado");
        assertThat(texts(root, "timelineBox")).containsExactly("No se puede mostrar el historial de este cliente.");
        assertThat(disabled(root, "registerCard")).isTrue();
    }

    @Test
    @DisplayName("an authorized customer shows the timeline with notes, kind and photos, and the completed appointments to register")
    void authorizedCustomer() throws Exception {
        Parent root = open().root();

        choose(root, ana);

        assertThat(shown(root, "authorizationBanner")).isFalse();
        assertThat(texts(root, "timelineBox")).contains("Cambio personal", "Corte nuevo en casa", "Antes");
        assertThat(photos(root)).isEqualTo(1);
        assertThat(disabled(root, "registerCard")).isFalse();
        assertThat(FxTest.onFxThread(() -> ((ComboBox<?>) root.lookup("#appointmentBox")).getItems().size())).isEqualTo(1);
    }

    @Test
    @DisplayName("registering needs the appointment and the notes, each asked for under its field, and calls nothing without them")
    void registerNeedsAppointmentAndNotes() throws Exception {
        Parent root = open().root();
        choose(root, ana);

        press(root, "saveButton");
        assertThat(texts(root, "appointmentMessage")).singleElement().asString().contains("Elige la cita");
        pickAppointment(root);
        press(root, "saveButton");

        assertThat(texts(root, "notesMessage")).singleElement().asString().contains("notas");
        assertThat(context.changes().forCustomer(ana.getId())).hasSize(1);
    }

    @Test
    @DisplayName("a valid record is saved as a service with its before photo, shows in the timeline and clears the form")
    void registersTheAttention() throws Exception {
        var screen = open();
        Parent root = screen.root();
        choose(root, ana);
        pickAppointment(root);
        FxTest.onFxThread(() -> { ((TextArea) root.lookup("#notesArea")).setText("Color cobrizo aplicado"); return null; });
        screen.controller().beforePicker().useFilePicker(() -> photo);
        FxTest.onFxThread(() -> { ((Button) root.lookup("#beforeBox").lookup("#uploadButton")).fire(); return null; });
        FxTest.onFxThread(() -> { ((Button) root.lookup("#beforeBox").lookup("#useButton")).fire(); return null; });

        press(root, "saveButton");

        var records = context.changes().forCustomer(ana.getId());
        assertThat(records).hasSize(2);
        assertThat(records).anyMatch(record -> record.kind() == ChangeKind.SERVICE && record.notes().equals("Color cobrizo aplicado")
            && record.sourceKey() != null && record.appointmentId() != null && record.appointmentId() == visit.getId());
        assertThat(texts(root, "timelineBox")).contains("Servicio del negocio", "Color cobrizo aplicado");
        assertThat(FxTest.onFxThread(() -> ((TextArea) root.lookup("#notesArea")).getText())).isEmpty();
        assertThat(photos(root)).isEqualTo(2);
    }

    @Test
    @DisplayName("removing the photos asks first, takes them away and keeps the record with its notes")
    void removesThePhotos() throws Exception {
        Parent root = open(true, Runnable::run).root();
        choose(root, ana);
        assertThat(photos(root)).isEqualTo(1);

        FxTest.onFxThread(() -> {
            root.lookupAll(".button").stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> "Retirar fotos".equals(button.getText())).findFirst().orElseThrow().fire();
            return null;
        });
        settle();

        assertThat(dialogs).anyMatch(text -> text.contains("notas y el registro se conservan"));
        assertThat(photos(root)).isZero();
        assertThat(texts(root, "timelineBox")).contains("Corte nuevo en casa");
        assertThat(context.changes().forCustomer(ana.getId())).hasSize(1);
    }

    @Test
    @DisplayName("an answer that arrives after the screen was discarded is ignored and no history stays on screen")
    void lateAnswerIsIgnored() throws Exception {
        var screen = open(true, held::add);
        Parent root = screen.root();
        FxTest.onFxThread(() -> {
            @SuppressWarnings("unchecked")
            ComboBox<Customer> box = (ComboBox<Customer>) root.lookup("#customerBox");
            box.setValue(box.getItems().stream().filter(item -> item.getId() == ana.getId()).findFirst().orElseThrow());
            return null;
        });

        FxTest.onFxThread(() -> { screen.controller().dispose(); return null; });
        held.forEach(Runnable::run);
        settle();

        assertThat(texts(root, "timelineBox")).isEmpty();
    }
}
