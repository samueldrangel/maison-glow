package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Customer;
import com.maisonglow.service.CustomerPortalService;
import com.maisonglow.util.Initials;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.BackgroundTask;
import com.maisonglow.view.ChangePasswordDialog;
import com.maisonglow.view.FieldValidator;
import com.maisonglow.view.ViewLifecycle;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Controller of {@code ProfileView.fxml}: "Mi perfil" of a customer (task N-R01). They see and change their name, phone and
 * email; the document is shown but cannot be edited, and neither can the role, the linked identity or the no-shows: the
 * service ({@link CustomerPortalService#updateContact}) only takes those three values and works on the customer of the
 * session. Each error is shown under its field. Changing the password opens the same window as the side menu, and the
 * window of the application then closes the session, because the service ends it. The calls run in the background and an
 * answer that arrives after the session closed is ignored.
 */
public final class ProfileController implements ViewLifecycle {

    private final CustomerPortalService portal;
    private final AppContext context;
    private final FieldValidator fields = new FieldValidator();
    private Executor executor = ForkJoinPool.commonPool();
    private AlertPresenter alerts;
    private Runnable passwordOpener;
    private Customer loaded;
    private boolean disposed;

    @FXML
    private Label initialsLabel;
    @FXML
    private Label nameTitle;
    @FXML
    private Label documentLabel;
    @FXML
    private TextField nameField;
    @FXML
    private Label nameMessage;
    @FXML
    private TextField phoneField;
    @FXML
    private Label phoneMessage;
    @FXML
    private TextField emailField;
    @FXML
    private Label emailMessage;
    @FXML
    private Button saveButton;
    @FXML
    private Button discardButton;
    @FXML
    private Button passwordButton;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public ProfileController(AppContext context) {
        this.context = Objects.requireNonNull(context, "context");
        this.portal = context.portal();
        this.passwordOpener = () -> ChangePasswordDialog.show(
            passwordButton.getScene() == null ? null : passwordButton.getScene().getWindow(), context);
    }

    /**
     * Replaces where the calls run, which the tests use to hold an answer back.
     *
     * @param newExecutor runs the call
     */
    public void useExecutor(Executor newExecutor) {
        this.executor = Objects.requireNonNull(newExecutor, "newExecutor");
    }

    /**
     * Replaces the presenter of dialogs, which the tests use so that no real window is opened.
     *
     * @param presenter the presenter to use from now on
     */
    public void useAlerts(AlertPresenter presenter) {
        this.alerts = Objects.requireNonNull(presenter, "presenter");
    }

    /**
     * Replaces what the password button opens, which the tests use so that no real window is opened.
     *
     * @param opener opens the change-password window
     */
    public void usePasswordOpener(Runnable opener) {
        this.passwordOpener = Objects.requireNonNull(opener, "opener");
    }

    @FXML
    private void initialize() {
        alerts = new AlertPresenter(saveButton);
        fields.field(nameField, nameMessage, "nombre");
        fields.field(phoneField, phoneMessage, "teléfono", "telefono");
        fields.field(emailField, emailMessage, "correo");
        saveButton.setDisable(true);
        saveButton.setOnAction(event -> save());
        discardButton.setOnAction(event -> fill(loaded));
        passwordButton.setOnAction(event -> passwordOpener.run());
        BackgroundTask.run(executor, portal::profile, this::fill, error -> alerts.showError(error), () -> !disposed);
    }

    /** Puts a customer's data in the card and the fields; with none the form stays off. */
    private void fill(Customer customer) {
        loaded = customer;
        fields.clear();
        if (customer == null) {
            return;
        }
        initialsLabel.setText(Initials.of(customer.getName()));
        nameTitle.setText(customer.getName());
        documentLabel.setText("Documento " + customer.getDocument());
        nameField.setText(customer.getName());
        phoneField.setText(customer.getPhone());
        emailField.setText(customer.getEmail());
        saveButton.setDisable(false);
    }

    private void save() {
        fields.clear();
        String name = nameField.getText();
        String phone = phoneField.getText();
        String email = emailField.getText();
        saveButton.setDisable(true);
        BackgroundTask.run(executor, () -> portal.updateContact(name, phone, email), saved -> {
            fill(saved);
            alerts.toast("Datos actualizados.");
        }, error -> {
            saveButton.setDisable(false);
            if (!((error instanceof ValidationException || error instanceof BusinessRuleException) && fields.show(error))) {
                alerts.showError(error);
            }
        }, () -> !disposed);
    }

    /** Closing the session: an answer that arrives later is ignored and the data of the customer is dropped. */
    @Override
    public void dispose() {
        disposed = true;
        loaded = null;
        nameField.clear();
        phoneField.clear();
        emailField.clear();
    }

    /** @return the customer shown, for the tests */
    Customer shown() {
        return loaded;
    }
}
