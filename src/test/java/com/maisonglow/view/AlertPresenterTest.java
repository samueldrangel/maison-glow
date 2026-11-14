package com.maisonglow.view;

import com.maisonglow.exception.ValidationException;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("AlertPresenter tests")
class AlertPresenterTest {

    private static boolean toolkit;

    private final List<Alert> shown = new ArrayList<>();

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    /** A presenter that records the dialog and answers with the given button instead of opening a window. */
    private AlertPresenter presenter(Function<Alert, Optional<ButtonType>> answer) {
        return new AlertPresenter(() -> null, alert -> {
            shown.add(alert);
            return answer.apply(alert);
        });
    }

    private static Optional<ButtonType> pressing(Alert alert, String label) {
        return alert.getButtonTypes().stream().filter(button -> button.getText().equals(label)).findFirst();
    }

    @Test
    @DisplayName("explains a service error with its message and the look of the application")
    void showsAServiceError() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");

        FxTest.onFxThread(() -> {
            presenter(alert -> Optional.empty()).showError(new ValidationException("El campo correo es obligatorio."));
            return null;
        });

        Alert alert = shown.get(0);
        assertThat(alert.getAlertType()).isEqualTo(AlertType.WARNING);
        assertThat(((Label) alert.getDialogPane().getContent().lookup(".dialog-title")).getText()).isEqualTo("Datos no válidos");
        assertThat(((Label) alert.getDialogPane().getContent().lookup(".dialog-text")).getText()).isEqualTo("El campo correo es obligatorio.");
        assertThat(alert.getDialogPane().getContent().lookup(".dialog-medallion").getStyleClass()).contains("tone-warning");
        assertThat(alert.getDialogPane().getStylesheets()).anyMatch(url -> url.endsWith("styles.css"));
        assertThat(alert.getDialogPane().getStyleClass()).contains("view-root");
        assertThat(alert.getButtonTypes()).extracting(ButtonType::getText).containsExactly("Entendido");
        assertThat(alert.getDialogPane().lookupButton(alert.getButtonTypes().get(0)).getStyleClass()).contains("button-primary");
    }

    @Test
    @DisplayName("shows a notice")
    void showsANotice() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");

        FxTest.onFxThread(() -> {
            presenter(alert -> Optional.empty()).showInfo("Venta registrada.");
            return null;
        });

        assertThat(shown.get(0).getAlertType()).isEqualTo(AlertType.INFORMATION);
        assertThat(shown.get(0).getContentText()).isEqualTo("Venta registrada.");
    }

    @Test
    @DisplayName("confirms only when the user presses Confirmar")
    void confirmsOnlyWithTheConfirmButton() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");

        boolean confirmed = FxTest.onFxThread(() -> presenter(alert -> pressing(alert, "Confirmar")).confirm("¿Eliminar?"));
        boolean cancelled = FxTest.onFxThread(() -> presenter(alert -> pressing(alert, "Cancelar")).confirm("¿Eliminar?"));
        boolean closed = FxTest.onFxThread(() -> presenter(alert -> Optional.empty()).confirm("¿Eliminar?"));

        assertThat(confirmed).isTrue();
        assertThat(cancelled).isFalse();
        assertThat(closed).as("closing the window is not a confirmation").isFalse();
    }
}
