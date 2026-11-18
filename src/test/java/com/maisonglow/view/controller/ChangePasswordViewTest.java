package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLoader;
import com.maisonglow.view.access.InMemoryAccessGateway;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Change password view tests")
class ChangePasswordViewTest {

    private static boolean toolkit;

    @TempDir
    Path data;

    private InMemoryAccessGateway access;
    private AppContext context;
    private int done;
    private int cancelled;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @BeforeEach
    void prepare() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        context = new AppContext(data, Clock.systemDefaultZone(), AiConfig.simulated());
        access = new InMemoryAccessGateway();
        access.addAdministrator("jefa", "La jefa", "clave-de-prueba");
        access.login("jefa", "clave-de-prueba");
        context.useAccess(access);
    }

    private Parent open() throws Exception {
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<ChangePasswordController> loaded = ViewLoader.loadWithController(
                ChangePasswordViewTest.class.getResource("/com/maisonglow/view/ChangePasswordView.fxml"), context);
            loaded.controller().useExecutor(Runnable::run);
            loaded.controller().onDone(() -> done++);
            loaded.controller().onCancel(() -> cancelled++);
            new Scene(loaded.root(), 480, 560);
            loaded.root().applyCss();
            loaded.root().layout();
            return loaded.root();
        });
    }

    private static void type(Parent root, String id, String text) throws Exception {
        FxTest.onFxThread(() -> {
            ((PasswordField) root.lookup("#" + id)).setText(text);
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

    private static void waitFor(Parent root, String id) throws Exception {
        for (int attempt = 0; attempt < 80 && shown(root, id).isEmpty(); attempt++) {
            Thread.sleep(25);
        }
    }

    @Test
    @DisplayName("a wrong current password is refused under that field and nothing changes")
    void wrongCurrentPassword() throws Exception {
        Parent root = open();
        type(root, "currentField", "mala");
        type(root, "newField", "nueva-clave-1");
        type(root, "confirmationField", "nueva-clave-1");

        press(root, "saveButton");
        waitFor(root, "currentMessage");

        assertThat(shown(root, "currentMessage")).contains("actual");
        assertThat(shown(root, "newMessage")).isEmpty();
        assertThat(done).isZero();
    }

    @Test
    @DisplayName("a short new password and a confirmation that does not match are shown under their own fields")
    void invalidNewPassword() throws Exception {
        Parent root = open();
        type(root, "currentField", "clave-de-prueba");
        type(root, "newField", "corta");
        type(root, "confirmationField", "corta");
        press(root, "saveButton");
        waitFor(root, "newMessage");
        assertThat(shown(root, "newMessage")).contains("12");

        type(root, "newField", "nueva-clave-1");
        type(root, "confirmationField", "distinta-1");
        press(root, "saveButton");
        waitFor(root, "confirmationMessage");
        assertThat(shown(root, "confirmationMessage")).contains("confirmación");
        assertThat(done).isZero();
    }

    @Test
    @DisplayName("a valid change tells the window, clears the fields, and the old password stops working")
    void changesThePassword() throws Exception {
        Parent root = open();
        type(root, "currentField", "clave-de-prueba");
        type(root, "newField", "nueva-clave-1");
        type(root, "confirmationField", "nueva-clave-1");

        press(root, "saveButton");
        for (int attempt = 0; attempt < 80 && done == 0; attempt++) {
            Thread.sleep(25);
        }

        assertThat(done).isEqualTo(1);
        assertThat(FxTest.onFxThread(() -> ((PasswordField) root.lookup("#newField")).getText())).isEmpty();
        access.logout();
        assertThat(access.login("jefa", "nueva-clave-1")).isNotNull();
    }

    @Test
    @DisplayName("with the session ended the change is refused above the buttons")
    void endedSession() throws Exception {
        access.logout();
        Parent root = open();
        type(root, "currentField", "clave-de-prueba");
        type(root, "newField", "nueva-clave-1");
        type(root, "confirmationField", "nueva-clave-1");

        press(root, "saveButton");
        waitFor(root, "formMessage");

        assertThat(shown(root, "formMessage")).contains("sesión");
    }

    @Test
    @DisplayName("Cancelar asks the window to close without changing anything")
    void cancel() throws Exception {
        Parent root = open();

        press(root, "cancelButton");

        assertThat(cancelled).isEqualTo(1);
        assertThat(done).isZero();
    }
}
