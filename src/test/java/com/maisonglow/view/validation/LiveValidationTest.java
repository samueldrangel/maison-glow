package com.maisonglow.view.validation;

import com.maisonglow.view.FxTest;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Live validation of a form")
class LiveValidationTest {

    private static boolean toolkit;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @Test
    @DisplayName("an untouched field shows nothing; pressing the button shows every message and the first mistake is focused")
    void validateAllShowsEverything() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit not available");
        FxTest.onFxThread(() -> {
            TextField name = new TextField();
            TextField mail = new TextField();
            Label nameMessage = new Label();
            Label mailMessage = new Label();
            LiveValidation live = new LiveValidation();
            live.text(name, nameMessage, () -> FieldRules.fullName(name.getText()));
            live.text(mail, mailMessage, () -> FieldRules.email(mail.getText()));

            assertThat(nameMessage.isVisible()).isFalse();
            assertThat(live.isValid()).isFalse();
            assertThat(live.validateAll()).isFalse();
            assertThat(nameMessage.getText()).isEqualTo("Escribe tu nombre completo.");
            assertThat(mailMessage.getText()).isEqualTo("Escribe tu correo.");
            assertThat(name.getStyleClass()).contains("field-error");

            name.setText("Ana García");
            assertThat(nameMessage.isVisible()).isFalse();
            assertThat(name.getStyleClass()).doesNotContain("field-error");
            mail.setText("ana@correo.com");
            assertThat(live.validateAll()).isTrue();
            assertThat(live.isValid()).isTrue();

            live.clear();
            mail.setText("mal");
            assertThat(mailMessage.isVisible()).isFalse();
            return null;
        });
    }

    @Test
    @DisplayName("a rule that reads another field is checked again when that field changes")
    void dependentFields() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit not available");
        FxTest.onFxThread(() -> {
            TextField password = new TextField();
            TextField confirmation = new TextField();
            Label message = new Label();
            LiveValidation live = new LiveValidation();
            live.text(password, new Label(), () -> FieldRules.newPassword(password.getText()));
            live.text(confirmation, message, () -> FieldRules.confirmation(password.getText(), confirmation.getText()));
            password.setText("clave-segura-1");
            live.validateAll();
            confirmation.setText("clave-segura-1");
            assertThat(message.isVisible()).isFalse();
            password.setText("clave-segura-2");
            assertThat(message.getText()).contains("no coincide");
            return null;
        });
    }

    @Test
    @DisplayName("the limit stops typing and pasting past the maximum")
    void limit() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit not available");
        FxTest.onFxThread(() -> {
            TextField field = new TextField();
            LiveValidation.limit(field, 5);
            field.setText("12345");
            field.setText("123456789");
            assertThat(field.getText()).isEqualTo("12345");
            field.replaceText(0, 5, "abc");
            assertThat(field.getText()).isEqualTo("abc");
            return null;
        });
    }
}
