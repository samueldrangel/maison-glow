package com.maisonglow.view;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Toast tests")
class ToastTest {

    private static boolean toolkit;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @Test
    @DisplayName("is a check mark and the text, with the look of the palette")
    void buildsTheNotice() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");

        Node toast = FxTest.onFxThread(() -> Toast.build("Venta registrada."));

        assertThat(toast.getStyleClass()).contains("toast", "view-root");
        assertThat(((Label) toast.lookup(".toast-text")).getText()).isEqualTo("Venta registrada.");
        assertThat(((HBox) toast).getChildren()).hasSize(2);
        assertThat(((HBox) toast).getStylesheets()).anyMatch(url -> url.endsWith("styles.css"));
    }

    @Test
    @DisplayName("shows nothing, and does not fail, when there is no window")
    void doesNothingWithoutAWindow() {
        assertThatCode(() -> Toast.show(null, "Venta registrada.")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("is reachable from AlertPresenter with one line")
    void reachableFromThePresenter() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");

        assertThatCode(() -> FxTest.onFxThread(() -> {
            new AlertPresenter(() -> null, alert -> java.util.Optional.empty()).toast("Cliente guardado.");
            return null;
        })).doesNotThrowAnyException();
    }
}
