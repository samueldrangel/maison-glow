package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.view.FieldValidator;
import com.maisonglow.view.access.AccessGateway;
import com.maisonglow.view.access.AccessSession;
import com.maisonglow.view.access.RegistrationData;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/**
 * Controller of {@code RegisterView.fxml}: creates a customer account. There is no role on this screen, because a
 * public sign-up never creates anything but customers. Each error appears under the field it is about; a rule broken
 * by the account service (a username or a document already in use) is placed under its field too. While the service
 * answers, the form is off so a second click cannot create the account twice.
 */
public final class RegisterController {

    private final AccessGateway access;
    private final FieldValidator fields = new FieldValidator();
    private Executor executor = ForkJoinPool.commonPool();
    private Consumer<AccessSession> signedIn = session -> { };
    private Runnable login = () -> { };

    @FXML
    private TextField nameField;
    @FXML
    private Label nameMessage;
    @FXML
    private TextField documentField;
    @FXML
    private Label documentMessage;
    @FXML
    private TextField phoneField;
    @FXML
    private Label phoneMessage;
    @FXML
    private TextField emailField;
    @FXML
    private Label emailMessage;
    @FXML
    private TextField usernameField;
    @FXML
    private Label usernameMessage;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label passwordMessage;
    @FXML
    private PasswordField confirmationField;
    @FXML
    private Label confirmationMessage;
    @FXML
    private Label formMessage;
    @FXML
    private Button registerButton;
    @FXML
    private Button loginLink;
    @FXML
    private ProgressIndicator waiting;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public RegisterController(AppContext context) {
        this.access = Objects.requireNonNull(context, "context").access();
    }

    /**
     * Sets what happens when the account is created (the session is already open).
     *
     * @param action receives the open session
     */
    public void onSignedIn(Consumer<AccessSession> action) {
        this.signedIn = Objects.requireNonNull(action, "action");
    }

    /**
     * Sets what happens when the person already has an account.
     *
     * @param action opens the sign-in screen
     */
    public void onLogin(Runnable action) {
        this.login = Objects.requireNonNull(action, "action");
    }

    /**
     * Replaces where the sign-up is asked, which the tests use to answer on the spot.
     *
     * @param newExecutor runs the call to the service
     */
    public void useExecutor(Executor newExecutor) {
        this.executor = Objects.requireNonNull(newExecutor, "newExecutor");
    }

    @FXML
    private void initialize() {
        // The confirmation is registered first: its message also says "contraseña".
        fields.field(confirmationField, confirmationMessage, "confirmación", "confirmacion");
        fields.field(passwordField, passwordMessage, "contraseña");
        fields.field(nameField, nameMessage, "nombre");
        fields.field(documentField, documentMessage, "documento");
        fields.field(phoneField, phoneMessage, "teléfono");
        fields.field(emailField, emailMessage, "correo");
        fields.field(usernameField, usernameMessage, "usuario");
        hideFormMessage();
        setWaiting(false);
        registerButton.setDefaultButton(true);
        registerButton.setOnAction(event -> register());
        loginLink.setOnAction(event -> login.run());
    }

    private void register() {
        hideFormMessage();
        fields.clear();
        RegistrationData data = new RegistrationData(nameField.getText(), documentField.getText(), phoneField.getText(),
            emailField.getText(), usernameField.getText(), passwordField.getText(), confirmationField.getText());
        setWaiting(true);
        CompletableFuture.supplyAsync(() -> {
            try {
                return access.register(data);
            } catch (ValidationException | BusinessRuleException | DataAccessException e) {
                throw new CompletionException(e);
            }
        }, executor).whenComplete((session, error) -> Platform.runLater(() -> {
            setWaiting(false);
            if (error == null) {
                clearPasswords();
                signedIn.accept(session);
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
            : "No se pudo crear la cuenta. Inténtalo de nuevo.");
        formMessage.setVisible(true);
        formMessage.setManaged(true);
    }

    private void clearPasswords() {
        passwordField.clear();
        confirmationField.clear();
    }

    private void hideFormMessage() {
        formMessage.setVisible(false);
        formMessage.setManaged(false);
    }

    private void setWaiting(boolean value) {
        waiting.setVisible(value);
        waiting.setManaged(value);
        registerButton.setDisable(value);
        List.of(nameField, documentField, phoneField, emailField, usernameField, passwordField, confirmationField)
            .forEach(field -> field.setDisable(value));
    }
}
