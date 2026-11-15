package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Product;
import com.maisonglow.model.SaleLine;
import com.maisonglow.model.Sellable;
import com.maisonglow.service.CustomerService;
import com.maisonglow.service.ProductService;
import com.maisonglow.service.SaleService;
import com.maisonglow.service.TreatmentService;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.util.TextSearch;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FieldValidator;
import com.maisonglow.view.Icons;
import com.maisonglow.view.sales.Cart;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
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
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

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
    private final Cart cart = new Cart();
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
    @FXML
    private TableView<SaleLine> cartTable;
    @FXML
    private TableColumn<SaleLine, String> cartNameColumn;
    @FXML
    private TableColumn<SaleLine, SaleLine> cartQuantityColumn;
    @FXML
    private TableColumn<SaleLine, SaleLine> cartSubtotalColumn;
    @FXML
    private TableColumn<SaleLine, SaleLine> cartRemoveColumn;
    @FXML
    private Label cartMessage;
    @FXML
    private Label totalLabel;
    @FXML
    private Button clearButton;

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
        setUpCart();
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
        catalogPriceColumn.setCellFactory(column -> new TextCell<>(item -> CurrencyFormatter.format(item.getPrice())));
        catalogAvailableColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        catalogAvailableColumn.setCellFactory(column -> new TextCell<>(SalesController::availability));
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

    private void setUpCart() {
        cartNameColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getItem().getName()));
        cartQuantityColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        cartQuantityColumn.setCellFactory(column -> new QuantityCell());
        cartSubtotalColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        cartSubtotalColumn.setCellFactory(column ->
            new TextCell<>(line -> CurrencyFormatter.format(SaleService.calculateSubtotal(line))));
        cartRemoveColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        cartRemoveColumn.setCellFactory(column -> new RemoveCell());
        cartTable.setItems(cart.lines());
        totalLabel.textProperty().bind(Bindings.createStringBinding(
            () -> CurrencyFormatter.format(cart.total().get()), cart.total()));
        hideCartMessage();
        cart.lines().addListener((ListChangeListener<SaleLine>) change -> hideCartMessage());
        clearButton.disableProperty().bind(Bindings.isEmpty(cart.lines()));
        clearButton.setOnAction(event -> cart.clear());
    }

    /** Adds one unit of the item to the cart. */
    private void addToCart(Sellable item) {
        cart.add(Objects.requireNonNull(item, "item"));
    }

    private void showCartMessage(String text) {
        cartMessage.setText(text);
        cartMessage.setVisible(true);
        cartMessage.setManaged(true);
    }

    private void hideCartMessage() {
        cartMessage.setVisible(false);
        cartMessage.setManaged(false);
    }

    private static final class TextCell<S, T> extends TableCell<S, T> {
        private final Function<T, String> text;

        TextCell(Function<T, String> text) {
            this.text = text;
        }

        @Override
        protected void updateItem(T item, boolean empty) {
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
            Button add = iconButton(Icons.Kind.PLUS, "Agregar " + item.getName());
            add.setDisable(item instanceof Product product && product.getStock() <= 0);
            add.setOnAction(event -> addToCart(item));
            setGraphic(add);
        }
    }

    /** Minus, the quantity and plus: the quantity of a line is changed right in the cart. */
    private final class QuantityCell extends TableCell<SaleLine, SaleLine> {
        @Override
        protected void updateItem(SaleLine line, boolean empty) {
            super.updateItem(line, empty);
            if (empty || line == null) {
                setGraphic(null);
                return;
            }
            Button minus = iconButton(Icons.Kind.MINUS, "Quitar una unidad de " + line.getItem().getName());
            minus.setDisable(line.getQuantity() <= 1);
            minus.setOnAction(event -> cart.decrease(line));
            Button plus = iconButton(Icons.Kind.PLUS, "Agregar una unidad de " + line.getItem().getName());
            plus.setOnAction(event -> cart.increase(line));
            HBox box = new HBox(2, minus, new Label(String.valueOf(line.getQuantity())), plus);
            box.getStyleClass().add("quantity-box");
            setGraphic(box);
        }
    }

    /** The trash button that takes a line out of the cart. */
    private final class RemoveCell extends TableCell<SaleLine, SaleLine> {
        @Override
        protected void updateItem(SaleLine line, boolean empty) {
            super.updateItem(line, empty);
            if (empty || line == null) {
                setGraphic(null);
                return;
            }
            Button remove = iconButton(Icons.Kind.TRASH, "Quitar " + line.getItem().getName() + " de la venta");
            remove.getStyleClass().add("danger");
            remove.setOnAction(event -> cart.remove(line));
            setGraphic(remove);
        }
    }

    private static Button iconButton(Icons.Kind kind, String description) {
        Button button = new Button();
        button.setGraphic(Icons.small(kind, 16));
        button.getStyleClass().add("icon-button");
        button.setAccessibleText(description);
        button.setTooltip(new Tooltip(description));
        return button;
    }
}
