package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Product;
import com.maisonglow.model.Sellable;
import com.maisonglow.service.CustomerService;
import com.maisonglow.service.ProductService;
import com.maisonglow.service.TreatmentService;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.util.TextSearch;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FieldValidator;
import com.maisonglow.view.Icons;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Objects;

/**
 * Controller of {@code SalesView.fxml}: a new sale (mockup M6). The left side picks the customer and
 * finds products and treatments; the right side is the cart, which {@link com.maisonglow.service.SaleService}
 * registers. It never talks to a DAO.
 */
public final class SalesController {

    private final CustomerService customers;
    private final ProductService products;
    private final TreatmentService treatments;
    private final ObservableList<Sellable> catalog = FXCollections.observableArrayList();
    private final FilteredList<Sellable> shown = new FilteredList<>(catalog);
    private final FieldValidator fields = new FieldValidator();
    private AlertPresenter alerts;

    @FXML
    private ComboBox<Customer> customerBox;
    @FXML
    private Label customerMessage;
    @FXML
    private TextField searchField;
    @FXML
    private ToggleGroup kindGroup;
    @FXML
    private ToggleButton productsChip;
    @FXML
    private TableView<Sellable> catalogTable;
    @FXML
    private TableColumn<Sellable, String> catalogNameColumn;
    @FXML
    private TableColumn<Sellable, Sellable> catalogPriceColumn;
    @FXML
    private TableColumn<Sellable, Sellable> catalogAvailableColumn;
    @FXML
    private TableColumn<Sellable, Sellable> catalogActionColumn;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public SalesController(AppContext context) {
        Objects.requireNonNull(context, "context");
        this.customers = context.customers();
        this.products = context.products();
        this.treatments = context.treatments();
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
        alerts = new AlertPresenter(catalogTable);
        fields.field(customerBox, customerMessage, "cliente");
        setUpCustomers();
        setUpCatalog();
        refreshCatalog();
    }

    private void setUpCustomers() {
        customerBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Customer customer) {
                return customer == null ? "" : customer.getName() + " · " + customer.getDocument();
            }

            @Override
            public Customer fromString(String text) {
                return null;
            }
        });
        try {
            customerBox.setItems(FXCollections.observableArrayList(customers.listAll()));
        } catch (DataAccessException e) {
            alerts.showError(e);
        }
    }

    private void setUpCatalog() {
        catalogNameColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getName()));
        catalogPriceColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        catalogPriceColumn.setCellFactory(column -> new TextCell(item -> CurrencyFormatter.format(item.getPrice())));
        catalogAvailableColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        catalogAvailableColumn.setCellFactory(column -> new TextCell(SalesController::availability));
        catalogActionColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        catalogActionColumn.setCellFactory(column -> new AddCell());
        catalogTable.setItems(shown);
        searchField.textProperty().addListener((observable, before, text) ->
            shown.setPredicate(item -> TextSearch.contains(item.getName(), text)));
        kindGroup.selectedToggleProperty().addListener((observable, before, chip) -> {
            if (chip == null) {
                before.setSelected(true);
            } else {
                refreshCatalog();
            }
        });
    }

    /** Loads what can be sold: the active products or the active treatments, whichever chip is chosen. */
    private void refreshCatalog() {
        try {
            List<? extends Sellable> items = productsChip.isSelected() ? products.listActive() : treatments.listActive();
            catalog.setAll(items);
        } catch (DataAccessException e) {
            alerts.showError(e);
        }
    }

    private static String availability(Sellable item) {
        return item instanceof Product product ? product.getStock() + " uds." : "Servicio";
    }

    /** Adds one unit of the item to the cart; filled in with the cart. */
    private void addToCart(Sellable item) {
        Objects.requireNonNull(item, "item");
    }

    private static final class TextCell extends TableCell<Sellable, Sellable> {
        private final java.util.function.Function<Sellable, String> text;

        TextCell(java.util.function.Function<Sellable, String> text) {
            this.text = text;
        }

        @Override
        protected void updateItem(Sellable item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : text.apply(item));
        }
    }

    /** The "add" button at the end of each catalog row; a product with no units cannot be added. */
    private final class AddCell extends TableCell<Sellable, Sellable> {
        @Override
        protected void updateItem(Sellable item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            Button add = new Button();
            add.setGraphic(Icons.small(Icons.Kind.PLUS, 16));
            add.getStyleClass().add("icon-button");
            add.setAccessibleText("Agregar " + item.getName());
            add.setTooltip(new Tooltip("Agregar " + item.getName()));
            add.setDisable(item instanceof Product product && product.getStock() <= 0);
            add.setOnAction(event -> addToCart(item));
            setGraphic(add);
        }
    }
}
