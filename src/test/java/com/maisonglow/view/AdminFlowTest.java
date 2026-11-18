package com.maisonglow.view;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The whole administrative flow of the block, end to end and over the real services: sign in through the screen, walk every
 * entry of the administrator's menu, close the session, sign in again and check that nothing of the first session is left.
 */
@DisplayName("Administrator flow, from the sign-in to the sign-out and back")
class AdminFlowTest {

    private static final char[] PASSWORD = "clave-de-prueba-123".toCharArray();

    private static boolean toolkit;

    @TempDir
    Path data;

    private AppContext context;
    private AppWindow window;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @BeforeEach
    void prepare() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        AppContext.seed(data);
        context = new AppContext(data, Clock.systemDefaultZone(), AiConfig.simulated());
        context.auth().createFirstAdmin("admin", PASSWORD);
        window = FxTest.onFxThread(() -> new AppWindow(new Stage(), context, stage -> { }));
        FxTest.onFxThread(() -> {
            window.start();
            return null;
        });
    }

    /** Looks a node up on the JavaFX thread, after the screen has been styled and laid out (content of scroll panes needs it). */
    private Node findNow(String selector) {
        Parent root = window.root();
        root.applyCss();
        root.layout();
        return root.lookup(selector);
    }

    private Node find(String selector) throws Exception {
        return FxTest.onFxThread(() -> findNow(selector));
    }

    private void until(BooleanSupplier condition) throws Exception {
        for (int attempt = 0; attempt < 200; attempt++) {
            if (FxTest.onFxThread(condition::getAsBoolean)) {
                return;
            }
            Thread.sleep(50);
        }
        assertThat(FxTest.onFxThread(condition::getAsBoolean)).as("what the flow waited for").isTrue();
    }

    /** Types in the sign-in screen and presses the button, as a person would. */
    private void signIn(String user, String password) throws Exception {
        FxTest.onFxThread(() -> {
            ((TextField) window.root().lookup("#usernameField")).setText(user);
            ((PasswordField) window.root().lookup("#passwordField")).setText(password);
            ((Button) window.root().lookup("#loginButton")).fire();
            return null;
        });
    }

    private List<ToggleButton> menu() throws Exception {
        return FxTest.onFxThread(() -> {
            window.root().applyCss();
            window.root().layout();
            return window.root().lookupAll(".nav-button").stream().filter(ToggleButton.class::isInstance)
                .map(ToggleButton.class::cast).filter(Node::isVisible).toList();
        });
    }

    private List<String> shownTexts() throws Exception {
        return FxTest.onFxThread(() -> window.root().lookup(".content-area").lookupAll(".label").stream()
            .filter(Label.class::isInstance).map(node -> ((Label) node).getText()).toList());
    }

    @Test
    @DisplayName("wrong credentials stay at the sign-in; the right ones open the administrator's menu with their name")
    void signsIn() throws Exception {
        assertThat(find("#loginButton")).isNotNull();

        signIn("admin", "clave-equivocada-1");
        until(() -> findNow("#formMessage").isVisible());
        assertThat(find("#sidebar")).isNull();

        signIn("admin", new String(PASSWORD));
        until(() -> findNow("#sidebar") != null);

        assertThat(((Label) find("#userName")).getText()).isEqualTo("admin");
        assertThat(((Label) find("#userRole")).getText()).isEqualTo("Administración");
    }

    @Test
    @DisplayName("every entry of the administrator's menu opens a real screen, never a placeholder or an error")
    void walksTheWholeMenu() throws Exception {
        signIn("admin", new String(PASSWORD));
        until(() -> findNow("#sidebar") != null);

        List<ToggleButton> entries = menu();
        assertThat(entries.stream().map(entry -> (String) entry.getUserData()))
            .contains("HOME", "CUSTOMERS", "ACCOUNTS", "HISTORY", "APPOINTMENTS", "SALES", "ORDERS", "PRODUCTS",
                "STATISTICS", "ASSISTANT", "TRY_ON", "SETTINGS")
            .doesNotContain("SHOP", "PROFILE", "MY_PURCHASES", "MY_APPOINTMENTS", "MY_CHANGES", "CUSTOMER_HOME");
        for (ToggleButton entry : entries) {
            FxTest.onFxThread(() -> {
                entry.fire();
                return null;
            });
            Thread.sleep(150);
            assertThat(shownTexts()).as("screen of " + entry.getUserData()).doesNotContain("En construcción", "Sin permiso");
        }
    }

    @Test
    @DisplayName("closing the session returns to the sign-in and the next session starts with new screens")
    void signsOutAndBackIn() throws Exception {
        signIn("admin", new String(PASSWORD));
        until(() -> findNow("#sidebar") != null);
        ToggleButton assistant = menu().stream().filter(entry -> "ASSISTANT".equals(entry.getUserData())).findFirst().orElseThrow();
        FxTest.onFxThread(() -> {
            assistant.fire();
            return null;
        });
        until(() -> findNow("#messagesBox") != null);
        Node firstChat = find("#messagesBox");
        assertThat(firstChat).isNotNull();

        FxTest.onFxThread(() -> {
            ((Button) window.root().lookup("#logoutButton")).fire();
            return null;
        });

        assertThat(find("#loginButton")).isNotNull();
        assertThat(find("#sidebar")).isNull();
        assertThat(context.access().current()).isEmpty();

        signIn("admin", new String(PASSWORD));
        until(() -> findNow("#sidebar") != null);
        ToggleButton again = menu().stream().filter(entry -> "ASSISTANT".equals(entry.getUserData())).findFirst().orElseThrow();
        FxTest.onFxThread(() -> {
            again.fire();
            return null;
        });
        until(() -> findNow("#messagesBox") != null);
        assertThat(find("#messagesBox")).isNotSameAs(firstChat);
        assertThat(FxTest.onFxThread(() -> ((StackPane) window.root().lookup(".content-area")).getChildren().size())).isEqualTo(1);
    }
}
