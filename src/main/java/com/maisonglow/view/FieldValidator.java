package com.maisonglow.view;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Shows a service error next to the field it is about: the field gets a red
 * border and its message label shows the text of the exception. The services
 * do not say which field failed, so every field is registered with the words
 * that identify it in the messages (the messages are in Spanish, for example
 * "El campo correo es obligatorio." or "Ya existe un cliente con el documento 5.").
 *
 * <pre>{@code
 * FieldValidator fields = new FieldValidator();
 * fields.field(emailField, emailMessage, "correo");
 * fields.field(documentField, documentMessage, "documento");
 * try { service.register(data); }
 * catch (ValidationException | BusinessRuleException e) { if (!fields.show(e)) alerts.showError(e); }
 * }</pre>
 *
 * <p>The error goes away by itself when the user edits the field again.
 */
public final class FieldValidator {

    private static final String ERROR_STYLE = "field-error";

    private final List<Entry> entries = new ArrayList<>();

    /**
     * Registers a text field.
     *
     * @param control the field the user types in
     * @param message the label under it, where the error is written
     * @param keywords words that identify the field in an error message (accents and case do not matter)
     */
    public void field(TextInputControl control, Labeled message, String... keywords) {
        register(control, control.textProperty(), message, keywords);
    }

    /**
     * Registers a combo box or a date picker.
     *
     * @param control the field the user picks a value in
     * @param message the label under it, where the error is written
     * @param keywords words that identify the field in an error message (accents and case do not matter)
     */
    public void field(ComboBoxBase<?> control, Labeled message, String... keywords) {
        register(control, control.valueProperty(), message, keywords);
    }

    /**
     * Marks the field that an error talks about.
     *
     * @param error the exception thrown by a service
     * @return true if a field was marked; false if no registered field matches, so the
     *         caller shows the error in a dialog instead
     */
    public boolean show(Exception error) {
        Objects.requireNonNull(error, "error");
        clear();
        String text = normalize(String.valueOf(error.getMessage()));
        for (Entry entry : entries) {
            if (entry.matches(text)) {
                entry.mark(String.valueOf(error.getMessage()));
                return true;
            }
        }
        return false;
    }

    /** Removes every mark and message. */
    public void clear() {
        entries.forEach(Entry::clear);
    }

    private void register(Node control, ObservableValue<?> value, Labeled message, String... keywords) {
        Entry entry = new Entry(control, message, keywords);
        entry.clear();
        value.addListener((observable, previous, current) -> entry.clear());
        entries.add(entry);
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /** A field, its message label and the words that identify it. */
    private static final class Entry {
        private final Node control;
        private final Labeled message;
        private final List<String> keywords = new ArrayList<>();

        Entry(Node control, Labeled message, String... words) {
            this.control = Objects.requireNonNull(control, "control");
            this.message = Objects.requireNonNull(message, "message");
            message.setWrapText(true);
            for (String word : words) {
                keywords.add(normalize(word));
            }
        }

        boolean matches(String normalizedMessage) {
            return keywords.stream().anyMatch(normalizedMessage::contains);
        }

        void mark(String text) {
            if (!control.getStyleClass().contains(ERROR_STYLE)) {
                control.getStyleClass().add(ERROR_STYLE);
            }
            message.setText(text);
            message.setGraphic(Icons.small(Icons.Kind.ALERT_CIRCLE, 15));
            message.setVisible(true);
            message.setManaged(true);
            Animations.shake(control);
        }

        void clear() {
            control.getStyleClass().remove(ERROR_STYLE);
            message.setText("");
            message.setGraphic(null);
            message.setVisible(false);
            message.setManaged(false);
        }
    }
}
