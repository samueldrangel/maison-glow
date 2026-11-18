package com.maisonglow.view;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import com.maisonglow.view.access.AccessRole;
import com.maisonglow.view.access.AccessSession;
import com.maisonglow.view.access.InMemoryAccessGateway;
import com.maisonglow.view.access.RegistrationData;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Window flow by session tests")
class AppWindowTest {

    private static boolean toolkit;

    @TempDir
    Path data;

    private AppContext context;
    private InMemoryAccessGateway access;
    private AppWindow window;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @BeforeEach
    void prepare() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        context = com.maisonglow.TestAccounts.open(data, Clock.systemDefaultZone(), AiConfig.simulated());
        access = new InMemoryAccessGateway();
        access.addAdministrator("jefa", "La jefa", "clave-de-prueba");
        context.useAccess(access);
        window = FxTest.onFxThread(() -> new AppWindow(new Stage(), context, stage -> { }));
    }

    private boolean has(String selector) throws Exception {
        return FxTest.onFxThread(() -> {
            window.root().applyCss();
            window.root().layout();
            return window.root().lookup(selector) != null;
        });
    }

    /** The menu entries a person sees, by the screen each one opens. */
    private static java.util.List<String> menuOf(Parent root) throws Exception {
        return FxTest.onFxThread(() -> root.lookupAll(".nav-button").stream()
            .filter(javafx.scene.control.ToggleButton.class::isInstance).filter(javafx.scene.Node::isVisible)
            .map(button -> (String) button.getUserData()).toList());
    }

    @Test
    @DisplayName("with nobody signed in the first screen is the sign-in")
    void startsAtTheSignIn() throws Exception {
        FxTest.onFxThread(() -> { window.start(); return null; });

        assertThat(has("#loginButton")).isTrue();
        assertThat(has("#sidebar")).isFalse();
    }

    @Test
    @DisplayName("an administrator reaches the menu with their name and role, and a customer never does")
    void areaDependsOnTheRole() throws Exception {
        FxTest.onFxThread(() -> { window.showArea(new AccessSession("jefa", "La jefa", AccessRole.ADMIN)); return null; });

        assertThat(has("#sidebar")).isTrue();
        assertThat(menuOf(window.root())).contains("HOME", "PRODUCTS", "SALES", "ORDERS", "SETTINGS").doesNotContain("SHOP", "PROFILE", "MY_PURCHASES");
        assertThat(FxTest.onFxThread(() -> ((Label) window.root().lookup("#userName")).getText())).isEqualTo("La jefa");
        assertThat(FxTest.onFxThread(() -> ((Label) window.root().lookup("#userRole")).getText())).isEqualTo("Administración");
        assertThat(FxTest.onFxThread(() -> ((Label) window.root().lookup("#avatarInitials")).getText())).isEqualTo("LJ");

        FxTest.onFxThread(() -> { window.showArea(new AccessSession("ana", "Ana García", AccessRole.CUSTOMER)); return null; });

        assertThat(FxTest.onFxThread(() -> ((Label) window.root().lookup("#userRole")).getText())).isEqualTo("Cliente");
        assertThat(menuOf(window.root())).containsExactly("CUSTOMER_HOME", "PROFILE", "SHOP", "MY_PURCHASES", "MY_APPOINTMENTS", "MY_CHANGES");
    }

    @Test
    @DisplayName("closing the session goes back to the sign-in with a new menu, and the session is closed")
    void logoutDiscardsTheArea() throws Exception {
        access.login("jefa", "clave-de-prueba");
        FxTest.onFxThread(() -> { window.start(); return null; });
        Parent area = FxTest.onFxThread(window::root);
        assertThat(has("#sidebar")).isTrue();

        FxTest.onFxThread(() -> {
            ((Button) window.root().lookup("#logoutButton")).fire();
            return null;
        });

        assertThat(access.current()).isEmpty();
        assertThat(has("#loginButton")).isTrue();
        FxTest.onFxThread(() -> { window.showArea(new AccessSession("jefa", "La jefa", AccessRole.ADMIN)); return null; });
        assertThat(FxTest.onFxThread(window::root)).isNotSameAs(area);
    }

    @Test
    @DisplayName("the sign-up and the sign-in link to each other")
    void movesBetweenSignInAndSignUp() throws Exception {
        FxTest.onFxThread(() -> { window.showLogin(); return null; });
        FxTest.onFxThread(() -> { ((Button) window.root().lookup("#registerLink")).fire(); return null; });
        assertThat(has("#registerButton")).isTrue();

        FxTest.onFxThread(() -> {
            window.root().applyCss();
            window.root().layout();
            ((Button) window.root().lookup("#loginLink")).fire();
            return null;
        });
        assertThat(has("#loginButton")).isTrue();
        assertThat(new RegistrationData("a", "b", "c", "d", "e", "secreta", "secreta").toString()).doesNotContain("secreta");
    }
}
