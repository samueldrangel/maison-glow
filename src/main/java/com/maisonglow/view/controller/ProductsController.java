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
import com.maisonglow.view.Icons;
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
import javafx.scene.control.Tooltip;
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
    private final FieldValidator adjustFields = new FieldValidator();
    private Product editing;
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
    private ComboBox<Product> adjustProductBox;

    @FXML
    private Label adjustProductMessage;

    @FXML
    private Button minusButton;

    @FXML
    private Button plusButton;

    @FXML
    private TextField quantityField;

    @FXML
    private Label quantityMessage;

    @FXML
    private Button adjustButton;

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

    @FXML
    private TableColumn<Product, Product> actionsColumn;

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
        setUpAdjust();
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
        actionsColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue()));
        actionsColumn.setCellFactory(column -> new ActionsCell());
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

    /** Fills the form with a product so it can be changed; its stock is changed with the adjustment, not here. */
    private void startEditing(Product product) {
        editing = product;
        formTitle.setText("Editar producto");
        nameField.setText(product.getName());
        categoryBox.getItems().stream().filter(category -> category.getId() == product.getCategory().getId())
            .findFirst().ifPresent(categoryBox::setValue);
        priceField.setText(product.getPrice().stripTrailingZeros().toPlainString());
        minimumField.setText(String.valueOf(product.getMinimumStock()));
        initialStockBox.setVisible(false);
        initialStockBox.setManaged(false);
        formFields.clear();
        nameField.requestFocus();
    }

    private void setUpAdjust() {
        adjustProductBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Product product) {
                return product == null ? "" : product.getName() + " · " + product.getStock() + " uds.";
            }

            @Override
            public Product fromString(String text) {
                return null;
            }
        });
        adjustFields.field(adjustProductBox, adjustProductMessage, "producto");
        adjustFields.field(quantityField, quantityMessage, "cantidad", "ajuste", "insuficiente", "excede");
        minusButton.setGraphic(Icons.small(Icons.Kind.MINUS, 16));
        plusButton.setGraphic(Icons.small(Icons.Kind.PLUS, 16));
        minusButton.setOnAction(event -> stepQuantity(-1));
        plusButton.setOnAction(event -> stepQuantity(1));
        adjustButton.setOnAction(event -> adjustStock());
        productsTable.getSelectionModel().selectedItemProperty().addListener((observable, before, product) -> {
            if (product != null && product.isActive()) {
                adjustProductBox.getItems().stream().filter(item -> item.getId() == product.getId()).findFirst()
                    .ifPresent(adjustProductBox::setValue);
            }
        });
    }

    private void stepQuantity(int step) {
        int current;
        try {
            current = NumberInput.wholeNumber(quantityField.getText(), "cantidad");
        } catch (ValidationException e) {
            current = 0;
        }
        quantityField.setText(String.valueOf(current + step));
    }

    private void adjustStock() {
        try {
            Product selected = adjustProductBox.getValue();
            Validator.requirePresent(selected, "producto");
            int delta = NumberInput.wholeNumber(quantityField.getText(), "cantidad");
            Product updated = products.adjustStock(selected.getId(), delta);
            alerts.toast("Stock actualizado: " + updated.getName() + " ahora tiene " + updated.getStock() + " uds.");
            quantityField.setText("1");
            refresh();
        } catch (ValidationException | BusinessRuleException e) {
            if (!adjustFields.show(e)) {
                alerts.showError(e);
            }
        } catch (DataAccessException e) {
            alerts.showError(e);
        }
    }

    private void deactivate(Product product) {
        if (!alerts.confirm("¿Desactivar «" + product.getName() + "»? Dejará de aparecer en las ventas; "
            + "podrás reactivarlo cuando quieras.")) {
            return;
        }
        changeActive(product, false, "Producto desactivado.");
    }

    private void changeActive(Product product, boolean active, String notice) {
        try {
            if (active) {
                products.activate(product.getId());
            } else {
                products.deactivate(product.getId());
            }
            alerts.toast(notice);
            refresh();
        } catch (BusinessRuleException | DataAccessException e) {
            alerts.showError(e);
        }
    }

    /** Leaves the form ready to register a new product. */
    private void startRegistering() {
        editing = null;
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
            if (editing == null) {
                products.register(data, NumberInput.wholeNumber(initialStockField.getText(), "stock inicial"));
                alerts.toast("Producto registrado.");
            } else {
                products.update(editing.getId(), data);
                alerts.toast("Producto actualizado.");
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
            showProductsToAdjust();
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

    /** Fills the product box of the adjustment with the active products, keeping the one that was chosen. */
    private void showProductsToAdjust() {
        Product chosen = adjustProductBox.getValue();
        adjustProductBox.getItems().setAll(all.stream().filter(Product::isActive).toList());
        if (chosen != null) {
            adjustProductBox.getItems().stream().filter(item -> item.getId() == chosen.getId()).findFirst()
                .ifPresent(adjustProductBox::setValue);
        }
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

    /** Edit, and deactivate or reactivate, as small icon buttons at the end of each row. */
    private final class ActionsCell extends TableCell<Product, Product> {
        @Override
        protected void updateItem(Product product, boolean empty) {
            super.updateItem(product, empty);
            if (empty || product == null) {
                setGraphic(null);
                return;
            }
            Button edit = iconButton(Icons.Kind.EDIT, "Editar " + product.getName(), false);
            edit.setOnAction(event -> startEditing(product));
            Button toggle = product.isActive()
                ? iconButton(Icons.Kind.TRASH, "Desactivar " + product.getName(), true)
                : iconButton(Icons.Kind.RESTORE, "Reactivar " + product.getName(), false);
            toggle.setOnAction(event -> {
                if (product.isActive()) {
                    deactivate(product);
                } else {
                    changeActive(product, true, "Producto reactivado.");
                }
            });
            setGraphic(new HBox(4, edit, toggle));
        }

        private Button iconButton(Icons.Kind kind, String description, boolean danger) {
            Button button = new Button();
            button.setGraphic(Icons.small(kind, 16));
            button.getStyleClass().add("icon-button");
            if (danger) {
                button.getStyleClass().add("danger");
            }
            button.setAccessibleText(description);
            button.setTooltip(new Tooltip(description));
            return button;
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
