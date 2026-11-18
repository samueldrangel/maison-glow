package com.maisonglow.view;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.view.access.SessionExpiredException;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Shared components for loading, errors and ended sessions")
class SharedComponentsTest {

    private static boolean toolkit;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    /** Waits for what comes back on the JavaFX thread. */
    private static void until(java.util.function.BooleanSupplier condition) throws Exception {
        for (int attempt = 0; attempt < 80 && !condition.getAsBoolean(); attempt++) {
            Thread.sleep(25);
        }
    }

    @Test
    @DisplayName("the result comes back on the JavaFX thread and an error comes as the exception that was thrown")
    void bringsTheResultBack() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        List<String> got = new CopyOnWriteArrayList<>();
        List<Boolean> onFx = new CopyOnWriteArrayList<>();

        BackgroundTask.run(() -> "listo", result -> { got.add(result); onFx.add(javafx.application.Platform.isFxApplicationThread()); },
            error -> got.add("error"), () -> true);
        BackgroundTask.run(() -> { throw new BusinessRuleException("regla rota"); }, result -> got.add("no"),
            error -> { got.add(error.getClass().getSimpleName() + ":" + error.getMessage()); onFx.add(javafx.application.Platform.isFxApplicationThread()); },
            () -> true);
        until(() -> got.size() == 2);

        assertThat(got).containsExactlyInAnyOrder("listo", "BusinessRuleException:regla rota");
        assertThat(onFx).containsOnly(true);
    }

    @Test
    @DisplayName("an answer the screen no longer wants is dropped, whether it is a result or an error")
    void dropsWhatIsNotWanted() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        List<String> got = new ArrayList<>();
        boolean[] wanted = {true};
        List<Runnable> held = new ArrayList<>();

        BackgroundTask.run(held::add, () -> "tarde", got::add, error -> got.add("error"), () -> wanted[0]);
        BackgroundTask.<String>run(held::add, () -> { throw new DataAccessException("disco"); }, got::add, error -> got.add("error"), () -> wanted[0]);
        wanted[0] = false;
        held.forEach(Runnable::run);
        FxTest.onFxThread(() -> null);
        Thread.sleep(150);
        FxTest.onFxThread(() -> null);

        assertThat(got).isEmpty();
    }

    @Test
    @DisplayName("the loading veil covers the screen with its text, blocks what is behind and goes away")
    void loadingOverlay() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        StackPane host = FxTest.onFxThread(() -> {
            StackPane pane = new StackPane(new Button("Guardar"));
            new Scene(pane, 400, 300);
            return pane;
        });
        LoadingOverlay overlay = FxTest.onFxThread(() -> LoadingOverlay.over(host));
        assertThat(overlay.isShown()).isFalse();

        FxTest.onFxThread(() -> { overlay.show("Guardando la foto…"); return null; });

        assertThat(overlay.isShown()).isTrue();
        assertThat(FxTest.onFxThread(() -> host.getChildren().get(host.getChildren().size() - 1))).isSameAs(overlay);
        assertThat(FxTest.onFxThread(() -> overlay.lookup(".loading-text") != null)).isTrue();
        assertThat(FxTest.onFxThread(() -> overlay.isMouseTransparent())).isFalse();
        FxTest.onFxThread(() -> { overlay.hide(); return null; });
        assertThat(overlay.isShown()).isFalse();
    }

    @Test
    @DisplayName("a file error and an ended session get their own clear message and never a stack trace")
    void messagesForFilesAndSession() {
        AlertMessages.Message file = AlertMessages.of(new IOException("C:\\datos\\foto.png (Acceso denegado)"));
        AlertMessages.Message session = AlertMessages.of(new SessionExpiredException());

        assertThat(file.title()).isEqualTo("Error de archivo");
        assertThat(file.text()).doesNotContain("C:\\").contains("archivo");
        assertThat(session.title()).isEqualTo("Sesión terminada");
        assertThat(session.text()).isEqualTo(SessionExpiredException.MESSAGE);
        assertThat(AlertMessages.of(new DataAccessException("SQLITE_BUSY")).text()).doesNotContain("SQLITE");
    }

    @Test
    @DisplayName("showing an ended session explains it and then sends the window back to the sign-in; other errors do not")
    void endedSessionLeadsToTheSignIn() {
        int[] returned = {0};
        AlertPresenter.onSessionExpired(() -> returned[0]++);
        List<String> dialogs = new ArrayList<>();
        AlertPresenter alerts = new AlertPresenter(() -> null, alert -> {
            dialogs.add(alert.getContentText());
            return Optional.empty();
        });
        assumeTrue(FxTest.start(), "JavaFX toolkit needs a display");

        try {
            FxTest.onFxThread(() -> { alerts.showError(new BusinessRuleException("otra regla")); return null; });
            assertThat(returned[0]).isZero();
            FxTest.onFxThread(() -> { alerts.showError(new SessionExpiredException()); return null; });
        } catch (Exception e) {
            throw new AssertionError(e);
        } finally {
            AlertPresenter.onSessionExpired(() -> { });
        }

        assertThat(returned[0]).isEqualTo(1);
        assertThat(dialogs).hasSize(2);
    }
}
