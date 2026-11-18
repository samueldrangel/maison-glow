package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.TreatmentData;
import com.maisonglow.service.WorkSchedule;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.Navigator;
import com.maisonglow.view.View;
import com.maisonglow.view.ViewLoader;
import com.maisonglow.view.access.AccessRole;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
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

@DisplayName("Closing the session discards what is pending")
class SessionLifecycleTest {

    private static boolean toolkit;

    @TempDir
    Path data;

    private AppContext context;
    private Customer ana;
    private Professional camila;
    private Treatment cut;
    private LocalDate tomorrow;
    /** Work asked to the background, held back until the test lets it run: this is a slow answer. */
    private final List<Runnable> slow = new ArrayList<>();

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @BeforeEach
    void prepare() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        AppContext.seed(data);
        context = new AppContext(data, Clock.systemDefaultZone(), AiConfig.simulated());
        tomorrow = LocalDate.now().plusDays(1);
        ana = context.customers().register(new ContactData("1001", "Ana García", "3001112233", "ana@correo.com"));
        cut = context.treatments().register(new TreatmentData("Corte y peinado", "", 55000, 60));
        camila = context.professionals().register(new ContactData("2001", "Camila Ruiz", "3001112233", "c@correo.com"),
            "Estilista", new WorkSchedule(LocalTime.of(8, 0), LocalTime.of(17, 0)));
        context.professionals().assignTreatment(camila.getId(), cut.getId());
    }

    private ViewLoader.Loaded<AssistantController> openAssistant(java.util.concurrent.Executor executor) throws Exception {
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<AssistantController> loaded = ViewLoader.loadWithController(
                SessionLifecycleTest.class.getResource("/com/maisonglow/view/AssistantView.fxml"), context);
            loaded.controller().useExecutor(executor);
            new Scene(loaded.root(), 1200, 760);
            loaded.root().applyCss();
            loaded.root().layout();
            return loaded;
        });
    }

    private static void send(Parent root, String text) throws Exception {
        FxTest.onFxThread(() -> {
            ((TextField) root.lookup("#messageField")).setText(text);
            ((Button) root.lookup("#sendButton")).fire();
            return null;
        });
    }

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

    private static List<Button> confirmationButtons(Parent root) throws Exception {
        return FxTest.onFxThread(() -> root.lookupAll(".button-compact").stream().filter(Button.class::isInstance)
            .map(Button.class::cast).toList());
    }

    private static void runLater() throws Exception {
        FxTest.onFxThread(() -> null);
        Thread.sleep(150);
        FxTest.onFxThread(() -> null);
    }

    @Test
    @DisplayName("an answer that arrives after the session closed does not show up in the chat")
    void lateAnswerIsIgnored() throws Exception {
        var loaded = openAssistant(slow::add);
        send(loaded.root(), "ayuda");
        int before = bubbles(loaded.root()).size();

        FxTest.onFxThread(() -> { loaded.controller().dispose(); return null; });
        slow.forEach(Runnable::run);
        runLater();

        assertThat(bubbles(loaded.root())).hasSize(before);
    }

    @Test
    @DisplayName("an action waiting for confirmation is rejected and its buttons go off, so nothing is booked afterwards")
    void pendingConfirmationIsDiscarded() throws Exception {
        var loaded = openAssistant(Runnable::run);
        send(loaded.root(), "book_appointment customerId=" + ana.getId() + "; professionalId=" + camila.getId()
            + "; treatmentId=" + cut.getId() + "; start=" + tomorrow + "T10:00; deposit=16500");
        for (int attempt = 0; attempt < 80 && confirmationButtons(loaded.root()).isEmpty(); attempt++) {
            Thread.sleep(25);
        }
        List<Button> buttons = confirmationButtons(loaded.root());
        assertThat(buttons).hasSize(2);

        FxTest.onFxThread(() -> { loaded.controller().dispose(); return null; });
        FxTest.onFxThread(() -> { buttons.forEach(Button::fire); return null; });
        runLater();

        assertThat(buttons).allMatch(Button::isDisabled);
        assertThat(context.appointments().listByDate(tomorrow)).isEmpty();
    }

    @Test
    @DisplayName("closing the navigator empties the area and disposes the screens it kept")
    void navigatorCloseDisposesTheScreens() throws Exception {
        StackPane content = FxTest.onFxThread(StackPane::new);
        Navigator navigator = new Navigator(content, context, AccessRole.ADMIN);
        FxTest.onFxThread(() -> { navigator.show(View.ASSISTANT); return null; });
        Node chat = FxTest.onFxThread(() -> content.getChildren().get(0));
        FxTest.onFxThread(() -> { navigator.show(View.HOME); return null; });

        FxTest.onFxThread(() -> { navigator.close(); return null; });

        assertThat(FxTest.onFxThread(() -> content.getChildren())).isEmpty();
        assertThat(navigator.current()).isEmpty();
        FxTest.onFxThread(() -> { navigator.show(View.ASSISTANT); return null; });
        assertThat(FxTest.onFxThread(() -> content.getChildren().get(0))).isNotSameAs(chat);
    }

    @Test
    @DisplayName("a try-on that is generated after the session closed is not shown")
    void tryOnLateResultIsIgnored() throws Exception {
        var loaded = FxTest.onFxThread(() -> {
            ViewLoader.Loaded<TryOnController> opened = ViewLoader.loadWithController(
                SessionLifecycleTest.class.getResource("/com/maisonglow/view/TryOnView.fxml"), context);
            opened.controller().useExecutor(slow::add);
            new Scene(opened.root(), 1250, 760);
            opened.root().applyCss();
            opened.root().layout();
            return opened;
        });

        FxTest.onFxThread(() -> { loaded.controller().dispose(); return null; });
        FxTest.onFxThread(() -> { ((Button) loaded.root().lookup("#generateButton")).fire(); return null; });
        slow.forEach(Runnable::run);
        runLater();

        assertThat(slow).isEmpty();
        assertThat(FxTest.onFxThread(() -> loaded.root().lookup("#generateButton").isDisabled())).isFalse();
    }
}
