package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.UserAccount;
import com.maisonglow.model.UserRole;
import com.maisonglow.service.AuthenticationService;
import com.maisonglow.service.CustomerService;
import com.maisonglow.util.TextSearch;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.BackgroundTask;
import com.maisonglow.view.LoadingOverlay;
import com.maisonglow.view.ViewLifecycle;
import com.maisonglow.view.access.AccessSession;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Controller of {@code AccountsView.fxml}: who can sign in, for the administrator (task I-R05). It lists the accounts
 * of the service ({@link AuthenticationService#listAccounts()}, which never carries credentials) with the name of the
 * customer each one belongs to, and turns an account on or off. The administrator cannot turn off the account they are
 * using; the service also refuses to turn off the last active administrator. The calls run in the background with a
 * loading veil, and an answer that arrives after the screen was discarded is ignored.
 */
public final class AccountsController implements ViewLifecycle {

    /** A row: the account and the name of its customer, empty for an administrator. */
    record Row(UserAccount account, String customerName) { }

    /** Which accounts the chips show. */
    private enum Group { ALL, ACTIVE, OFF }

    private final AuthenticationService auth;
    private final CustomerService customers;
    private final AppContext context;
    private final ObservableList<Row> all = FXCollections.observableArrayList();
    private final FilteredList<Row> shown = new FilteredList<>(all);
    private Executor executor = ForkJoinPool.commonPool();
    private Group group = Group.ALL;
    private String text = "";
    private AlertPresenter alerts;
    private LoadingOverlay loading;
    private boolean disposed;

    @FXML
    private ToggleGroup filters;
    @FXML
    private TextField searchField;
    @FXML
    private StackPane tableHost;
    @FXML
    private TableView<Row> accountsTable;
    @FXML
    private TableColumn<Row, String> usernameColumn;
    @FXML
    private TableColumn<Row, UserRole> roleColumn;
    @FXML
    private TableColumn<Row, String> customerColumn;
    @FXML
    private TableColumn<Row, Row> stateColumn;
    @FXML
    private TableColumn<Row, Row> actionColumn;
    @FXML
    private Label emptyLabel;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public AccountsController(AppContext context) {
        this.context = Objects.requireNonNull(context, "context");
        this.auth = context.auth();
        this.customers = context.customers();
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

    @FXML
    private void initialize() {
        alerts = new AlertPresenter(accountsTable);
        loading = LoadingOverlay.over(tableHost);
        usernameColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().account().username()));
        roleColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().account().role()));
        customerColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().customerName()));
        stateColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        actionColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        roleColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(UserRole role, boolean empty) {
                super.updateItem(role, empty);
                setText(empty || role == null ? null : role == UserRole.ADMIN ? "Administración" : "Cliente");
            }
        });
        stateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Row row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setGraphic(null);
                    return;
                }
                Label chip = new Label(row.account().active() ? "Activa" : "Desactivada");
                chip.getStyleClass().add(row.account().active() ? "chip-success" : "chip-neutral");
                setGraphic(chip);
            }
        });
        actionColumn.setCellFactory(column -> new ActionCell());
        accountsTable.setItems(shown);
        List<Toggle> chips = filters.getToggles();
        for (int index = 0; index < chips.size(); index++) {
            chips.get(index).setUserData(Group.values()[index]);
        }
        filters.selectedToggleProperty().addListener((observable, before, chip) -> {
            if (chip == null && before != null) {
                before.setSelected(true);
            } else if (chip != null) {
                group = (Group) chip.getUserData();
                apply();
            }
        });
        searchField.textProperty().addListener((observable, before, now) -> {
            text = now == null ? "" : now.trim();
            apply();
        });
        refresh();
    }

    private void apply() {
        shown.setPredicate(row -> switch (group) {
            case ALL -> true;
            case ACTIVE -> row.account().active();
            case OFF -> !row.account().active();
        } && (text.isEmpty() || TextSearch.contains(row.account().username(), text) || TextSearch.contains(row.customerName(), text)));
        emptyLabel.setText(text.isEmpty() ? "No hay cuentas en esta lista." : "Ninguna cuenta coincide con la búsqueda.");
    }

    /** Reads the accounts again. */
    void refresh() {
        loading.show("Cargando cuentas…");
        BackgroundTask.run(executor, this::readRows, rows -> {
            loading.hide();
            all.setAll(rows);
        }, this::fail, () -> !disposed);
    }

    private List<Row> readRows() throws BusinessRuleException, DataAccessException {
        return auth.listAccounts().stream().map(account -> new Row(account, customerNameOf(account))).toList();
    }

    private String customerNameOf(UserAccount account) {
        if (account.customerId() == null) {
            return "";
        }
        try {
            return customers.findById(account.customerId()).getName();
        } catch (BusinessRuleException | DataAccessException e) {
            return "Cliente n.º " + account.customerId();
        }
    }

    private boolean isMine(UserAccount account) {
        return context.access().current().map(AccessSession::username).filter(account.username()::equals).isPresent();
    }

    private void change(Row row) {
        UserAccount account = row.account();
        boolean turnOn = !account.active();
        if (!turnOn && !alerts.confirm("¿Desactivar la cuenta «" + account.username() + "»? Se cerrarán sus sesiones y no podrá "
            + "entrar hasta que la actives de nuevo.")) {
            return;
        }
        loading.show(turnOn ? "Activando la cuenta…" : "Desactivando la cuenta…");
        BackgroundTask.run(executor, () -> {
            auth.setActive(account.id(), turnOn);
            return account.username();
        }, name -> {
            loading.hide();
            alerts.toast(turnOn ? "Cuenta «" + name + "» activada." : "Cuenta «" + name + "» desactivada.");
            refresh();
        }, this::fail, () -> !disposed);
    }

    private void fail(Exception error) {
        loading.hide();
        alerts.showError(error);
    }

    /** Closing the session: an answer that arrives later is ignored and the list of accounts is dropped. */
    @Override
    public void dispose() {
        disposed = true;
        all.clear();
    }

    /** The button that turns an account on or off; it is off for the account of the person using the screen. */
    private final class ActionCell extends TableCell<Row, Row> {
        @Override
        protected void updateItem(Row row, boolean empty) {
            super.updateItem(row, empty);
            if (empty || row == null) {
                setGraphic(null);
                return;
            }
            boolean active = row.account().active();
            Button button = new Button(active ? "Desactivar" : "Activar");
            button.getStyleClass().addAll(active ? "button-secondary" : "button-primary", "button-compact");
            if (isMine(row.account())) {
                button.setDisable(true);
                button.setTooltip(new Tooltip("No puedes desactivar la cuenta con la que estás usando la aplicación."));
            }
            button.setOnAction(event -> change(row));
            setGraphic(button);
        }
    }
}
