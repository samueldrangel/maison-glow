package com.maisonglow.ui.console;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Product;
import com.maisonglow.service.ProductData;
import com.maisonglow.service.ProductService;
import java.util.List;
import java.util.Objects;

/**
 * Console module for products and inventory: register, update, list, low
 * stock, stock adjustments and activate or deactivate. It delegates every
 * rule to {@link ProductService}.
 */
public final class ProductModule extends MenuModule {

    private static final String NONE_FOUND = "No hay productos que mostrar.";

    private final ProductService products;

    /**
     * Creates the module.
     *
     * @param console  where the module talks to the user
     * @param products the product service
     */
    public ProductModule(ConsoleIo console, ProductService products) {
        super("Productos e inventario", console);
        this.products = Objects.requireNonNull(products, "products");
    }

    @Override
    protected List<MenuOption> options() {
        return List.of(
            new MenuOption("Registrar producto", this::register),
            new MenuOption("Listar todos los productos", this::listAll),
            new MenuOption("Listar productos activos", this::listActive),
            new MenuOption("Productos con stock bajo", this::listLowStock),
            new MenuOption("Ver producto por id", this::findById),
            new MenuOption("Actualizar producto", this::update),
            new MenuOption("Ajustar stock (sumar o restar unidades)", this::adjustStock),
            new MenuOption("Activar producto", this::activate),
            new MenuOption("Desactivar producto", this::deactivate),
            new MenuOption("Ver categorías", this::showCategories));
    }

    private ProductData askData() throws DataAccessException {
        String name = console().readLine("Nombre");
        showCategories();
        int categoryId = console().readInt("Id de la categoría");
        double price = console().readDouble("Precio");
        int minimumStock = console().readInt("Stock mínimo");
        return new ProductData(name, categoryId, price, minimumStock);
    }

    private void showCategories() throws DataAccessException {
        console().println("Categorías:");
        products.listCategories().forEach(category ->
            console().println("  [" + category.getId() + "] " + category.getName()));
    }

    private void register() throws ValidationException, BusinessRuleException, DataAccessException {
        ProductData data = askData();
        int initialStock = console().readInt("Stock inicial");
        Product product = products.register(data, initialStock);
        console().println("Producto registrado: " + ConsoleFormat.product(product));
    }

    private void listAll() throws DataAccessException {
        ConsoleFormat.printAll(console(), NONE_FOUND,
            products.listAll().stream().map(ConsoleFormat::product).toList());
    }

    private void listActive() throws DataAccessException {
        ConsoleFormat.printAll(console(), NONE_FOUND,
            products.listActive().stream().map(ConsoleFormat::product).toList());
    }

    private void listLowStock() throws DataAccessException {
        ConsoleFormat.printAll(console(), "Ningún producto está por debajo del stock mínimo.",
            products.listLowStock().stream().map(ConsoleFormat::product).toList());
    }

    private void findById() throws BusinessRuleException, DataAccessException {
        console().println(ConsoleFormat.product(products.findById(console().readInt("Id del producto"))));
    }

    private void update() throws ValidationException, BusinessRuleException, DataAccessException {
        int id = console().readInt("Id del producto");
        console().println("Datos actuales: " + ConsoleFormat.product(products.findById(id)));
        Product product = products.update(id, askData());
        console().println("Producto actualizado: " + ConsoleFormat.product(product));
    }

    private void adjustStock() throws ValidationException, BusinessRuleException, DataAccessException {
        int id = console().readInt("Id del producto");
        int delta = console().readInt("Unidades a sumar (use un número negativo para restar)");
        Product product = products.adjustStock(id, delta);
        console().println("Stock ajustado: " + ConsoleFormat.product(product));
    }

    private void activate() throws BusinessRuleException, DataAccessException {
        int id = console().readInt("Id del producto");
        products.activate(id);
        console().println("Producto activado: " + ConsoleFormat.product(products.findById(id)));
    }

    private void deactivate() throws BusinessRuleException, DataAccessException {
        int id = console().readInt("Id del producto");
        products.deactivate(id);
        console().println("Producto desactivado: " + ConsoleFormat.product(products.findById(id)));
    }
}
