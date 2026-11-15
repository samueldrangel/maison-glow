package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Customer;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.CustomerService;
import com.maisonglow.service.CustomerStatistics;
import com.maisonglow.service.SaleService;
import com.maisonglow.service.StatisticsService;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.util.DateFormats;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FieldValidator;
import com.maisonglow.view.Icons;
import com.maisonglow.view.SlidePanel;
import com.maisonglow.view.customer.CustomerFilter;
import com.maisonglow.view.customer.CustomerProfile;
import com.maisonglow.view.customer.PurchaseSummary;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Controller of {@code CustomersView.fxml}: the customers (mockup M2). It lists them, searches by name,
 * document or phone, and marks the ones who missed appointments. It only talks to
 * {@link CustomerService}, never to a DAO.
 */
public final class CustomersController {

    private static final int RECENT_PURCHASES = 3;

    private final CustomerService customers;
    private final StatisticsService statistics;
    private final SaleService sales;
    private final LocalDate today;
    private final ObservableList<Customer> all = FXCollections.observableArrayList();
    private final FilteredList<Customer> shown = new FilteredList<>(all);
    private final FieldValidator formFields = new FieldValidator();
    private CustomerFilter filter;
    private Customer editing;
    private SlidePanel formDrawer;
    private SlidePanel statsDrawer;
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
    private TableColumn<Customer, Customer> actionsColumn;
    @FXML
    private Label countLabel;
    @FXML
    private Label statsInitials;
    @FXML
    private Label statsName;
    @FXML
    private Label statsDocument;
    @FXML
    private HBox statsChips;
    @FXML
    private VBox purchasesBox;
    @FXML
    private Label editNote;
    @FXML
    private Label statsPhone;
    @FXML
    private Label statsEmail;
    @FXML
    private Label statsSince;
    @FXML
    private Label purchasesValue;
    @FXML
    private Label spentValue;
    @FXML
    private Label appointmentsValue;
    @FXML
    private Label completedValue;
    @FXML
    private Label averageValue;
    @FXML
    private Label attendanceValue;
    @FXML
    private ProgressBar attendanceBar;
    @FXML
    private Button newButton;
    @FXML
    private ScrollPane formPanel;
    @FXML
    private ScrollPane statsPanel;
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
        this.statistics = context.statistics();
        this.sales = context.sales();
        this.today = LocalDate.now(context.clock());
        this.filter = CustomerFilter.all(today);
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
        actionsColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        actionsColumn.setCellFactory(column -> new ActionsCell());
        customersTable.getSelectionModel().selectedItemProperty()
            .addListener((observable, before, customer) -> showStatistics(customer));
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
        formDrawer = new SlidePanel(formPanel, formPanel.getContent(), formPanel.getPrefWidth());
        statsDrawer = new SlidePanel(statsPanel, statsPanel.getContent(), statsPanel.getPrefWidth());
        formFields.field(documentField, documentMessage, "documento");
        formFields.field(nameField, nameMessage, "nombre");
        formFields.field(phoneField, phoneMessage, "teléfono");
        formFields.field(emailField, emailMessage, "correo");
        newButton.setOnAction(event -> {
            startRegistering();
            showForm();
        });
        cancelButton.setOnAction(event -> {
            startRegistering();
            formDrawer.close();
        });
        saveButton.setOnAction(event -> save());
        startRegistering();
    }

    /** Brings the form in; the statistics give way to it, so only one panel takes the right side. */
    private void showForm() {
        customersTable.getSelectionModel().clearSelection();
        formDrawer.open();
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
        editNote.setVisible(false);
        editNote.setManaged(false);
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
        editNote.setVisible(true);
        editNote.setManaged(true);
        formFields.clear();
        showForm();
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
            formDrawer.close();
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

    /** Reads the customers again, so the table shows what is stored, keeping the selected customer. */
    void refresh() {
        Customer selected = customersTable.getSelectionModel().getSelectedItem();
        try {
            all.setAll(customers.listAll());
            showCount();
            if (selected != null) {
                customersTable.getItems().stream().filter(customer -> customer.getId() == selected.getId())
                    .findFirst().ifPresent(customersTable.getSelectionModel()::select);
            }
        } catch (DataAccessException e) {
            alerts.showError(e);
        }
    }

    /**
     * Brings in the panel with the purchases, appointments and no-shows of the selected customer, and
     * takes it out when nobody is selected. A form that was open gives way to it.
     */
    private void showStatistics(Customer customer) {
        if (customer == null) {
            statsDrawer.close();
            return;
        }
        try {
            showProfile(new CustomerProfile(customer, statistics.customerStatistics(customer.getId()), today));
            showPurchases(PurchaseSummary.recent(sales.listByCustomer(customer.getId()), RECENT_PURCHASES));
        } catch (ValidationException | DataAccessException e) {
            alerts.showError(e);
            return;
        }
        if (formDrawer.isOpen()) {
            startRegistering();
            formDrawer.close();
        }
        statsDrawer.open();
    }

    /** Tells who the customer is, how much the customer buys and how reliably the customer shows up. */
    private void showProfile(CustomerProfile profile) {
        Customer customer = profile.customer();
        CustomerStatistics figures = profile.statistics();
        statsInitials.setText(profile.initials());
        statsName.setText(customer.getName());
        statsDocument.setText("Documento " + customer.getDocument());
        statsPhone.setText(customer.getPhone());
        statsEmail.setText(customer.getEmail());
        statsSince.setText("Cliente desde " + DateFormats.date(customer.getRegisteredAt()));
        statsChips.getChildren().clear();
        if (profile.isNew()) {
            statsChips.getChildren().add(chip("Nuevo", "chip-gold"));
        }
        statsChips.getChildren().add(profile.noShows() == 0 ? chip("Sin inasistencias", "chip-success")
            : chip(profile.noShows() + (profile.noShows() == 1 ? " inasistencia" : " inasistencias"), "chip-warning"));
        purchasesValue.setText(String.valueOf(figures.purchases()));
        spentValue.setText(CurrencyFormatter.format(figures.totalSpent()));
        appointmentsValue.setText(String.valueOf(figures.appointments()));
        completedValue.setText(String.valueOf(figures.completedAppointments()));
        averageValue.setText(CurrencyFormatter.format(profile.averageTicket()));
        attendanceValue.setText(profile.attendance().isPresent()
            ? Math.round(profile.attendance().getAsDouble() * 100) + " %" : "Sin citas todavía");
        attendanceBar.setProgress(profile.attendance().orElse(0));
    }

    /** Lists the latest purchases, or says the customer has not bought yet. */
    private void showPurchases(List<PurchaseSummary> recent) {
        purchasesBox.getChildren().clear();
        if (recent.isEmpty()) {
            Label none = new Label("Aún no ha comprado.");
            none.getStyleClass().add("form-hint");
            purchasesBox.getChildren().add(none);
        }
        for (PurchaseSummary purchase : recent) {
            Label items = new Label(purchase.items());
            items.getStyleClass().add("purchase-items");
            Label date = new Label(DateFormats.date(purchase.soldAt().toLocalDate()));
            date.getStyleClass().add("stat-caption");
            Label total = new Label(CurrencyFormatter.format(purchase.total()));
            total.getStyleClass().add("stat-value");
            VBox words = new VBox(items, date);
            HBox.setHgrow(words, Priority.ALWAYS);
            HBox row = new HBox(words, total);
            row.getStyleClass().add("purchase-row");
            purchasesBox.getChildren().add(row);
        }
    }

    private static Label chip(String text, String style) {
        Label chip = new Label(text);
        chip.getStyleClass().add(style);
        return chip;
    }

    private void delete(Customer customer) {
        if (!alerts.confirm("¿Eliminar a «" + customer.getName() + "»? Esta acción no se puede deshacer.")) {
            return;
        }
        try {
            customers.delete(customer.getId());
            if (editing != null && editing.getId() == customer.getId()) {
                startRegistering();
                formDrawer.close();
            }
            alerts.toast("Cliente eliminado.");
            refresh();
        } catch (BusinessRuleException | DataAccessException e) {
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

    /** Edit and delete as small icon buttons at the end of each row. */
    private final class ActionsCell extends TableCell<Customer, Customer> {
        @Override
        protected void updateItem(Customer customer, boolean empty) {
            super.updateItem(customer, empty);
            if (empty || customer == null) {
                setGraphic(null);
                return;
            }
            Button edit = iconButton(Icons.Kind.EDIT, "Editar " + customer.getName());
            edit.setOnAction(event -> startEditing(customer));
            Button remove = iconButton(Icons.Kind.TRASH, "Eliminar " + customer.getName());
            remove.getStyleClass().add("danger");
            remove.setOnAction(event -> delete(customer));
            setGraphic(new HBox(4, edit, remove));
        }

        private Button iconButton(Icons.Kind kind, String description) {
            Button button = new Button();
            button.setGraphic(Icons.small(kind, 16));
            button.getStyleClass().add("icon-button");
            button.setAccessibleText(description);
            button.setTooltip(new Tooltip(description));
            return button;
        }
    }
}
