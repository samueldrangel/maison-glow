package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.view.FieldValidator;
import com.maisonglow.view.access.AccessGateway;
import com.maisonglow.view.validation.FieldRules;
import com.maisonglow.view.validation.LiveValidation;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Controller of {@code ChangePasswordView.fxml}: changes the password of the open session. The wrong current
 * password, a short new one or a confirmation that does not match are shown under their own field; an ended session
 * is shown above the buttons. The window that opens this screen learns of the result through {@link #onDone} (saved)
 * and {@link #onCancel}.
 */
public final class ChangePasswordController {

    private final AccessGateway access;
    private final FieldValidator fields = new FieldValidator();
    private final LiveValidation live = new LiveValidation();
    private Executor executor = ForkJoinPool.commonPool();
    private Runnable done = () -> { };
    private Runnable cancel = () -> { };

    @FXML
    private PasswordField currentField;
    @FXML
    private Label currentMessage;
    @FXML
    private PasswordField newField;
    @FXML
    private Label newMessage;
    @FXML
    private PasswordField confirmationField;
    @FXML
    private Label confirmationMessage;
    @FXML
    private Label formMessage;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;
    @FXML
    private ProgressIndicator waiting;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public ChangePasswordController(AppContext context) {
        this.access = Objects.requireNonNull(context, "context").access();
    }

    /**
     * Sets what happens when the password was changed.
     *
     * @param action closes the window
     */
    public void onDone(Runnable action) {
        this.done = Objects.requireNonNull(action, "action");
    }

    /**
     * Sets what happens when the person gives up.
     *
     * @param action closes the window
     */
    public void onCancel(Runnable action) {
        this.cancel = Objects.requireNonNull(action, "action");
    }

    /**
     * Replaces where the change is asked, which the tests use to answer on the spot.
     *
     * @param newExecutor runs the call to the service
     */
    public void useExecutor(Executor newExecutor) {
        this.executor = Objects.requireNonNull(newExecutor, "newExecutor");
    }

    @FXML
    private void initialize() {
        // The most specific words go first: "contraseña nueva" and "actual" also contain "contraseña".
        fields.field(confirmationField, confirmationMessage, "confirmación", "confirmacion");
        fields.field(currentField, currentMessage, "actual");
        fields.field(newField, newMessage, "nueva", "contraseña");
        live.text(currentField, currentMessage, () -> FieldRules.currentPassword(currentField.getText()));
        live.text(newField, newMessage, () -> {
            String problem = FieldRules.newPassword(newField.getText());
            return problem != null ? problem : FieldRules.differentFromCurrent(currentField.getText(), newField.getText());
        });
        live.text(confirmationField, confirmationMessage,
            () -> FieldRules.confirmation(newField.getText(), confirmationField.getText()));
        LiveValidation.limit(newField, FieldRules.PASSWORD_MAX);
        LiveValidation.limit(confirmationField, FieldRules.PASSWORD_MAX);
        formMessage.setVisible(false);
        formMessage.setManaged(false);
        setWaiting(false);
        saveButton.setDefaultButton(true);
        saveButton.setOnAction(event -> save());
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(event -> cancel.run());
    }

    private void save() {
        formMessage.setVisible(false);
        formMessage.setManaged(false);
        fields.clear();
        if (!live.validateAll()) {
            return;
        }
        String current = currentField.getText();
        String replacement = newField.getText();
        String confirmation = confirmationField.getText();
        setWaiting(true);
        CompletableFuture.runAsync(() -> {
            try {
                access.changePassword(current, replacement, confirmation);
            } catch (ValidationException | BusinessRuleException | DataAccessException e) {
                throw new CompletionException(e);
            }
        }, executor).whenComplete((nothing, error) -> Platform.runLater(() -> {
            setWaiting(false);
            if (error == null) {
                clear();
                done.run();
            } else {
                showError(error.getCause() instanceof Exception cause ? cause : new IllegalStateException(error));
            }
        }));
    }

    private void showError(Exception error) {
        if ((error instanceof ValidationException || error instanceof BusinessRuleException) && fields.show(error)) {
            return;
        }
        formMessage.setText(error instanceof BusinessRuleException ? error.getMessage()
            : "No se pudo cambiar la contraseña. Inténtalo de nuevo.");
        formMessage.setVisible(true);
        formMessage.setManaged(true);
    }

    private void clear() {
        List.of(currentField, newField, confirmationField).forEach(PasswordField::clear);
    }

    private void setWaiting(boolean value) {
        waiting.setVisible(value);
        waiting.setManaged(value);
        saveButton.setDisable(value);
        List.of(currentField, newField, confirmationField).forEach(field -> field.setDisable(value));
    }
}
