package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.model.Commerce.ChangeKind;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLifecycle;
import com.maisonglow.view.ViewLoader;
import com.maisonglow.view.shop.ShopFixture;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Mis cambios: historial propio, fotos y permiso del negocio, sobre los servicios reales")
class MyChangesViewTest {

    private static boolean toolkit;

    @TempDir
    Path data;

    @TempDir
    Path files;

    private ShopFixture shop;
    private AppContext context;
    private Path before;
    private Path after;
    private int anaId;
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
        before = image("antes.png", 0x6b4a3a);
        after = image("despues.png", 0xd4a94f);
        anaId = context.portal().profile().getId();
    }

    private Path image(String name, int color) throws Exception {
        BufferedImage image = new BufferedImage(120, 90, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 120; x++) {
            for (int y = 0; y < 90; y++) {
                image.setRGB(x, y, color);
            }
        }
        Path file = files.resolve(name);
        ImageIO.write(image, "png", file.toFile());
        return file;
    }

    private ViewLoader.Loaded<MyChangesController> open(boolean confirm) throws Exception {
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<MyChangesController> loaded = ViewLoader.loadWithController(
                MyChangesViewTest.class.getResource("/com/maisonglow/view/MyChangesView.fxml"), context);
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

    private static long photos(Parent root) throws Exception {
        settle();
        return FxTest.onFxThread(() -> root.lookup("#timelineBox").lookupAll(".image-view").stream()
            .filter(node -> node instanceof ImageView view && view.getImage() != null).count());
    }

    private static void type(Parent root, String notes) throws Exception {
        FxTest.onFxThread(() -> {
            ((TextArea) root.lookup("#notesArea")).setText(notes);
            return null;
        });
    }

    private static void press(Parent root, String id) throws Exception {
        settle();
        FxTest.onFxThread(() -> {
            ((javafx.scene.control.ButtonBase) root.lookup("#" + id)).fire();
            return null;
        });
        settle();
    }

    private void choosePhoto(ViewLoader.Loaded<MyChangesController> screen, boolean beforeBox, Path photo) throws Exception {
        (beforeBox ? screen.controller().beforePicker() : screen.controller().afterPicker()).useFilePicker(() -> photo);
        String host = beforeBox ? "#beforeBox" : "#afterBox";
        FxTest.onFxThread(() -> {
            ((Button) screen.root().lookup(host).lookup("#uploadButton")).fire();
            ((Button) screen.root().lookup(host).lookup("#useButton")).fire();
            return null;
        });
    }

    @Test
    @DisplayName("with no changes it says where to start, the business sees nothing yet and the date is today")
    void opensEmpty() throws Exception {
        Parent root = open(true).root();

        assertThat(texts(root, "timelineBox")).singleElement().asString().contains("Todavía no has registrado cambios");
        assertThat(FxTest.onFxThread(() -> ((CheckBox) root.lookup("#staffAccessBox")).isSelected())).isFalse();
        assertThat(text(root, "privacyNote")).contains("nadie del negocio");
        assertThat(FxTest.onFxThread(() -> ((DatePicker) root.lookup("#datePicker")).getValue())).isEqualTo(LocalDate.now(shop.clock));
    }

    @Test
    @DisplayName("a personal change with only notes is saved for the customer of the session and shows in the timeline")
    void addsAPersonalChange() throws Exception {
        Parent root = open(true).root();
        type(root, "Me pinté el cabello cobrizo");

        press(root, "saveButton");

        assertThat(context.changes().mine()).singleElement().satisfies(change -> {
            assertThat(change.kind()).isEqualTo(ChangeKind.PERSONAL);
            assertThat(change.notes()).isEqualTo("Me pinté el cabello cobrizo");
            assertThat(change.customerId()).isEqualTo(anaId);
            assertThat(change.happenedOn()).isEqualTo(LocalDate.now(shop.clock));
        });
        assertThat(texts(root, "timelineBox")).contains("Cambio personal", "Me pinté el cabello cobrizo");
        assertThat(FxTest.onFxThread(() -> ((TextArea) root.lookup("#notesArea")).getText())).isEmpty();
    }

    @Test
    @DisplayName("the photos before and after are kept with the change, shown in its card, and only when the customer uses them")
    void addsPhotos() throws Exception {
        var screen = open(true);
        choosePhoto(screen, true, before);
        choosePhoto(screen, false, after);
        type(screen.root(), "Corte nuevo");

        press(screen.root(), "saveButton");

        assertThat(context.changes().mine()).singleElement().satisfies(change -> {
            assertThat(change.sourceKey()).isNotNull();
            assertThat(change.resultKey()).isNotNull();
        });
        assertThat(texts(screen.root(), "timelineBox")).contains("Antes", "Después");
        assertThat(photos(screen.root())).isEqualTo(2);
        assertThat(screen.controller().beforePicker().pending()).isEmpty();
    }

    @Test
    @DisplayName("an empty note is asked for under its field, and a future date is refused under the date; nothing is saved")
    void validations() throws Exception {
        Parent root = open(true).root();

        press(root, "saveButton");
        assertThat(texts(root, "notesMessage")).singleElement().asString().contains("Cuéntanos");

        type(root, "Algo");
        FxTest.onFxThread(() -> {
            ((DatePicker) root.lookup("#datePicker")).setValue(LocalDate.now(shop.clock).plusDays(2));
            return null;
        });
        press(root, "saveButton");

        assertThat(texts(root, "dateMessage")).singleElement().asString().containsIgnoringCase("fecha");
        assertThat(context.changes().mine()).isEmpty();
    }

    @Test
    @DisplayName("future days cannot be chosen in the calendar")
    void futureDaysAreOff() throws Exception {
        Parent root = open(true).root();
        settle();

        boolean ok = FxTest.onFxThread(() -> {
            DatePicker picker = (DatePicker) root.lookup("#datePicker");
            DateCell tomorrow = picker.getDayCellFactory().call(picker);
            tomorrow.updateItem(LocalDate.now(shop.clock).plusDays(1), false);
            DateCell past = picker.getDayCellFactory().call(picker);
            past.updateItem(LocalDate.now(shop.clock).minusDays(5), false);
            return tomorrow.isDisabled() && !past.isDisabled();
        });

        assertThat(ok).isTrue();
    }

    @Test
    @DisplayName("by default the business cannot see the history; authorizing lets it, and revoking takes it away again")
    void consent() throws Exception {
        context.changes().addOwn(ChangeKind.PERSONAL, "Algo privado", LocalDate.now(shop.clock), null, null);
        Parent root = open(true).root();
        shop.asAdmin();
        assertThatThrownBy(() -> context.changes().forCustomer(anaId)).isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("no ha autorizado");
        shop.asCustomer("ana");

        press(root, "staffAccessBox");

        assertThat(context.changes().staffAccessAllowed()).isTrue();
        assertThat(text(root, "privacyNote")).contains("puede consultar");
        shop.asAdmin();
        assertThat(context.changes().forCustomer(anaId)).hasSize(1);
        shop.asCustomer("ana");

        press(root, "staffAccessBox");

        assertThat(context.changes().staffAccessAllowed()).isFalse();
        shop.asAdmin();
        assertThatThrownBy(() -> context.changes().forCustomer(anaId)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("removing the photos asks first and keeps the record with its notes; answering no keeps everything")
    void removesPhotos() throws Exception {
        context.changes().addOwn(ChangeKind.PERSONAL, "Con fotos", LocalDate.now(shop.clock), before, after);
        Parent declined = open(false).root();
        assertThat(photos(declined)).isEqualTo(2);
        FxTest.onFxThread(() -> {
            declined.lookupAll(".button").stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> "Retirar fotos".equals(button.getText())).findFirst().orElseThrow().fire();
            return null;
        });
        settle();
        assertThat(context.changes().mine().getFirst().sourceKey()).isNotNull();

        Parent root = open(true).root();
        FxTest.onFxThread(() -> {
            root.lookupAll(".button").stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> "Retirar fotos".equals(button.getText())).findFirst().orElseThrow().fire();
            return null;
        });
        settle();

        assertThat(dialogs).anyMatch(text -> text.contains("Tus notas se conservan"));
        assertThat(photos(root)).isZero();
        assertThat(texts(root, "timelineBox")).contains("Con fotos");
        assertThat(context.changes().mine().getFirst().sourceKey()).isNull();
    }

    @Test
    @DisplayName("a simulation saved from the try-on says it is not a real result, and only the customer's own changes show")
    void simulationsAndOnlyMine() throws Exception {
        context.changes().addOwn(ChangeKind.SIMULATION, "Rojo cobrizo", LocalDate.now(shop.clock), before, after);
        shop.asCustomer("laura");
        context.changes().addOwn(ChangeKind.PERSONAL, "Cambio de Laura", LocalDate.now(shop.clock), null, null);
        shop.asCustomer("ana");

        Parent root = open(true).root();

        List<String> timeline = texts(root, "timelineBox");
        assertThat(timeline).contains("Simulación (no es un resultado real)", "Rojo cobrizo");
        assertThat(timeline).doesNotContain("Cambio de Laura");
        assertThat(photos(root)).isEqualTo(2);
    }

    @Test
    @DisplayName("an answer that arrives after the session closed is ignored and nothing stays on screen")
    void lateAnswerIsIgnored() throws Exception {
        context.changes().addOwn(ChangeKind.PERSONAL, "Algo", LocalDate.now(shop.clock), null, null);
        ViewLoader.Loaded<MyChangesController> loaded = FxTest.onFxThread(() -> {
            ViewLoader.Loaded<MyChangesController> opened = ViewLoader.loadWithController(
                MyChangesViewTest.class.getResource("/com/maisonglow/view/MyChangesView.fxml"), context);
            new Scene(opened.root(), 1100, 720);
            opened.root().applyCss();
            opened.root().layout();
            ((ViewLifecycle) opened.controller()).dispose();
            return opened;
        });
        Thread.sleep(300);
        settle();

        assertThat(texts(loaded.root(), "timelineBox")).isEmpty();
    }
}
