package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Product;
import com.maisonglow.model.ProductCategory;
import com.maisonglow.service.ProductService;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.product.ProductFilter;
import com.maisonglow.view.product.ProductStatus;
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
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.util.Callback;

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
    private ProductFilter filter = ProductFilter.all();
    private AlertPresenter alerts;

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

    /** Reads the inventory again, so the table, the warning and the chips show what is stored. */
    void refresh() {
        try {
            all.setAll(products.listAll());
            showCategoryChips(products.listCategories());
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
