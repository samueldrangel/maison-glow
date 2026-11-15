package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Product;
import com.maisonglow.model.ProductCategory;
import com.maisonglow.service.ProductData;
import com.maisonglow.service.ProductService;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.util.NumberInput;
import com.maisonglow.util.Validator;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FieldValidator;
import com.maisonglow.view.product.ProductFilter;
import com.maisonglow.view.product.ProductStatus;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Controller of {@code ProductsView.fxml}: the inventory (mockup M4). It lists
 * the products, warns about the ones to restock and filters them. It only talks
 * to {@link ProductService}, never to a DAO.
 */
public final class ProductsController {

    private final ProductService products;
    private final ObservableList<Product> all = FXCollections.observableArrayList();
    private final FilteredList<Product> shown = new FilteredList<>(all);
    private final ToggleGroup categories = new ToggleGroup();
    private final FieldValidator formFields = new FieldValidator();
    private ProductFilter filter = ProductFilter.all();
    private AlertPresenter alerts;

    @FXML
    private Button newButton;

    @FXML
    private Label formTitle;

    @FXML
    private TextField nameField;

    @FXML
    private Label nameMessage;

    @FXML
    private ComboBox<ProductCategory> categoryBox;

    @FXML
    private Label categoryMessage;

    @FXML
    private TextField priceField;

    @FXML
    private Label priceMessage;

    @FXML
    private TextField minimumField;

    @FXML
    private Label minimumMessage;

    @FXML
    private VBox initialStockBox;

    @FXML
    private TextField initialStockField;

    @FXML
    private Label initialStockMessage;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    @FXML
    private HBox lowStockBanner;

    @FXML
    private Label lowStockText;

    @FXML
    private Button lowStockButton;

    @FXML
    private TextField searchField;

    @FXML
    private HBox categoryChips;

    @FXML
    private TableView<Product> productsTable;

    @FXML
    private TableColumn<Product, String> nameColumn;

    @FXML
    private TableColumn<Product, String> categoryColumn;

    @FXML
    private TableColumn<Product, BigDecimal> priceColumn;

    @FXML
    private TableColumn<Product, Integer> stockColumn;

    @FXML
    private TableColumn<Product, Integer> minimumColumn;

    @FXML
    private TableColumn<Product, Product> statusColumn;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public ProductsController(AppContext context) {
        this.products = Objects.requireNonNull(context, "context").products();
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
        alerts = new AlertPresenter(productsTable);
        setUpTable();
        setUpFilters();
        setUpForm();
        refresh();
    }

