package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Product;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.ProductData;
import com.maisonglow.service.TreatmentData;
import com.maisonglow.service.WorkSchedule;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLifecycle;
import com.maisonglow.view.ViewLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Customer area: home and profile, over the real services")
class CustomerAreaViewTest {

    private static final char[] PASSWORD = "clave-segura-123".toCharArray();

    private static boolean toolkit;

    @TempDir
    Path data;

    private final Clock clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
    private AppContext context;
    private Appointment visit;
    private Product serum;
    private final List<String> dialogs = new ArrayList<>();

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
        serum = context.products().register(new ProductData("Sérum Vitamina C", 1, 48000, 2), 10);
        context.catalog().setPublished(serum.getId(), true);

        context.auth().login("ana", PASSWORD);
        LocalDateTime slot = LocalDateTime.now(clock).plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        visit = context.portal().book(camila.getId(), cut.getId(), slot);
        context.orders().place(UUID.randomUUID().toString(), Map.of(serum.getId(), 2));
        context.auth().login("laura", PASSWORD);
        context.orders().place(UUID.randomUUID().toString(), Map.of(serum.getId(), 1));
        context.auth().login("ana", PASSWORD);
    }

    private <C> ViewLoader.Loaded<C> open(String fxml, Executor executor, java.util.function.Consumer<C> setUp) throws Exception {
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<C> loaded = ViewLoader.loadWithController(
                CustomerAreaViewTest.class.getResource("/com/maisonglow/view/" + fxml), context);
            setUp.accept(loaded.controller());
            new Scene(loaded.root(), 1100, 700);
            loaded.root().applyCss();
            loaded.root().layout();
            return loaded;
        });
    }

    private ViewLoader.Loaded<CustomerHomeController> openHome() throws Exception {
        return open("CustomerHomeView.fxml", Runnable::run, controller -> controller.useExecutor(Runnable::run));
    }

    private ViewLoader.Loaded<ProfileController> openProfile(Runnable passwordOpener) throws Exception {
        return open("ProfileView.fxml", Runnable::run, controller -> {
            controller.useExecutor(Runnable::run);
            controller.usePasswordOpener(passwordOpener);
            controller.useAlerts(new AlertPresenter(() -> null, alert -> {
                dialogs.add(alert.getContentText());
                return Optional.empty();
            }));
        });
    }

    private static void settle() throws Exception {
        for (int round = 0; round < 3; round++) {
            FxTest.onFxThread(() -> null);
            Thread.sleep(60);
        }
    }

    private static String text(Parent root, String id) throws Exception {
        settle();
        return FxTest.onFxThread(() -> ((Label) root.lookup("#" + id)).getText());
    }

    private static boolean shown(Parent root, String id) throws Exception {
        settle();
        return FxTest.onFxThread(() -> root.lookup("#" + id).isVisible());
    }

    private static List<String> labels(Parent root, String id) throws Exception {
        settle();
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

    private static void type(Parent root, String id, String value) throws Exception {
        FxTest.onFxThread(() -> {
            ((TextField) root.lookup("#" + id)).setText(value);
            return null;
        });
    }

    private static void press(Parent root, String id) throws Exception {
        FxTest.onFxThread(() -> {
            ((Button) root.lookup("#" + id)).fire();
            return null;
        });
        settle();
    }

    // ---- Inicio ----

    @Test
    @DisplayName("the home greets the customer by their first name and dates the day")
    void homeGreets() throws Exception {
        Parent root = openHome().root();

        assertThat(text(root, "greetingLabel")).matches("(Buenos días|Buenas tardes|Buenas noches), Ana");
        assertThat(text(root, "dateLabel")).matches("^\\p{Lu}.*");
    }

    @Test
    @DisplayName("the indicators count what is theirs: appointments, purchases and orders still to pay")
    void homeIndicators() throws Exception {
        Parent root = openHome().root();

        assertThat(text(root, "appointmentsValue")).isEqualTo("1");
        assertThat(text(root, "purchasesValue")).isEqualTo("0");
        assertThat(text(root, "pendingOrdersValue")).isEqualTo("1");
    }

    @Test
    @DisplayName("the next appointment shows its treatment, professional, time and state; the header says when it is")
    void homeNextAppointment() throws Exception {
        Parent root = openHome().root();

        assertThat(shown(root, "nextEmpty")).isFalse();
        assertThat(labels(root, "nextBox")).anyMatch(text -> text.equals("Corte y peinado"))
            .anyMatch(text -> text.contains("Camila Ruiz") && text.contains("10:00"));
        assertThat(text(root, "nextState")).isEqualTo("Pendiente");
        assertThat(text(root, "summaryLabel")).startsWith("Tu próxima cita es el").contains("10:00").contains("1 pedido por pagar");
    }

    @Test
    @DisplayName("the recent orders are only the customer's own, with their state and where to pay")
    void homeOrders() throws Exception {
        Parent root = openHome().root();

        List<String> rows = labels(root, "ordersBox");
        assertThat(text(root, "ordersCount")).isEqualTo("1 pedido");
        assertThat(rows).anyMatch(row -> row.startsWith("Pedido n.º") && row.contains("96.000"));
        assertThat(rows).anyMatch(row -> row.startsWith("Págalo en el local antes de las"));
        assertThat(rows).contains("Pendiente de pago");
        assertThat(rows).noneMatch(row -> row.contains("48.000"));
        assertThat(shown(root, "ordersEmpty")).isFalse();
    }

    @Test
    @DisplayName("a customer with nothing yet sees empty cards that say where to start, and no other customer's data")
    void homeEmptyStates() throws Exception {
        context.auth().login("admin", PASSWORD);
        context.auth().registerCustomer(new ContactData("1003", "Marta Díaz", "3003334455", "marta@correo.com"), "marta", PASSWORD);
        context.auth().login("marta", PASSWORD);
        Parent root = openHome().root();

        assertThat(text(root, "greetingLabel")).endsWith(", Marta");
        assertThat(shown(root, "nextEmpty")).isTrue();
        assertThat(text(root, "nextEmpty")).contains("Mis citas");
        assertThat(shown(root, "ordersEmpty")).isTrue();
        assertThat(text(root, "ordersEmpty")).contains("Tienda");
        assertThat(text(root, "appointmentsValue")).isEqualTo("0");
        assertThat(text(root, "pendingOrdersValue")).isEqualTo("0");
        assertThat(text(root, "summaryLabel")).isEqualTo("No tienes citas próximas.");
    }

    @Test
    @DisplayName("the sentence of the header says what comes next")
    void summarySentence() {
        assertThat(CustomerHomeController.summary(Optional.empty(), 0)).isEqualTo("No tienes citas próximas.");
        assertThat(CustomerHomeController.summary(Optional.empty(), 2)).isEqualTo("No tienes citas próximas. Tienes 2 pedidos por pagar en el local.");
        assertThat(CustomerHomeController.summary(Optional.of(visit), 0)).startsWith("Tu próxima cita es el").endsWith("a las 10:00.");
    }

    /** Loads a screen and discards it in the same step, so the answer of the background arrives after the session closed. */
    private <C extends ViewLifecycle> ViewLoader.Loaded<C> openAndDiscard(String fxml) throws Exception {
        ViewLoader.Loaded<C> loaded = FxTest.onFxThread(() -> {
            ViewLoader.Loaded<C> opened = ViewLoader.loadWithController(
                CustomerAreaViewTest.class.getResource("/com/maisonglow/view/" + fxml), context);
            new Scene(opened.root(), 1100, 700);
            opened.controller().dispose();
            return opened;
        });
        Thread.sleep(300);
        settle();
        return loaded;
    }

    @Test
    @DisplayName("an answer that arrives after the session closed is ignored and nothing is shown")
    void homeLateAnswerIsIgnored() throws Exception {
        var loaded = this.<CustomerHomeController>openAndDiscard("CustomerHomeView.fxml");

        assertThat(text(loaded.root(), "appointmentsValue")).isEqualTo("3");
        assertThat(labels(loaded.root(), "ordersBox")).isEmpty();
        assertThat(labels(loaded.root(), "nextBox")).isEmpty();
    }

    // ---- Mi perfil ----

    @Test
    @DisplayName("the profile shows the customer's data, with the document shown but not editable")
    void profileShowsTheData() throws Exception {
        var profile = openProfile(() -> { });
        Parent root = profile.root();
        settle();

        assertThat(text(root, "nameTitle")).isEqualTo("Ana García");
        assertThat(text(root, "initialsLabel")).isEqualTo("AG");
        assertThat(text(root, "documentLabel")).isEqualTo("Documento 1001");
        assertThat(FxTest.onFxThread(() -> ((TextField) root.lookup("#nameField")).getText())).isEqualTo("Ana García");
        assertThat(FxTest.onFxThread(() -> ((TextField) root.lookup("#phoneField")).getText())).isEqualTo("3001112233");
        assertThat(FxTest.onFxThread(() -> ((TextField) root.lookup("#emailField")).getText())).isEqualTo("ana@correo.com");
        assertThat(FxTest.onFxThread(() -> root.lookup("#documentField"))).isNull();
    }

    @Test
    @DisplayName("changing the name, phone and email saves them for this customer and keeps the document")
    void profileSavesTheContact() throws Exception {
        Parent root = openProfile(() -> { }).root();
        settle();
        type(root, "nameField", "Ana María García");
        type(root, "phoneField", "3109998877");
        type(root, "emailField", "ana.maria@correo.com");

        press(root, "saveButton");

        Customer saved = context.portal().profile();
        assertThat(saved.getName()).isEqualTo("Ana María García");
        assertThat(saved.getPhone()).isEqualTo("3109998877");
        assertThat(saved.getEmail()).isEqualTo("ana.maria@correo.com");
        assertThat(saved.getDocument()).isEqualTo("1001");
        assertThat(text(root, "nameTitle")).isEqualTo("Ana María García");
    }

    @Test
    @DisplayName("an empty name, a bad phone and a bad email are each refused under their own field and nothing changes")
    void profileRefusesWhatIsWrong() throws Exception {
        Parent root = openProfile(() -> { }).root();
        settle();
        type(root, "nameField", " ");
        press(root, "saveButton");
        assertThat(text(root, "nameMessage")).containsIgnoringCase("nombre");

        type(root, "nameField", "Ana García");
        type(root, "phoneField", "12");
        press(root, "saveButton");
        assertThat(text(root, "phoneMessage")).containsIgnoringCase("teléfono");

        type(root, "phoneField", "3001112233");
        type(root, "emailField", "sin-arroba");
        press(root, "saveButton");
        assertThat(text(root, "emailMessage")).containsIgnoringCase("correo");

        assertThat(context.portal().profile().getPhone()).isEqualTo("3001112233");
        assertThat(context.portal().profile().getEmail()).isEqualTo("ana@correo.com");
        assertThat(FxTest.onFxThread(() -> root.lookup("#saveButton").isDisabled())).isFalse();
    }

    @Test
    @DisplayName("Descartar puts back what was saved")
    void profileDiscards() throws Exception {
        Parent root = openProfile(() -> { }).root();
        settle();
        type(root, "nameField", "Otro nombre");

        press(root, "discardButton");

        assertThat(FxTest.onFxThread(() -> ((TextField) root.lookup("#nameField")).getText())).isEqualTo("Ana García");
    }

    @Test
    @DisplayName("the password button opens the change-password window and the profile never holds a password")
    void profilePasswordButton() throws Exception {
        int[] opened = {0};
        Parent root = openProfile(() -> opened[0]++).root();
        settle();

        press(root, "passwordButton");

        assertThat(opened[0]).isEqualTo(1);
        assertThat(FxTest.onFxThread(() -> root.lookupAll(".password-field").size())).isZero();
    }

    @Test
    @DisplayName("an answer that arrives after the session closed is ignored and the fields stay empty")
    void profileLateAnswerIsIgnored() throws Exception {
        var loaded = this.<ProfileController>openAndDiscard("ProfileView.fxml");

        assertThat(FxTest.onFxThread(() -> ((TextField) loaded.root().lookup("#nameField")).getText())).isEmpty();
        assertThat(loaded.controller().shown()).isNull();
    }
}
