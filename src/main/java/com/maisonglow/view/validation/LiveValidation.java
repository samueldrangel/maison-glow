package com.maisonglow.view.validation;

import com.maisonglow.view.Animations;
import com.maisonglow.view.Icons;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Validation of the fields of one form, shown under each field while the person fills it in. A field is checked when it loses
 * the focus and, from then on, every time it changes, so the message appears when the person leaves a field with a mistake and
 * goes away as soon as it is fixed; a field the person has not touched yet shows nothing. Pressing the button of the form
 * calls {@link #validateAll}, which checks every field, shows every message, takes the focus to the first field with a mistake
 * and says whether the form can be sent. Each rule is a {@link Supplier} that reads the current value of the field, so a rule
 * can depend on another field (the confirmation of a password); when any field changes, the fields that were already touched
 * are checked again.
 *
 * <p>The mark is the same one {@link com.maisonglow.view.FieldValidator} uses for the answers of the services (a red border and
 * the message with its icon under the field), so both can share the same label. Every method is meant to be called from the
 * JavaFX thread.
 */
public final class LiveValidation {

    private static final String ERROR_STYLE = "field-error";

    private final List<Field> fields = new ArrayList<>();

    /** One field with its message, its rule and whether the person already touched it. */
    private final class Field {
        private final Node control;
        private final Label message;
        private final Supplier<String> rule;
        private boolean touched;

        Field(Node control, Label message, Supplier<String> rule) {
            this.control = control;
            this.message = message;
            this.rule = rule;
        }

        /** Checks the field and shows or hides its message; @return true if it is valid */
        boolean check(boolean animate) {
            String problem = rule.get();
            if (problem == null) {
                clear();
                return true;
            }
            if (!control.getStyleClass().contains(ERROR_STYLE)) {
                control.getStyleClass().add(ERROR_STYLE);
            }
            message.setWrapText(true);
            message.setText(problem);
            message.setGraphic(Icons.small(Icons.Kind.ALERT_CIRCLE, 15));
            message.setVisible(true);
            message.setManaged(true);
            if (animate) {
                Animations.shake(control);
            }
            return false;
        }

        void clear() {
            control.getStyleClass().remove(ERROR_STYLE);
            message.setText("");
            message.setGraphic(null);
            message.setVisible(false);
            message.setManaged(false);
        }
    }

    /**
     * Watches a text field.
     *
     * @param control the field the person types in
     * @param message the label under it
     * @param rule    reads what is in the field (and in others, if the rule needs them) and answers null if fine, or the sentence
     */
    public void text(TextInputControl control, Label message, Supplier<String> rule) {
        Field field = register(control, message, rule);
        control.focusedProperty().addListener((observable, was, focused) -> {
            if (!focused) {
                field.touched = true;
                field.check(false);
            }
        });
        control.textProperty().addListener((observable, before, now) -> {
            if (field.touched) {
                field.check(false);
            }
            recheckTouched(field);
        });
    }

    /**
     * Watches a field where something is chosen (a list, a date).
     *
     * @param control the field
     * @param value   the value that changes when the person chooses
     * @param message the label under it
     * @param rule    answers null if what is chosen is fine, or the sentence
     */
    public void choice(Node control, ObservableValue<?> value, Label message, Supplier<String> rule) {
        Field field = register(control, message, rule);
        value.addListener((observable, before, now) -> {
            if (field.touched) {
                field.check(false);
            }
            recheckTouched(field);
        });
    }

    /**
     * Checks every field, shows every message and takes the focus to the first field with a mistake.
     *
     * @return true if every field is valid and the form can be sent
     */
    public boolean validateAll() {
        Field first = null;
        for (Field field : fields) {
            field.touched = true;
            if (!field.check(true) && first == null) {
                first = field;
            }
        }
        if (first != null) {
            first.control.requestFocus();
        }
        return first == null;
    }

    /** @return true if every field is valid; nothing is shown */
    public boolean isValid() {
        return fields.stream().allMatch(field -> field.rule.get() == null);
    }

    /** Hides every message and forgets what was touched, for a form that starts again after being sent. */
    public void clear() {
        fields.forEach(field -> {
            field.touched = false;
            field.clear();
        });
    }

    /**
     * Limits how much can be typed in a field, so it never holds more than the services accept.
     *
     * @param control the field
     * @param maximum the most characters it may hold
     */
    public static void limit(TextInputControl control, int maximum) {
        control.setTextFormatter(new TextFormatter<String>(change -> change.getControlNewText().length() <= maximum ? change : null));
    }

    private Field register(Node control, Label message, Supplier<String> rule) {
        Field field = new Field(Objects.requireNonNull(control, "control"), Objects.requireNonNull(message, "message"),
            Objects.requireNonNull(rule, "rule"));
        field.clear();
        fields.add(field);
        return field;
    }

    /** A rule may read another field: when one changes, the ones the person already touched are checked again. */
    private void recheckTouched(Field changed) {
        fields.stream().filter(other -> other != changed && other.touched).forEach(other -> other.check(false));
    }
}