    private void setUpTable() {
        nameColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getName()));
        categoryColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getCategory().getName()));
        priceColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getPrice()));
        stockColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getStock()));
        minimumColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().getMinimumStock()));
        statusColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        priceColumn.setCellFactory(showing(CurrencyFormatter::format));
        statusColumn.setCellFactory(column -> new StatusCell());
        statusColumn.setComparator((first, second) -> ProductStatus.of(first).compareTo(ProductStatus.of(second)));
        productsTable.setRowFactory(table -> new RestockRow());
        SortedList<Product> sorted = new SortedList<>(shown);
        sorted.comparatorProperty().bind(productsTable.comparatorProperty());
        productsTable.setItems(sorted);
    }

    private void setUpFilters() {
        searchField.textProperty().addListener((observable, before, text) -> apply(filter.withText(text)));
        lowStockButton.setOnAction(event -> apply(filter.withOnlyRestock(!filter.onlyRestock())));
        categories.selectedToggleProperty().addListener((observable, before, chip) ->
            apply(filter.withCategory(chip == null ? null : (Integer) chip.getUserData())));
    }

    private void apply(ProductFilter newFilter) {
        filter = newFilter;
        shown.setPredicate(filter::matches);
        lowStockButton.setText(filter.onlyRestock() ? "Ver todos" : "Ver solo esos");
    }

    private void setUpForm() {
        categoryBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ProductCategory category) {
                return category == null ? "" : category.getName();
            }

            @Override
            public ProductCategory fromString(String text) {
                return null;
            }
        });
        // The order matters: "stock mínimo" is looked for before "stock", which its message also contains.
        formFields.field(nameField, nameMessage, "nombre", "llamado");
        formFields.field(categoryBox, categoryMessage, "categoría");
        formFields.field(priceField, priceMessage, "precio");
        formFields.field(minimumField, minimumMessage, "mínimo");
        formFields.field(initialStockField, initialStockMessage, "campo stock");
        newButton.setOnAction(event -> startRegistering());
        cancelButton.setOnAction(event -> startRegistering());
        saveButton.setOnAction(event -> save());
        startRegistering();
    }

    /** Leaves the form ready to register a new product. */
    private void startRegistering() {
        formTitle.setText("Registrar producto");
        nameField.clear();
        categoryBox.setValue(null);
        priceField.clear();
        minimumField.clear();
        initialStockField.setText("0");
        initialStockBox.setVisible(true);
        initialStockBox.setManaged(true);
        formFields.clear();
        nameField.requestFocus();
    }

    private void save() {
        try {
            ProductData data = readForm();
            int initialStock = NumberInput.wholeNumber(initialStockField.getText(), "stock inicial");
            products.register(data, initialStock);
            alerts.toast("Producto registrado.");
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

    /** Reads the fields in the order they appear, so the first mistake is the one that shows. */
    private ProductData readForm() throws ValidationException {
        Validator.requireText(nameField.getText(), "nombre");
        ProductCategory category = categoryBox.getValue();
        Validator.requirePresent(category, "categoría");
        BigDecimal price = NumberInput.money(priceField.getText(), "precio");
        int minimum = NumberInput.wholeNumber(minimumField.getText(), "stock mínimo");
        return new ProductData(nameField.getText(), category.getId(), price, minimum);
    }

    /** Reads the inventory again, so the table, the warning and the chips show what is stored. */
    void refresh() {
        try {
            all.setAll(products.listAll());
            List<ProductCategory> categoryList = products.listCategories();
            showCategoryChips(categoryList);
            showCategoriesInForm(categoryList);
            showLowStockWarning(products.listLowStock().size());
        } catch (DataAccessException e) {
            alerts.showError(e);
        }
    }

    private void showLowStockWarning(int count) {
        lowStockBanner.setVisible(count > 0);
        lowStockBanner.setManaged(count > 0);
        lowStockText.setText(count == 1 ? "1 producto por debajo del stock mínimo"
            : count + " productos por debajo del stock mínimo");
    }

    /** One chip per category; pressing the chosen one again shows every category. */
    private void showCategoryChips(List<ProductCategory> list) {
        Integer chosen = filter.categoryId();
        categoryChips.getChildren().setAll(list.stream().map(category -> {
            ToggleButton chip = new ToggleButton(category.getName());
            chip.getStyleClass().add("filter-chip");
            chip.setUserData(category.getId());
            chip.setToggleGroup(categories);
            chip.setSelected(chosen != null && chosen == category.getId());
            return chip;
        }).toList());
    }

    /** Fills the category box of the form, keeping the category that was chosen. */
    private void showCategoriesInForm(List<ProductCategory> list) {
        ProductCategory chosen = categoryBox.getValue();
        categoryBox.getItems().setAll(list);
        if (chosen != null) {
            list.stream().filter(category -> category.getId() == chosen.getId()).findFirst().ifPresent(categoryBox::setValue);
        }
    }

    private static <T> Callback<TableColumn<Product, T>, TableCell<Product, T>> showing(Function<T, String> text) {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : text.apply(item));
            }
        };
    }

    /** The state of a product as a chip with a dot. */
    private static final class StatusCell extends TableCell<Product, Product> {
        @Override
        protected void updateItem(Product product, boolean empty) {
            super.updateItem(product, empty);
            if (empty || product == null) {
                setGraphic(null);
                return;
            }
            ProductStatus status = ProductStatus.of(product);
            Label chip = new Label(status.label());
            Circle dot = new Circle(3.5);
            dot.getStyleClass().add("chip-dot");
            chip.setGraphic(dot);
            chip.getStyleClass().add(status.chipStyle());
            setGraphic(chip);
        }
    }

    /** A row that stands out when the product has to be restocked. */
    private static final class RestockRow extends TableRow<Product> {
        @Override
        protected void updateItem(Product product, boolean empty) {
            super.updateItem(product, empty);
            getStyleClass().remove("row-restock");
            if (!empty && product != null && ProductStatus.of(product).needsRestock()) {
                getStyleClass().add("row-restock");
            }
        }
    }
}
