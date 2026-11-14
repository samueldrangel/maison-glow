package com.maisonglow.view;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import javafx.scene.control.Alert.AlertType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@DisplayName("AlertMessages tests")
class AlertMessagesTest {

    @Test
    @DisplayName("shows the validation message as a warning")
    void validationIsAWarningWithItsMessage() {
        AlertMessages.Message message = AlertMessages.of(new ValidationException("El campo correo es obligatorio."));

        assertThat(message.type()).isEqualTo(AlertType.WARNING);
        assertThat(message.title()).isEqualTo("Datos no válidos");
        assertThat(message.text()).isEqualTo("El campo correo es obligatorio.");
    }

    @Test
    @DisplayName("shows the business rule message as a warning")
    void businessRuleIsAWarningWithItsMessage() {
        AlertMessages.Message message = AlertMessages.of(new BusinessRuleException("Stock insuficiente de Shampoo."));

        assertThat(message.type()).isEqualTo(AlertType.WARNING);
        assertThat(message.title()).isEqualTo("No se pudo completar la acción");
        assertThat(message.text()).isEqualTo("Stock insuficiente de Shampoo.");
    }

    @Test
    @DisplayName("hides the technical detail of a data error")
    void dataErrorHidesTheDetail() {
        AlertMessages.Message message = AlertMessages.of(new DataAccessException("java.io.IOException: C:\\data\\x.txt"));

        assertThat(message.type()).isEqualTo(AlertType.ERROR);
        assertThat(message.text()).doesNotContain("IOException", "C:\\");
        assertThat(message.text()).contains("carpeta de datos");
    }

    @Test
    @DisplayName("never shows an unexpected exception as it is")
    void unexpectedErrorIsGeneric() {
        AlertMessages.Message message = AlertMessages.of(new IllegalStateException("boom at Foo.java:12"));

        assertThat(message.type()).isEqualTo(AlertType.ERROR);
        assertThat(message.title()).isEqualTo("Error inesperado");
        assertThat(message.text()).doesNotContain("boom", "Foo.java");
    }

    @Test
    @DisplayName("rejects a missing exception")
    void rejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> AlertMessages.of(null));
    }
}
