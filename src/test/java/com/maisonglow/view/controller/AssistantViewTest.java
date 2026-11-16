package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.ProductData;
import com.maisonglow.service.TreatmentData;
import com.maisonglow.service.WorkSchedule;
import com.maisonglow.view.FxTest;
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

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Assistant view tests")
class AssistantViewTest {

    private static boolean toolkit;

    @TempDir
    Path data;

    private AppContext context;
    private Customer ana;
    private Professional camila;
    private Treatment cut;
    private LocalDate tomorrow;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @BeforeEach
    void prepareData() throws Exception {
        AppContext.seed(data);
        context = new AppContext(data, Clock.systemDefaultZone(), AiConfig.simulated());
        tomorrow = LocalDate.now().plusDays(1);
        ana = context.customers().register(new ContactData("1001", "Ana García", "3001112233", "ana@correo.com"));
        cut = context.treatments().register(new TreatmentData("Corte y peinado", "", 55000, 60));
        camila = context.professionals().register(new ContactData("2001", "Camila Ruiz", "3001112233", "c@correo.com"),
            "Estilista", new WorkSchedule(LocalTime.of(8, 0), LocalTime.of(17, 0)));
        context.professionals().assignTreatment(camila.getId(), cut.getId());
    }

    private Parent open() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        context = new AppContext(data, Clock.systemDefaultZone(), AiConfig.simulated());
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<AssistantController> loaded = ViewLoader.loadWithController(
                AssistantViewTest.class.getResource("/com/maisonglow/view/AssistantView.fxml"), context);
            loaded.controller().useExecutor(Runnable::run);
            Parent root = loaded.root();
            new Scene(root, 1200, 760);
            root.applyCss();
            root.layout();
            return root;
        });
    }

    /** What the chat says, in order: the words of every bubble, the user's and the assistant's. */
    private static List<String> bubbles(Parent root) throws Exception {
        return FxTest.onFxThread(() -> {
            List<String> texts = new ArrayList<>();
            collect(root.lookup("#messagesBox"), texts);
            return texts;
        });
    }

    private static void collect(Node node, List<String> texts) {
        if (node instanceof Label label && (label.getStyleClass().contains("bubble-user") || label.getStyleClass().contains("bubble-assistant"))) {
            texts.add(label.getText());
        }
        if (node instanceof Pane pane) {
            pane.getChildren().forEach(child -> collect(child, texts));
        }
    }

    /** The animation and the answer come back on their own; this waits until what the test expects is on screen. */
    private static List<String> until(Parent root, java.util.function.Predicate<List<String>> done) throws Exception {
        List<String> now = bubbles(root);
        for (int attempt = 0; attempt < 80 && !done.test(now); attempt++) {
            Thread.sleep(25);
            now = bubbles(root);
        }
        return now;
    }

    private static void send(Parent root, String text) throws Exception {
        FxTest.onFxThread(() -> {
            ((TextField) root.lookup("#messageField")).setText(text);
            ((Button) root.lookup("#sendButton")).fire();
            return null;
        });
    }

    private static void press(Parent root, String id) throws Exception {
        FxTest.onFxThread(() -> {
            ((Button) root.lookup("#" + id)).fire();
            return null;
        });
    }

    /** The buttons of the confirmation card, found by their text. */
    private static Button button(Parent root, String text) throws Exception {
        return FxTest.onFxThread(() -> {
            List<Button> found = new ArrayList<>();
            root.lookupAll(".button-compact").stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> text.equals(button.getText())).forEach(found::add);
            return found.isEmpty() ? null : found.get(found.size() - 1);
        });
    }

    private String bookingMessage(String hour) {
        return "book_appointment customerId=" + ana.getId() + "; professionalId=" + camila.getId() + "; treatmentId=" + cut.getId()
            + "; start=" + tomorrow + "T" + hour + "; deposit=16500";
    }

    @Test
    @DisplayName("opens with a welcome from the assistant, and the banner that says it is simulated")
    void opensWithWelcome() throws Exception {
        Parent root = open();

        assertThat(bubbles(root)).singleElement().asString().contains("asistente de Maison Glow");
        assertThat(FxTest.onFxThread(() -> root.lookup("#simulatedBanner").isVisible())).isTrue();
    }

    @Test
    @DisplayName("what the user writes goes to the assistant and its answer comes back in the chat")
    void sendsAndShowsTheAnswer() throws Exception {
        Parent root = open();

        send(root, "ayuda");
        List<String> chat = until(root, texts -> texts.size() >= 3);

        assertThat(chat.get(1)).isEqualTo("ayuda");
        assertThat(chat.get(2)).startsWith("Asistente simulado");
        assertThat(FxTest.onFxThread(() -> ((TextField) root.lookup("#messageField")).getText())).isEmpty();
        assertThat(FxTest.onFxThread(() -> root.lookup("#sendButton").isDisabled())).isFalse();
    }

    @Test
    @DisplayName("an empty message is not sent")
    void emptyMessageIsIgnored() throws Exception {
        Parent root = open();

        send(root, "   ");

        assertThat(bubbles(root)).hasSize(1);
    }

    @Test
    @DisplayName("the shortcut of low stock asks for the products to restock")
    void lowStockShortcut() throws Exception {
        context.products().register(new ProductData("Mascarilla Reparadora", 1, 35000, 5), 1);
        Parent root = open();

        press(root, "lowStockShortcut");
        List<String> chat = until(root, texts -> texts.size() >= 3);

        assertThat(chat.get(1)).isEqualTo("stock bajo");
        assertThat(chat.get(2)).contains("Mascarilla Reparadora");
    }

    @Test
    @DisplayName("the customer shortcut prepares the question for the user to finish, and finishing it finds the customer")
    void customerShortcut() throws Exception {
        Parent root = open();

        press(root, "customerShortcut");
        assertThat(FxTest.onFxThread(() -> ((TextField) root.lookup("#messageField")).getText())).isEqualTo("buscar cliente ");
        assertThat(bubbles(root)).hasSize(1);

        send(root, "buscar cliente ana");
        assertThat(until(root, texts -> texts.size() >= 3).get(2)).contains("Ana García");
    }

    @Test
    @DisplayName("an error of the assistant is shown as an answer, not as a crash")
    void errorIsAnAnswer() throws Exception {
        Parent root = open();

        send(root, "inventada x=1");

        assertThat(until(root, texts -> texts.size() >= 3).get(2)).contains("Herramienta no registrada");
    }

    @Test
    @DisplayName("booking an appointment asks to confirm, shows the names and does not book until Confirmar is pressed")
    void bookingWaitsForConfirmation() throws Exception {
        Parent root = open();

        send(root, bookingMessage("10:00"));
        until(root, texts -> texts.size() >= 3);

        assertThat(button(root, "Confirmar")).isNotNull();
        assertThat(button(root, "Cancelar")).isNotNull();
        List<String> shown = FxTest.onFxThread(() -> root.lookupAll(".stat-value").stream().map(node -> ((Label) node).getText()).toList());
        assertThat(shown).contains("Ana García", "Camila Ruiz", "Corte y peinado");
        assertThat(FxTest.onFxThread(() -> root.lookupAll(".confirm-title").stream().map(node -> ((Label) node).getText()).toList()))
            .containsExactly("Confirmar acción: Agendar cita");
        assertThat(context.appointments().listByDate(tomorrow)).isEmpty();
    }

    @Test
    @DisplayName("Cancelar discards the action: nothing is booked and the buttons go off")
    void cancelDiscards() throws Exception {
        Parent root = open();
        send(root, bookingMessage("10:00"));
        until(root, texts -> texts.size() >= 3);

        Button cancel = button(root, "Cancelar");
        FxTest.onFxThread(() -> {
            cancel.fire();
            return null;
        });
        List<String> chat = until(root, texts -> texts.size() >= 4);

        assertThat(chat.get(3)).contains("cancelada");
        assertThat(context.appointments().listByDate(tomorrow)).isEmpty();
        assertThat(button(root, "Confirmar").isDisabled()).isTrue();
        assertThat(button(root, "Cancelar").isDisabled()).isTrue();
    }

    @Test
    @DisplayName("Confirmar books the appointment, and the same card cannot be used twice")
    void confirmBooks() throws Exception {
        Parent root = open();
        send(root, bookingMessage("10:00"));
        until(root, texts -> texts.size() >= 3);
        Button confirm = button(root, "Confirmar");

        FxTest.onFxThread(() -> {
            confirm.fire();
            return null;
        });
        List<String> chat = until(root, texts -> texts.size() >= 4);

        assertThat(chat.get(3)).contains("Cita agendada");
        assertThat(context.appointments().listByDate(tomorrow)).hasSize(1);
        assertThat(confirm.isDisabled()).isTrue();
    }
}
