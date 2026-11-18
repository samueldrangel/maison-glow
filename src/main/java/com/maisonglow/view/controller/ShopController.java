package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Commerce.CatalogProduct;
import com.maisonglow.service.CatalogService;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.BackgroundTask;
import com.maisonglow.view.RefreshableView;
import com.maisonglow.view.ViewLifecycle;
import com.maisonglow.view.shop.ProductAvailability;
import com.maisonglow.view.shop.Cart;
import com.maisonglow.view.shop.ShopFilter;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Controller of {@code ShopView.fxml}: the shop of a customer (task N-R02). It lists what the business published, with its
 * cover, price and availability, and shows the detail of the product chosen. The customer only gets published products from
 * {@link CatalogService}, which also tells how many units are left after the reservations of other orders; the shop says
 * only {@linkplain ProductAvailability whether it is available}, never an internal figure. A product the business withdraws
 * while it is being looked at stops being offered: asking for its detail says so and the list is read again. The data is read
 * in the background and an answer that arrives after the session closed is ignored.
 */
public final class ShopController implements ViewLifecycle, RefreshableView {

    /** A published product with the file of its cover, if it has one. */
    private record Item(CatalogProduct product, Path cover) { }

    private final CatalogService catalog;
    private final Cart cart;
    private CatalogProduct open;
    private int quantity = 1;
    private final ToggleGroup categories = new ToggleGroup();
    private List<Item> items = List.of();
    private ShopFilter filter = ShopFilter.all();
    private int selected;
    private Executor executor = ForkJoinPool.commonPool();
    private AlertPresenter alerts;
    private boolean disposed;

    @FXML
    private HBox categoryChips;
    @FXML
    private TextField searchField;
    @FXML
    private FlowPane productsPane;
    @FXML
    private Label emptyLabel;
    @FXML
    private Label detailEmpty;
    @FXML
    private VBox detailBox;
    @FXML
    private ImageView detailCover;
    @FXML
    private Label detailPlaceholder;
    @FXML
    private Label detailName;
    @FXML
    private Label detailCategory;
    @FXML
    private Label detailAvailability;
    @FXML
    private Label detailPrice;
    @FXML
    private HBox detailUnavailable;
    @FXML
    private Label detailNote;
    @FXML
    private VBox orderBox;
    @FXML
    private Button minusButton;
    @FXML
    private Label quantityLabel;
    @FXML
    private Button plusButton;
    @FXML
    private Button addButton;
    @FXML
    private Label inCartLabel;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public ShopController(AppContext context) {
        Objects.requireNonNull(context, "context");
        this.catalog = context.catalog();
        this.cart = context.cart();
    }

    /**
     * Replaces where the data is read, which the tests use to hold an answer back.
     *
     * @param newExecutor runs the reading
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
        alerts = new AlertPresenter(productsPane);
        searchField.textProperty().addListener((observable, before, text) -> apply(filter.withText(text)));
        categories.selectedToggleProperty().addListener((observable, before, chip) ->
            apply(filter.withCategory(chip == null ? null : (String) chip.getUserData())));
        minusButton.setOnAction(event -> step(-1));
        plusButton.setOnAction(event -> step(1));
        addButton.setOnAction(event -> addToCart());
        showDetail(null);
        refresh();
    }

    /** Changes the quantity to add, between one and what is left after what is already in the cart. */
    private void step(int delta) {
        if (open == null) {
            return;
        }
        int room = Math.max(1, open.availableStock() - cart.quantityOf(open.id()));
        quantity = Math.max(1, Math.min(room, quantity + delta));
        quantityLabel.setText(String.valueOf(quantity));
    }

    private void addToCart() {
        if (open == null) {
            return;
        }
        try {
            cart.add(open, quantity);
            alerts.toast("Agregado al carrito.");
            quantity = 1;
            quantityLabel.setText("1");
            showInCart();
        } catch (ValidationException e) {
            alerts.showError(e);
        }
    }

