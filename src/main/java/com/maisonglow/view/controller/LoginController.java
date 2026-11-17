package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.view.FieldValidator;
import com.maisonglow.view.access.AccessGateway;
import com.maisonglow.view.access.AccessSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/**
 * Controller of {@code LoginView.fxml}: signs a person in. An empty field is rejected under that field; wrong
 * credentials show one message above the button that does not say which of the two was wrong. While the answer
 * comes back the button and the fields are off and a small indicator spins, so a second click cannot send it twice.
 * The window that owns this screen decides what happens next through {@link #onSignedIn} and {@link #onRegister}.
 */
public final class LoginController {

    private final AccessGateway access;
    private final FieldValidator fields = new FieldValidator();
    private Executor executor = ForkJoinPool.commonPool();
    private Consumer<AccessSession> signedIn = session -> { };
    private Runnable register = () -> { };

    @FXML
    private TextField usernameField;
    @FXML
    private Label usernameMessage;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label passwordMessage;
    @FXML
    private Label formMessage;
    @FXML
    private Button loginButton;
    @FXML
    private Button registerLink;
    @FXML
    private ProgressIndicator waiting;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public LoginController(AppContext context) {
        this.access = Objects.requireNonNull(context, "context").access();
    }

    /**
     * Sets what happens when the person signs in.
     *
     * @param action receives the open session
     */
    public void onSignedIn(Consumer<AccessSession> action) {
        this.signedIn = Objects.requireNonNull(action, "action");
    }

    /**
     * Sets what happens when the person asks to create an account.
     *
     * @param action opens the sign-up screen
     */
    public void onRegister(Runnable action) {
        this.register = Objects.requireNonNull(action, "action");
    }

    /**
     * Replaces where the sign-in is asked, which the tests use to answer on the spot.
     *
     * @param newExecutor runs the call to the service
     */
    public void useExecutor(Executor newExecutor) {
        this.executor = Objects.requireNonNull(newExecutor, "newExecutor");
    }

    @FXML
    private void initialize() {
        fields.field(usernameField, usernameMessage, "usuario");
        fields.field(passwordField, passwordMessage, "contraseña");
        hideFormMessage();
        setWaiting(false);
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(event -> login());
        registerLink.setOnAction(event -> register.run());
        usernameField.textProperty().addListener((observable, before, now) -> hideFormMessage());
        passwordField.textProperty().addListener((observable, before, now) -> hideFormMessage());
    }

    private void login() {
        hideFormMessage();
        fields.clear();
        String username = usernameField.getText();
        String password = passwordField.getText();
        setWaiting(true);
        CompletableFuture.supplyAsync(() -> {
            try {
                return access.login(username, password);
            } catch (ValidationException | BusinessRuleException e) {
                throw new CompletionException(e);
            }
        }, executor).whenComplete((session, error) -> Platform.runLater(() -> {
            setWaiting(false);
            if (error == null) {
                passwordField.clear();
                signedIn.accept(session);
            } else {
                showError(error.getCause() instanceof Exception cause ? cause : new IllegalStateException(error));
            }
        }));
    }

    private void showError(Exception error) {
        if (error instanceof ValidationException && fields.show(error)) {
            return;
        }
        passwordField.clear();
        formMessage.setText(error instanceof BusinessRuleException ? error.getMessage()
            : "No se pudo iniciar sesión. Inténtalo de nuevo.");
        formMessage.setVisible(true);
        formMessage.setManaged(true);
    }

    private void hideFormMessage() {
        formMessage.setVisible(false);
        formMessage.setManaged(false);
    }

    private void setWaiting(boolean value) {
        waiting.setVisible(value);
        waiting.setManaged(value);
        loginButton.setDisable(value);
        usernameField.setDisable(value);
        passwordField.setDisable(value);
    }
}
