package com.maisonglow.ui.console;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Sale;
import com.maisonglow.service.CustomerService;
import com.maisonglow.service.EmailNotifier;
import com.maisonglow.service.ProductService;
import com.maisonglow.service.SaleService;
import com.maisonglow.service.TreatmentService;
import java.util.List;
import java.util.Objects;

/**
 * Console module for sales: build a cart with products and treatments,
 * register it (the stock is deducted by {@link SaleService}), send the
 * invoice and list the sales of a customer.
 */
public final class SaleModule extends MenuModule {

    private static final int FINISH = 0;
    private static final int PRODUCT = 1;
    private static final int TREATMENT = 2;

    private final SaleService sales;
    private final CustomerService customers;
    private final ProductService products;
    private final TreatmentService treatments;
    private final EmailNotifier notifier;

    /**
     * Creates the module.
     *
     * @param console    where the module talks to the user
     * @param sales      the sale service
     * @param customers  the customer service, to find the buyer
     * @param products   the product service, to find products to sell
     * @param treatments the treatment service, to find treatments to sell
     * @param notifier   sends the invoice once the sale is registered
     */
    public SaleModule(ConsoleIo console, SaleService sales, CustomerService customers, ProductService products,
                      TreatmentService treatments, EmailNotifier notifier) {
        super("Ventas", console);
        this.sales = Objects.requireNonNull(sales, "sales");
        this.customers = Objects.requireNonNull(customers, "customers");
        this.products = Objects.requireNonNull(products, "products");
        this.treatments = Objects.requireNonNull(treatments, "treatments");
        this.notifier = Objects.requireNonNull(notifier, "notifier");
    }

    @Override
    protected List<MenuOption> options() {
        return List.of(
            new MenuOption("Registrar una venta", this::register),
            new MenuOption("Ver las ventas de un cliente", this::listByCustomer));
    }

    private void register() throws ValidationException, BusinessRuleException, DataAccessException {
        Customer customer = customers.findById(console().readInt("Id del cliente"));
        Sale sale = new Sale(customer);
        fillCart(sale);
        if (sale.getLines().isEmpty()) {
            console().println("La venta no tiene ítems; no se registró nada.");
            return;
        }
        showCart(sale);
        if (!console().confirm("¿Registrar la venta?")) {
            console().println("Venta cancelada; el stock no cambió.");
            return;
        }
        Sale registered = sales.register(sale);
        console().println("Venta registrada: " + ConsoleFormat.sale(registered));
        notifier.sendInvoice(customer, registered);
    }

    private void fillCart(Sale sale) throws DataAccessException {
        int type;
        do {
            type = console().readInt("Agregar al carrito: 1 producto, 2 tratamiento, 0 terminar");
            if (type == PRODUCT || type == TREATMENT) {
                addItem(sale, type);
            } else if (type != FINISH) {
                console().println("Opción no válida.");
            }
        } while (type != FINISH);
    }

    private void addItem(Sale sale, int type) throws DataAccessException {
        try {
            if (type == PRODUCT) {
                products.listActive().forEach(product -> console().println("  " + ConsoleFormat.product(product)));
                int id = console().readInt("Id del producto");
                sale.addLine(products.findById(id), console().readInt("Cantidad"));
            } else {
                treatments.listActive().forEach(treatment ->
                    console().println("  " + ConsoleFormat.treatment(treatment)));
                int id = console().readInt("Id del tratamiento");
                sale.addLine(treatments.findById(id), console().readInt("Cantidad"));
            }
            showCart(sale);
        } catch (BusinessRuleException e) {
            console().println("No se agregó el ítem: " + e.getMessage());
        }
    }

    private void showCart(Sale sale) {
        console().println("Carrito:");
        sale.getLines().forEach(line -> console().println("  " + ConsoleFormat.saleLine(line)));
        console().println("  Total: " + ConsoleFormat.money(sale.calculateTotal()));
    }

    private void listByCustomer() throws ValidationException, DataAccessException {
        List<Sale> found = sales.listByCustomer(console().readInt("Id del cliente"));
        if (found.isEmpty()) {
            console().println("Ese cliente no tiene ventas.");
            return;
        }
        for (Sale sale : found) {
            console().println("  " + ConsoleFormat.sale(sale));
            sale.getLines().forEach(line -> console().println("      " + ConsoleFormat.saleLine(line)));
        }
    }
}