    /** Tells how many units of the open product are already in the cart, and turns the order controls off when no more fit. */
    private void showInCart() {
        int inCart = open == null ? 0 : cart.quantityOf(open.id());
        inCartLabel.setVisible(inCart > 0);
        inCartLabel.setManaged(inCart > 0);
        inCartLabel.setText("En tu carrito: " + inCart);
        boolean room = open != null && ProductAvailability.of(open.availableStock()).canBeOrdered() && inCart < open.availableStock();
        addButton.setDisable(!room);
        plusButton.setDisable(!room);
        minusButton.setDisable(!room);
    }

    /** Reads the shop again; it is called when the screen is shown, so a product withdrawn meanwhile is not offered. */
    @Override
    public void refresh() {
        BackgroundTask.run(executor, this::read, this::show, error -> alerts.showError(error), () -> !disposed);
    }

    private List<Item> read() throws BusinessRuleException, DataAccessException, ValidationException {
        return catalog.listPublished("").stream().sorted(Comparator.comparing(CatalogProduct::name, String.CASE_INSENSITIVE_ORDER))
            .map(product -> new Item(product, coverOf(product))).toList();
    }

    private Path coverOf(CatalogProduct product) {
        if (!product.hasCover()) {
            return null;
        }
        try {
            Path cover = catalog.cover(product.id());
            return Files.isRegularFile(cover) ? cover : null;
        } catch (BusinessRuleException | DataAccessException e) {
            return null;
        }
    }

    private void show(List<Item> read) {
        items = read;
        showCategoryChips();
        apply(filter);
        if (selected != 0 && read.stream().noneMatch(item -> item.product().id() == selected)) {
            showDetail(null);
        }
    }

    /** One chip per category of the published products; pressing the chosen one again shows every category. */
    private void showCategoryChips() {
        String chosen = filter.category();
        categoryChips.getChildren().setAll(new TreeSet<>(items.stream().map(item -> item.product().category()).toList()).stream()
            .map(category -> {
                ToggleButton chip = new ToggleButton(category);
                chip.getStyleClass().add("filter-chip");
                chip.setUserData(category);
                chip.setToggleGroup(categories);
                chip.setSelected(category.equals(chosen));
                return (Node) chip;
            }).toList());
    }

    private void apply(ShopFilter newFilter) {
        filter = newFilter;
        List<Item> shown = items.stream().filter(item -> filter.matches(item.product())).toList();
        productsPane.getChildren().setAll(shown.stream().map(this::card).toList());
        emptyLabel.setText(items.isEmpty() ? "Aún no hay productos en la tienda." : "Ningún producto coincide con tu búsqueda.");
        emptyLabel.setVisible(shown.isEmpty());
        emptyLabel.setManaged(shown.isEmpty());
    }

    private Node card(Item item) {
        CatalogProduct product = item.product();
        VBox card = new VBox(6, frame(item.cover(), 190, 130), nameLabel(product), categoryLabel(product), priceRow(product));
        card.getStyleClass().addAll("card", "shop-card");
        card.setPrefWidth(224);
        card.setUserData(product.id());
        if (product.id() == selected) {
            card.getStyleClass().add("shop-card-selected");
        }
        card.setOnMouseClicked(event -> choose(product.id()));
        return card;
    }

    private static Node frame(Path cover, double width, double height) {
        StackPane frame = new StackPane();
        frame.getStyleClass().add("photo-frame");
        frame.setPrefHeight(height + 10);
        Image image = cover == null ? null : new Image(cover.toUri().toString(), width, height, true, true);
        if (image == null || image.isError()) {
            Label none = new Label("Sin foto");
            none.getStyleClass().add("placeholder-notice");
            frame.getChildren().add(none);
        } else {
            ImageView view = new ImageView(image);
            view.setPreserveRatio(true);
            view.setFitWidth(width);
            view.setFitHeight(height);
            frame.getChildren().add(view);
        }
        return frame;
    }

