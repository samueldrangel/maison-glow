package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLoader;
import com.maisonglow.view.access.AccessSession;
import com.maisonglow.view.access.InMemoryAccessGateway;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Sign-in and sign-up view tests")
class AccessViewsTest {

    private static boolean toolkit;

    @TempDir
    Path data;

    private AppContext context;
    private InMemoryAccessGateway access;
    private final List<AccessSession> signedIn = new ArrayList<>();
    private int registerRequests;
    private int loginRequests;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @BeforeEach
    void prepare() {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        context = com.maisonglow.TestAccounts.open(data, Clock.systemDefaultZone(), AiConfig.simulated());
        access = new InMemoryAccessGateway();
        access.addAdministrator("jefa", "La jefa", "clave-de-prueba");
        context.useAccess(access);
    }

    private Parent openLogin() throws Exception {
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<LoginController> loaded = ViewLoader.loadWithController(
                AccessViewsTest.class.getResource("/com/maisonglow/view/LoginView.fxml"), context);
            loaded.controller().useExecutor(Runnable::run);
            loaded.controller().onSignedIn(signedIn::add);
            loaded.controller().onRegister(() -> registerRequests++);
            new Scene(loaded.root(), 1100, 720);
            loaded.root().applyCss();
            loaded.root().layout();
            return loaded.root();
        });
    }

    private Parent openRegister() throws Exception {
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<RegisterController> loaded = ViewLoader.loadWithController(
                AccessViewsTest.class.getResource("/com/maisonglow/view/RegisterView.fxml"), context);
            loaded.controller().useExecutor(Runnable::run);
            loaded.controller().onSignedIn(signedIn::add);
            loaded.controller().onLogin(() -> loginRequests++);
            new Scene(loaded.root(), 1100, 720);
            loaded.root().applyCss();
            loaded.root().layout();
            return loaded.root();
        });
    }

    private static void type(Parent root, String id, String text) throws Exception {
        FxTest.onFxThread(() -> {
            ((TextField) root.lookup("#" + id)).setText(text);
            return null;
        });
    }

    private static void press(Parent root, String id) throws Exception {
        FxTest.onFxThread(() -> {
            ((Button) root.lookup("#" + id)).fire();
            return null;
        });
    }

    private static String shown(Parent root, String id) throws Exception {
        return FxTest.onFxThread(() -> {
            Label label = (Label) root.lookup("#" + id);
            return label.isVisible() ? label.getText() : "";
        });
    }

    /** The answer comes back on the FX thread a moment later; this waits for what the test expects. */
    private static void until(Check condition) throws Exception {
        for (int attempt = 0; attempt < 80 && !condition.met(); attempt++) {
            Thread.sleep(25);
        }
    }

    @FunctionalInterface
    private interface Check {
        boolean met() throws Exception;
    }

    private static void fillRegister(Parent root, String user, String password, String confirmation) throws Exception {
        type(root, "nameField", "Ana García");
        type(root, "documentField", "1001");
        type(root, "phoneField", "3001112233");
        type(root, "emailField", "ana@correo.com");
        type(root, "usernameField", user);
        type(root, "passwordField", password);
        type(root, "confirmationField", confirmation);
    }

    @Test
    @DisplayName("the password is typed in a hidden field and there is no way to choose a role")
    void hidesThePasswordAndOffersNoRole() throws Exception {
        for (Parent root : List.of(openLogin(), openRegister())) {
            assertThat(FxTest.onFxThread(() -> root.lookup("#passwordField") instanceof PasswordField)).isTrue();
            assertThat(FxTest.onFxThread(() -> root.lookupAll(".combo-box").size() + root.lookupAll(".toggle-button").size())).isZero();
            assertThat(FxTest.onFxThread(() -> root.lookupAll(".radio-button").size())).isZero();
        }
    }

    @Test
    @DisplayName("an empty username or password is rejected under its own field and nobody signs in")
    void loginRejectsEmptyFields() throws Exception {
        Parent root = openLogin();

        press(root, "loginButton");
        until(() -> !shown(root, "usernameMessage").isEmpty());

        assertThat(shown(root, "usernameMessage")).contains("usuario");
        assertThat(signedIn).isEmpty();
        type(root, "usernameField", "jefa");
        press(root, "loginButton");
        until(() -> !shown(root, "passwordMessage").isEmpty());
        assertThat(shown(root, "passwordMessage")).contains("contraseña");
    }

    @Test
    @DisplayName("wrong credentials show one message that does not say which was wrong, and clear the password")
    void loginWrongCredentials() throws Exception {
        Parent root = openLogin();
        type(root, "usernameField", "jefa");
        type(root, "passwordField", "equivocada");

        press(root, "loginButton");
        until(() -> !shown(root, "formMessage").isEmpty());

        assertThat(shown(root, "formMessage")).isEqualTo("Usuario o contraseña incorrectos.");
        assertThat(FxTest.onFxThread(() -> ((PasswordField) root.lookup("#passwordField")).getText())).isEmpty();
        assertThat(FxTest.onFxThread(() -> root.lookup("#loginButton").isDisabled())).isFalse();
        assertThat(signedIn).isEmpty();
    }

    @Test
    @DisplayName("right credentials hand the session to the window and leave no password on screen")
    void loginSucceeds() throws Exception {
        Parent root = openLogin();
        type(root, "usernameField", "jefa");
        type(root, "passwordField", "clave-de-prueba");

        press(root, "loginButton");
        until(() -> !signedIn.isEmpty());

        assertThat(signedIn).singleElement().satisfies(session -> assertThat(session.isAdministrator()).isTrue());
        assertThat(FxTest.onFxThread(() -> ((PasswordField) root.lookup("#passwordField")).getText())).isEmpty();
    }

    @Test
    @DisplayName("the create-account link asks the window to open the sign-up")
    void loginLinksToRegister() throws Exception {
        Parent root = openLogin();

        press(root, "registerLink");

        assertThat(registerRequests).isEqualTo(1);
    }

    @Test
    @DisplayName("a valid sign-up creates a customer account and opens its session")
    void registerSucceeds() throws Exception {
        Parent root = openRegister();
        fillRegister(root, "ana", "clave-segura-1", "clave-segura-1");

        press(root, "registerButton");
        until(() -> !signedIn.isEmpty());

        assertThat(signedIn).singleElement().satisfies(session -> {
            assertThat(session.isAdministrator()).isFalse();
            assertThat(session.displayName()).isEqualTo("Ana García");
        });
    }

    @Test
    @DisplayName("each sign-up error appears under its own field")
    void registerShowsErrorsUnderTheirField() throws Exception {
        Parent root = openRegister();
        fillRegister(root, "ana", "clave-segura-1", "otra-clave-12");
        type(root, "emailField", "sin-arroba");

        press(root, "registerButton");
        until(() -> !shown(root, "emailMessage").isEmpty());
        assertThat(shown(root, "emailMessage")).contains("correo");
        assertThat(signedIn).isEmpty();

        type(root, "emailField", "ana@correo.com");
        press(root, "registerButton");
        until(() -> !shown(root, "confirmationMessage").isEmpty());
        assertThat(shown(root, "confirmationMessage")).contains("confirmación");
        assertThat(shown(root, "passwordMessage")).isEmpty();
    }

    @Test
    @DisplayName("a username already in use is refused under the username, keeping what was typed")
    void registerDuplicateUsername() throws Exception {
        Parent root = openRegister();
        fillRegister(root, "jefa", "clave-segura-1", "clave-segura-1");

        press(root, "registerButton");
        until(() -> !shown(root, "usernameMessage").isEmpty() || !signedIn.isEmpty());

        assertThat(signedIn).isEmpty();
        assertThat(FxTest.onFxThread(() -> ((TextField) root.lookup("#nameField")).getText())).isEqualTo("Ana García");
    }

    @Test
    @DisplayName("the link to sign in asks the window to go back")
    void registerLinksToLogin() throws Exception {
        Parent root = openRegister();

        press(root, "loginLink");

        assertThat(loginRequests).isEqualTo(1);
    }
}
