package com.maisonglow.view;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.ValidationException;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("FieldValidator tests")
class FieldValidatorTest {

    private static boolean toolkit;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    /** A form with a document and an email field, like the customers screen. */
    private static final class Form {
        final TextField document = new TextField();
        final Label documentMessage = new Label();
        final TextField email = new TextField();
        final Label emailMessage = new Label();
        final FieldValidator fields = new FieldValidator();

        Form() {
            fields.field(document, documentMessage, "documento");
            fields.field(email, emailMessage, "correo");
        }
    }

    @Test
    @DisplayName("starts with every message hidden")
    void startsClean() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");

        Form form = FxTest.onFxThread(Form::new);

        assertThat(form.documentMessage.isVisible()).isFalse();
        assertThat(form.documentMessage.isManaged()).isFalse();
        assertThat(form.document.getStyleClass()).doesNotContain("field-error");
    }

    @Test
    @DisplayName("marks the field of a business rule error and writes the message under it")
    void marksTheMatchingField() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        Form form = FxTest.onFxThread(Form::new);

        boolean marked = FxTest.onFxThread(() ->
            form.fields.show(new BusinessRuleException("Ya existe un cliente con el documento 1065123456.")));

        assertThat(marked).isTrue();
        assertThat(form.document.getStyleClass()).contains("field-error");
        assertThat(form.documentMessage.getText()).isEqualTo("Ya existe un cliente con el documento 1065123456.");
        assertThat(form.documentMessage.isVisible()).isTrue();
        assertThat(form.email.getStyleClass()).doesNotContain("field-error");
    }

    @Test
    @DisplayName("matches the words without caring for accents or case")
    void ignoresAccentsAndCase() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        TextField phone = FxTest.onFxThread(TextField::new);
        Label message = FxTest.onFxThread(Label::new);
        FieldValidator fields = new FieldValidator();
        FxTest.onFxThread(() -> {
            fields.field(phone, message, "teléfono");
            return null;
        });

        boolean marked = FxTest.onFxThread(() -> fields.show(new ValidationException("El TELEFONO debe tener entre 7 y 12 dígitos.")));

        assertThat(marked).isTrue();
        assertThat(phone.getStyleClass()).contains("field-error");
    }

    @Test
    @DisplayName("returns false when no field matches, so the caller can use a dialog")
    void returnsFalseWithoutAMatch() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        Form form = FxTest.onFxThread(Form::new);

        boolean marked = FxTest.onFxThread(() -> form.fields.show(new BusinessRuleException("Stock insuficiente de Shampoo.")));

        assertThat(marked).isFalse();
        assertThat(form.document.getStyleClass()).doesNotContain("field-error");
        assertThat(form.email.getStyleClass()).doesNotContain("field-error");
    }

    @Test
    @DisplayName("moves the mark when a new error is about another field")
    void movesTheMark() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        Form form = FxTest.onFxThread(Form::new);

        FxTest.onFxThread(() -> form.fields.show(new ValidationException("El campo documento es obligatorio.")));
        FxTest.onFxThread(() -> form.fields.show(new ValidationException("El correo electrónico no tiene un formato válido.")));

        assertThat(form.document.getStyleClass()).doesNotContain("field-error");
        assertThat(form.email.getStyleClass()).contains("field-error");
    }

    @Test
    @DisplayName("clears the error when the user edits the field")
    void clearsWhenTheUserTypes() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        Form form = FxTest.onFxThread(Form::new);
        FxTest.onFxThread(() -> form.fields.show(new ValidationException("El campo documento es obligatorio.")));

        FxTest.onFxThread(() -> {
            form.document.setText("1065123456");
            return null;
        });

        assertThat(form.document.getStyleClass()).doesNotContain("field-error");
        assertThat(form.documentMessage.isVisible()).isFalse();
    }

    @Test
    @DisplayName("works with a date picker")
    void worksWithADatePicker() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        DatePicker date = FxTest.onFxThread(DatePicker::new);
        Label message = FxTest.onFxThread(Label::new);
        FieldValidator fields = new FieldValidator();
        FxTest.onFxThread(() -> {
            fields.field(date, message, "fecha");
            return fields.show(new ValidationException("El campo fecha es obligatorio."));
        });
        assertThat(date.getStyleClass()).contains("field-error");

        FxTest.onFxThread(() -> {
            date.setValue(LocalDate.of(2026, 11, 13));
            return null;
        });

        assertThat(date.getStyleClass()).doesNotContain("field-error");
    }
}