    private static Label nameLabel(CatalogProduct product) {
        Label name = new Label(product.name());
        name.getStyleClass().add("home-row-title");
        name.setWrapText(true);
        return name;
    }

    private static Label categoryLabel(CatalogProduct product) {
        Label category = new Label(product.category());
        category.getStyleClass().add("stat-caption");
        return category;
    }

    private static Node priceRow(CatalogProduct product) {
        ProductAvailability availability = ProductAvailability.of(product.availableStock());
        Label price = new Label(CurrencyFormatter.format(product.price()));
        price.getStyleClass().add("shop-price");
        price.setMinWidth(Region.USE_PREF_SIZE);
        Label chip = new Label(availability.label());
        chip.getStyleClass().add(availability.chipStyle());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(8, price, spacer, chip);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** Opens the detail of a product, asking the service again so that the price and the availability are the current ones. */
    private void choose(int productId) {
        selected = productId;
        apply(filter);
        BackgroundTask.run(executor, () -> catalog.findPublished(productId), this::showDetail, error -> {
            if (error instanceof BusinessRuleException) {
                showWithdrawn(productId);
                refresh();
            } else {
                alerts.showError(error);
            }
        }, () -> !disposed);
    }

    private void showDetail(CatalogProduct product) {
        boolean none = product == null;
        open = product;
        quantity = 1;
        quantityLabel.setText("1");
        orderBox.setVisible(!none);
        orderBox.setManaged(!none);
        detailEmpty.setVisible(none);
        detailEmpty.setManaged(none);
        detailBox.setVisible(!none);
        detailBox.setManaged(!none);
        if (none) {
            selected = 0;
            return;
        }
        selected = product.id();
        ProductAvailability availability = ProductAvailability.of(product.availableStock());
        detailName.setText(product.name());
        detailCategory.setText(product.category());
        detailAvailability.setText(availability.label());
        detailAvailability.getStyleClass().removeAll("chip-success", "chip-gold", "chip-neutral");
        detailAvailability.getStyleClass().add(availability.chipStyle());
        detailPrice.setText(CurrencyFormatter.format(product.price()));
        detailUnavailable.setVisible(false);
        detailUnavailable.setManaged(false);
        detailNote.setVisible(true);
        detailNote.setManaged(true);
        detailNote.setText(availability.canBeOrdered() ? "Se paga y se retira en el local." : "Por ahora no hay unidades disponibles.");
        Path cover = items.stream().filter(item -> item.product().id() == product.id()).map(Item::cover).findFirst().orElse(null);
        Image image = cover == null ? null : new Image(cover.toUri().toString(), 280, 200, true, true);
        boolean has = image != null && !image.isError();
        detailCover.setImage(has ? image : null);
        detailPlaceholder.setVisible(!has);
        showInCart();
    }

    /** The product was withdrawn while it was being looked at: the detail says so and offers nothing. */
    private void showWithdrawn(int productId) {
        detailUnavailable.setVisible(true);
        detailUnavailable.setManaged(true);
        detailNote.setVisible(false);
        detailNote.setManaged(false);
        open = null;
        orderBox.setVisible(false);
        orderBox.setManaged(false);
        detailAvailability.setText(ProductAvailability.SOLD_OUT.label());
        detailAvailability.getStyleClass().removeAll("chip-success", "chip-gold", "chip-neutral");
        detailAvailability.getStyleClass().add(ProductAvailability.SOLD_OUT.chipStyle());
        selected = productId;
    }

    /** Closing the session: an answer that arrives later is ignored and the products shown are dropped. */
    @Override
    public void dispose() {
        disposed = true;
        items = List.of();
        productsPane.getChildren().clear();
    }

    /** @return the id of the product whose detail is open, 0 if none; for the tests */
    int selected() {
        return selected;
    }
}
