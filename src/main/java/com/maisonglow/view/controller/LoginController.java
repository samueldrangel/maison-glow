package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.service.AuthenticationService;
import com.maisonglow.view.FieldValidator;
import com.maisonglow.view.access.AccessGateway;
import com.maisonglow.view.access.AccessSession;
import com.maisonglow.view.validation.FieldRules;
import com.maisonglow.view.validation.LiveValidation;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.util.Arrays;
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
    private final AuthenticationService auth;
    private final FieldValidator fields = new FieldValidator();
    private final LiveValidation live = new LiveValidation();
    private Executor executor = ForkJoinPool.commonPool();
    private Consumer<AccessSession> signedIn = session -> { };
    private Runnable register = () -> { };
    private Runnable administratorCreated = () -> { };
    private boolean administratorSetup;

    @FXML
    private Label title;
    @FXML
    private Label hint;

    @FXML
    private TextField usernameField;
    @FXML
    private Label usernameMessage;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label passwordMessage;
    @FXML
    private Label confirmationLabel;
    @FXML
    private PasswordField confirmationField;
    @FXML
    private Label confirmationMessage;
    @FXML
    private Label formMessage;
    @FXML
    private Button loginButton;
    @FXML
    private Button registerLink;
    @FXML
    private HBox registerPrompt;
    @FXML
    private Button setupLink;
    @FXML
    private ProgressIndicator waiting;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public LoginController(AppContext context) {
        this.access = Objects.requireNonNull(context, "context").access();
        this.auth = context.auth();
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

    public void offerAdministratorSetup(boolean showSetup, Runnable onCreated) {
        administratorCreated = Objects.requireNonNull(onCreated, "onCreated");
        setupLink.setVisible(true);
        setupLink.setManaged(true);
        setupLink.setOnAction(event -> setAdministratorSetup(!administratorSetup));
        setAdministratorSetup(showSetup);
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
        fields.field(confirmationField, confirmationMessage, "confirmación");
        live.text(usernameField, usernameMessage, () -> administratorSetup
            ? FieldRules.newUsername(usernameField.getText()) : FieldRules.loginUsername(usernameField.getText()));
        live.text(passwordField, passwordMessage, () -> administratorSetup
            ? FieldRules.newPassword(passwordField.getText()) : FieldRules.loginPassword(passwordField.getText()));
        live.text(confirmationField, confirmationMessage, () -> administratorSetup
            ? FieldRules.confirmation(passwordField.getText(), confirmationField.getText()) : null);
        hideFormMessage();
        setWaiting(false);
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(event -> submit());
        registerLink.setOnAction(event -> register.run());
        usernameField.textProperty().addListener((observable, before, now) -> hideFormMessage());
        passwordField.textProperty().addListener((observable, before, now) -> hideFormMessage());
    }

    private void setAdministratorSetup(boolean value) {
        administratorSetup = value;
        passwordField.clear();
        confirmationField.clear();
        live.clear();
        hideFormMessage();
        title.setText(value ? "Configura la administración" : "Bienvenida de nuevo");
        hint.setText(value ? "Crea la cuenta administradora de esta instalación. Solo se configura una vez."
            : "Inicia sesión para continuar.");
        loginButton.setText(value ? "Crear administrador" : "Iniciar sesión");
        setupLink.setText(value ? "Ir a inicio de sesión" : "Configurar administrador");
        confirmationLabel.setVisible(value);
        confirmationLabel.setManaged(value);
        confirmationField.setVisible(value);
        confirmationField.setManaged(value);
        registerPrompt.setVisible(!value);
        registerPrompt.setManaged(!value);
    }

    private void submit() {
        hideFormMessage();
        fields.clear();
        if (!live.validateAll()) {
            return;
        }
        String username = usernameField.getText();
        String password = passwordField.getText();
        boolean setup = administratorSetup;
        setWaiting(true);
        CompletableFuture.supplyAsync(() -> {
            try {
                if (setup) {
                    char[] secret = password.toCharArray();
                    try {
                        auth.createFirstAdmin(username, secret);
                    } finally {
                        Arrays.fill(secret, '\0');
                    }
                    return null;
                }
                return access.login(username, password);
            } catch (ValidationException | BusinessRuleException | DataAccessException e) {
                throw new CompletionException(e);
            }
        }, executor).whenComplete((session, error) -> Platform.runLater(() -> {
            setWaiting(false);
            if (error == null) {
                passwordField.clear();
                confirmationField.clear();
                if (setup) {
                    administratorCreated.run();
                } else {
                    signedIn.accept(session);
                }
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
        confirmationField.clear();
        live.clear();
        String fallback = administratorSetup ? "No se pudo crear el administrador. Inténtalo de nuevo."
            : "No se pudo iniciar sesión. Inténtalo de nuevo.";
        formMessage.setText(error instanceof BusinessRuleException ? error.getMessage() : fallback);
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
        confirmationField.setDisable(value);
        setupLink.setDisable(value);
        registerLink.setDisable(value);
    }
}
