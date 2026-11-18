package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.model.Commerce.Change;
import com.maisonglow.model.Commerce.ChangeKind;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.TreatmentData;
import com.maisonglow.service.WorkSchedule;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLifecycle;
import com.maisonglow.view.ViewLoader;
import com.maisonglow.view.access.AccessRole;
import com.maisonglow.view.access.AccessSession;
import com.maisonglow.view.shop.ShopFixture;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Customer area: virtual try-on and assistant, always on the customer's own account")
class CustomerAiViewTest {

    private static boolean toolkit;

    @TempDir
    Path data;

    @TempDir
    Path files;

    private ShopFixture shop;
    private AppContext context;
    private Treatment cut;
    private Professional camila;
    private Path photo;
    private final List<String> dialogs = new ArrayList<>();

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
        camila = context.professionals().register(new ContactData("2001", "Camila Ruiz", "3001112233", "c@correo.com"),
            "Estilista", new WorkSchedule(LocalTime.of(8, 0), LocalTime.of(18, 0)));
        context.professionals().assignTreatment(camila.getId(), cut.getId());
        shop.asCustomer("ana");
        BufferedImage image = new BufferedImage(160, 200, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 160; x++) {
            for (int y = 0; y < 200; y++) {
                image.setRGB(x, y, 0x6b4a3a);
            }
        }
        photo = files.resolve("foto.png");
        ImageIO.write(image, "png", photo.toFile());
    }

    private static void settle() throws Exception {
        for (int round = 0; round < 4; round++) {
            FxTest.onFxThread(() -> null);
            Thread.sleep(80);
        }
    }

    private AlertPresenter presenter(boolean confirm) {
        return new AlertPresenter(() -> null, alert -> {
            dialogs.add(alert.getContentText());
            return alert.getButtonTypes().stream().filter(button -> confirm && "Confirmar".equals(button.getText())).findFirst();
        });
    }

    private static void collect(Node node, List<String> found) {
        if (node instanceof Label label && label.getText() != null && (label.getStyleClass().contains("bubble-user")
            || label.getStyleClass().contains("bubble-assistant") || label.getStyleClass().contains("form-hint"))) {
            found.add(label.getText());
        }
        if (node instanceof Pane pane) {
            pane.getChildren().forEach(child -> collect(child, found));
        }
    }

    // ---- Probador ----

    private ViewLoader.Loaded<MyTryOnController> openTryOn() throws Exception {
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<MyTryOnController> loaded = ViewLoader.loadWithController(
                CustomerAiViewTest.class.getResource("/com/maisonglow/view/MyTryOnView.fxml"), context);
            loaded.controller().useExecutor(Runnable::run);
            loaded.controller().useAlerts(presenter(true));
            new Scene(loaded.root(), 1200, 760);
            loaded.root().applyCss();
            loaded.root().layout();
            return loaded;
        });
    }

    private static String text(Parent root, String id) throws Exception {
        settle();
        return FxTest.onFxThread(() -> ((Label) root.lookup("#" + id)).getText());
    }

    private static boolean visible(Parent root, String id) throws Exception {
        settle();
        return FxTest.onFxThread(() -> root.lookup("#" + id).isVisible());
    }

    private static boolean disabled(Parent root, String id) throws Exception {
        settle();
        return FxTest.onFxThread(() -> root.lookup("#" + id).isDisabled());
    }

    private static void press(Parent root, String id) throws Exception {
        settle();
        FxTest.onFxThread(() -> {
            ((Button) root.lookup("#" + id)).fire();
            return null;
        });
        settle();
    }

    private void usePhoto(ViewLoader.Loaded<MyTryOnController> screen) throws Exception {
        screen.controller().photoPicker().useFilePicker(() -> photo);
        FxTest.onFxThread(() -> {
            ((Button) screen.root().lookup("#uploadButton")).fire();
            ((Button) screen.root().lookup("#useButton")).fire();
            return null;
        });
    }

    private static void chooseTone(Parent root, String name) throws Exception {
        FxTest.onFxThread(() -> {
            root.lookupAll(".tone-chip").stream().map(ToggleButton.class::cast)
                .filter(chip -> chip.getText().equalsIgnoreCase(name)).findFirst().orElseThrow().fire();
            return null;
        });
    }

    @Test
    @DisplayName("opens with the tones, the simulated notice and no customer to choose, with nothing to save")
    void tryOnOpens() throws Exception {
        Parent root = openTryOn().root();
        settle();

        assertThat(FxTest.onFxThread(() -> root.lookupAll(".tone-chip").size())).isEqualTo(15);
        assertThat(visible(root, "simulatedBanner")).isTrue();
        assertThat(FxTest.onFxThread(() -> root.lookupAll(".combo-box").size())).isZero();
        assertThat(disabled(root, "saveButton")).isTrue();
        assertThat(text(root, "disclaimerLabel")).isEqualTo("Simulación, no garantía");
    }

    @Test
    @DisplayName("generating without a photo or without a tone asks for it under its field and creates nothing")
    void tryOnNeedsPhotoAndTone() throws Exception {
        var screen = openTryOn();

        press(screen.root(), "generateButton");
        assertThat(text(screen.root(), "needPhotoMessage")).contains("foto");
        usePhoto(screen);
        press(screen.root(), "generateButton");

        assertThat(text(screen.root(), "toneMessage")).contains("tono");
        assertThat(FxTest.onFxThread(() -> ((ImageView) screen.root().lookup("#resultView")).getImage())).isNull();
    }

    @Test
    @DisplayName("with a photo and a tone it shows the result with the notice that it is a simulation, and saves nothing by itself")
    void tryOnGenerates() throws Exception {
        var screen = openTryOn();
        usePhoto(screen);
        chooseTone(screen.root(), "Rojo cobrizo");

        press(screen.root(), "generateButton");

        assertThat(FxTest.onFxThread(() -> ((ImageView) screen.root().lookup("#resultView")).getImage())).isNotNull();
        assertThat(text(screen.root(), "disclaimerLabel").toLowerCase()).contains("simulación, no garantía");
        assertThat(disabled(screen.root(), "saveButton")).isFalse();
        assertThat(context.changes().mine()).isEmpty();
    }

    @Test
    @DisplayName("changing the tone discards the result, so a result is never saved against other choices")
    void tryOnInvalidatesTheResult() throws Exception {
        var screen = openTryOn();
        usePhoto(screen);
        chooseTone(screen.root(), "Rojo cobrizo");
        press(screen.root(), "generateButton");
        assertThat(disabled(screen.root(), "saveButton")).isFalse();

        chooseTone(screen.root(), "Rubio dorado");

        assertThat(disabled(screen.root(), "saveButton")).isTrue();
        assertThat(FxTest.onFxThread(() -> ((ImageView) screen.root().lookup("#resultView")).getImage())).isNull();
    }

    @Test
    @DisplayName("Guardar en Mis cambios keeps the photo and the result as a simulation of the customer, never as a real change")
    void tryOnSaves() throws Exception {
        var screen = openTryOn();
        usePhoto(screen);
        chooseTone(screen.root(), "Rojo cobrizo");
        press(screen.root(), "generateButton");

        press(screen.root(), "saveButton");

        List<Change> saved = context.changes().mine();
        assertThat(saved).singleElement().satisfies(change -> {
            assertThat(change.kind()).isEqualTo(ChangeKind.SIMULATION);
            assertThat(change.notes()).contains("rojo cobrizo").contains("50 %");
            assertThat(change.sourceKey()).isNotNull();
            assertThat(change.resultKey()).isNotNull();
            assertThat(change.happenedOn()).isEqualTo(LocalDate.now(shop.clock));
        });
    }

    @Test
    @DisplayName("an answer that arrives after the session closed is ignored by the try-on")
    void tryOnLateAnswerIsIgnored() throws Exception {
        List<Runnable> held = new ArrayList<>();
        var screen = FxTest.onFxThread(() -> {
            ViewLoader.Loaded<MyTryOnController> loaded = ViewLoader.loadWithController(
                CustomerAiViewTest.class.getResource("/com/maisonglow/view/MyTryOnView.fxml"), context);
            loaded.controller().useExecutor(held::add);
            loaded.controller().useAlerts(presenter(true));
            new Scene(loaded.root(), 1200, 760);
            loaded.root().applyCss();
            loaded.root().layout();
            return loaded;
        });
        usePhoto(screen);
        chooseTone(screen.root(), "Rojo cobrizo");
        press(screen.root(), "generateButton");

        FxTest.onFxThread(() -> { ((ViewLifecycle) screen.controller()).dispose(); return null; });
        held.forEach(Runnable::run);
        settle();

        assertThat(screen.controller().result()).isNull();
    }

    // ---- Asistente ----

    private ViewLoader.Loaded<AssistantController> openAssistant() throws Exception {
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<AssistantController> loaded = ViewLoader.loadWithController(
                CustomerAiViewTest.class.getResource("/com/maisonglow/view/MyAssistantView.fxml"), context);
            loaded.controller().useExecutor(Runnable::run);
            new Scene(loaded.root(), 1200, 760);
            loaded.root().applyCss();
            loaded.root().layout();
            return loaded;
        });
    }

    private static List<String> bubbles(Parent root) throws Exception {
        settle();
        return FxTest.onFxThread(() -> {
            List<String> texts = new ArrayList<>();
            collect(root.lookup("#messagesBox"), texts);
            return texts;
        });
    }

    private static void send(Parent root, String message) throws Exception {
        settle();
        FxTest.onFxThread(() -> {
            ((TextField) root.lookup("#messageField")).setText(message);
            ((Button) root.lookup("#sendButton")).fire();
            return null;
        });
        settle();
    }

    @Test
    @DisplayName("a customer gets their own shortcuts and welcome, and none of the business's")
    void assistantShortcuts() throws Exception {
        assertThat(context.access().current()).map(AccessSession::role).contains(AccessRole.CUSTOMER);
        Parent root = openAssistant().root();

        List<String> buttons = FxTest.onFxThread(() -> List.of("lowStockShortcut", "todayShortcut", "customerShortcut", "helpShortcut").stream()
            .map(id -> ((Button) root.lookup("#" + id)).getText()).toList());

        assertThat(buttons).containsExactly("Productos de la tienda", "Mis citas de hoy", "Mi perfil", "Ayuda");
        assertThat(bubbles(root)).singleElement().asString().contains("productos de la tienda").doesNotContain("stock");
    }

    @Test
    @DisplayName("the shortcuts answer about the customer's own profile and the published products")
    void assistantAnswersWithTheirOwnData() throws Exception {
        Parent root = openAssistant().root();

        FxTest.onFxThread(() -> { ((Button) root.lookup("#customerShortcut")).fire(); return null; });
        FxTest.onFxThread(() -> { ((Button) root.lookup("#lowStockShortcut")).fire(); return null; });

        List<String> chat = bubbles(root);
        assertThat(chat).anyMatch(text -> text.contains("Ana García"));
        assertThat(chat).anyMatch(text -> text.contains("Sérum Vitamina C"));
    }

    @Test
    @DisplayName("the business's tools are not available to a customer: the assistant says the tool is not registered")
    void assistantRefusesBusinessTools() throws Exception {
        Parent root = openAssistant().root();

        send(root, "stock bajo");

        assertThat(bubbles(root)).anyMatch(text -> text.contains("no registrada"));
        send(root, "buscar cliente laura");
        assertThat(bubbles(root)).noneMatch(text -> text.contains("Laura Pérez"));
    }

    @Test
    @DisplayName("booking through the assistant shows names, waits for Confirmar and books for the customer of the session")
    void assistantBooksWithConfirmation() throws Exception {
        Parent root = openAssistant().root();
        LocalDateTime start = LocalDate.now(shop.clock).plusDays(3).atTime(10, 0);

        send(root, "book_appointment professionalId=" + camila.getId() + "; treatmentId=" + cut.getId() + "; start=" + start);

        assertThat(context.portal().myAppointments()).isEmpty();
        List<String> shown = FxTest.onFxThread(() -> root.lookupAll(".stat-value").stream().map(node -> ((Label) node).getText()).toList());
        assertThat(shown).contains("Camila Ruiz", "Corte y peinado");
        FxTest.onFxThread(() -> {
            root.lookupAll(".button-compact").stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> "Confirmar".equals(button.getText())).findFirst().orElseThrow().fire();
            return null;
        });
        settle();

        assertThat(context.portal().myAppointments()).singleElement().satisfies(appointment -> {
            assertThat(appointment.getCustomer().getName()).isEqualTo("Ana García");
            assertThat(appointment.getStart()).isEqualTo(start);
        });
        assertThat(Optional.of(dialogs)).isPresent();
    }
}
