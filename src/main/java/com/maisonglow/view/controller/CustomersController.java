package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Customer;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.CustomerService;
import com.maisonglow.util.DateFormats;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FieldValidator;
import com.maisonglow.view.customer.CustomerFilter;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.util.Callback;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Function;

/**
 * Controller of {@code CustomersView.fxml}: the customers (mockup M2). It lists them, searches by name,
 * document or phone, and marks the ones who missed appointments. It only talks to
 * {@link CustomerService}, never to a DAO.
 */
public final class CustomersController {

    private final CustomerService customers;
    private final ObservableList<Customer> all = FXCollections.observableArrayList();
    private final FilteredList<Customer> shown = new FilteredList<>(all);
    private final FieldValidator formFields = new FieldValidator();
    private CustomerFilter filter;
    private Customer editing;
    private AlertPresenter alerts;

    @FXML
    private TextField searchField;
    @FXML
    private ToggleGroup groupChips;
    @FXML
    private ToggleButton allChip;
    @FXML
    private ToggleButton noShowsChip;
    @FXML
    private ToggleButton newChip;
    @FXML
    private TableView<Customer> customersTable;
    @FXML
    private TableColumn<Customer, String> documentColumn;
    @FXML
    private TableColumn<Customer, String> nameColumn;
    @FXML
    private TableColumn<Customer, String> phoneColumn;
    @FXML
    private TableColumn<Customer, String> emailColumn;
    @FXML
    private TableColumn<Customer, LocalDate> registeredColumn;
    @FXML
    private TableColumn<Customer, Integer> noShowsColumn;
    @FXML
    private Label countLabel;
    @FXML
    private Button newButton;
    @FXML
    private Label formTitle;
    @FXML
    private TextField documentField;
    @FXML
    private Label documentMessage;
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
    private Button cancelButton;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public CustomersController(AppContext context) {
        Objects.requireNonNull(context, "context");
        this.customers = context.customers();
        this.filter = CustomerFilter.all(LocalDate.now(context.clock()));
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
        alerts = new AlertPresenter(customersTable);
        setUpTable();
        setUpFilters();
        setUpForm();
        refresh();
    }

    private void setUpTable() {
        documentColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getDocument()));
        nameColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getName()));
        phoneColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getPhone()));
        emailColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getEmail()));
        registeredColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getRegisteredAt()));
        registeredColumn.setCellFactory(showing(DateFormats::date));
        noShowsColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getNoShowCount()));
        noShowsColumn.setCellFactory(column -> new NoShowsCell());
        customersTable.setRowFactory(table -> new NoShowRow());
        SortedList<Customer> sorted = new SortedList<>(shown);
        sorted.comparatorProperty().bind(customersTable.comparatorProperty());
        customersTable.setItems(sorted);
    }

    private void setUpFilters() {
        searchField.textProperty().addListener((observable, before, text) -> apply(filter.withText(text)));
        allChip.setUserData(CustomerFilter.Group.ALL);
        noShowsChip.setUserData(CustomerFilter.Group.WITH_NO_SHOWS);
        newChip.setUserData(CustomerFilter.Group.NEW);
        groupChips.selectedToggleProperty().addListener((observable, before, chip) -> {
            if (chip == null) {
                before.setSelected(true);
            } else {
                apply(filter.withGroup((CustomerFilter.Group) chip.getUserData()));
            }
        });
    }

    private void setUpForm() {
        formFields.field(documentField, documentMessage, "documento");
        formFields.field(nameField, nameMessage, "nombre");
        formFields.field(phoneField, phoneMessage, "teléfono");
        formFields.field(emailField, emailMessage, "correo");
        newButton.setOnAction(event -> startRegistering());
        cancelButton.setOnAction(event -> startRegistering());
        saveButton.setOnAction(event -> save());
        startRegistering();
    }

    /** Leaves the form ready to register a new customer. */
    private void startRegistering() {
        editing = null;
        formTitle.setText("Registrar cliente");
        documentField.clear();
        nameField.clear();
        phoneField.clear();
        emailField.clear();
        documentField.setDisable(false);
        formFields.clear();
        documentField.requestFocus();
    }

    /** Fills the form with a customer so it can be changed; the document cannot be changed. */
    private void startEditing(Customer customer) {
        editing = customer;
        formTitle.setText("Editar cliente");
        documentField.setText(customer.getDocument());
        nameField.setText(customer.getName());
        phoneField.setText(customer.getPhone());
        emailField.setText(customer.getEmail());
        documentField.setDisable(true);
        formFields.clear();
        nameField.requestFocus();
    }

    private void save() {
        try {
            ContactData data = new ContactData(documentField.getText(), nameField.getText(),
                phoneField.getText(), emailField.getText());
            if (editing == null) {
                customers.register(data);
                alerts.toast("Cliente registrado.");
            } else {
                customers.update(editing.getId(), data);
                alerts.toast("Cliente actualizado.");
            }
            startRegistering();
            refresh();
        } catch (ValidationException | BusinessRuleException e) {
            if (!formFields.show(e)) {
                alerts.showError(e);
            }
        } catch (DataAccessException e) {
            alerts.showError(e);
        }
    }

    private void apply(CustomerFilter newFilter) {
        filter = newFilter;
        shown.setPredicate(filter::matches);
        showCount();
    }

    /** Reads the customers again, so the table shows what is stored. */
    void refresh() {
        try {
            all.setAll(customers.listAll());
            showCount();
        } catch (DataAccessException e) {
            alerts.showError(e);
        }
    }

    private void showCount() {
        countLabel.setText("Mostrando " + shown.size() + " de " + all.size()
            + (all.size() == 1 ? " cliente" : " clientes"));
    }

    private static <T> Callback<TableColumn<Customer, T>, TableCell<Customer, T>> showing(Function<T, String> text) {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : text.apply(item));
            }
        };
    }

    /** The number of missed appointments, as a chip when there is at least one. */
    private static final class NoShowsCell extends TableCell<Customer, Integer> {
        @Override
        protected void updateItem(Integer count, boolean empty) {
            super.updateItem(count, empty);
            if (empty || count == null || count == 0) {
                setGraphic(null);
                setText(empty || count == null ? null : "0");
                return;
            }
            setText(null);
            Label chip = new Label(String.valueOf(count));
            chip.getStyleClass().add("chip-warning");
            setGraphic(chip);
        }
    }

    /** A row that stands out when the customer has missed appointments. */
    private static final class NoShowRow extends TableRow<Customer> {
        @Override
        protected void updateItem(Customer customer, boolean empty) {
            super.updateItem(customer, empty);
            getStyleClass().remove("row-noshow");
            if (!empty && customer != null && customer.getNoShowCount() > 0) {
                getStyleClass().add("row-noshow");
            }
        }
    }
}
