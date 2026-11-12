package com.maisonglow.ui.console;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Customer;
import com.maisonglow.service.CustomerService;
import java.util.List;
import java.util.Objects;

/**
 * Console module for customers: register, list, search, update and delete.
 * It only talks to the user and delegates every rule to {@link CustomerService}.
 */
public final class CustomerModule extends MenuModule {

    private static final String NONE_FOUND = "No hay clientes que mostrar.";

    private final CustomerService customers;

    /**
     * Creates the module.
     *
     * @param console   where the module talks to the user
     * @param customers the customer service
     */
    public CustomerModule(ConsoleIo console, CustomerService customers) {
        super("Clientes", console);
        this.customers = Objects.requireNonNull(customers, "customers");
    }

    @Override
    protected List<MenuOption> options() {
        return List.of(
            new MenuOption("Registrar cliente", this::register),
            new MenuOption("Listar todos los clientes", this::listAll),
            new MenuOption("Buscar por nombre", this::searchByName),
            new MenuOption("Buscar por documento", this::findByDocument),
            new MenuOption("Ver cliente por id", this::findById),
            new MenuOption("Actualizar datos de un cliente", this::update),
            new MenuOption("Registrar inasistencia", this::registerNoShow),
            new MenuOption("Eliminar cliente", this::delete));
    }

    private void register() throws ValidationException, BusinessRuleException, DataAccessException {
        Customer customer = customers.register(ContactPrompt.ask(console()));
        console().println("Cliente registrado: " + ConsoleFormat.customer(customer));
    }

    private void listAll() throws DataAccessException {
        ConsoleFormat.printAll(console(), NONE_FOUND,
            customers.listAll().stream().map(ConsoleFormat::customer).toList());
    }

    private void searchByName() throws ValidationException, DataAccessException {
        String text = console().readLine("Nombre o parte del nombre");
        ConsoleFormat.printAll(console(), NONE_FOUND,
            customers.searchByName(text).stream().map(ConsoleFormat::customer).toList());
    }

    private void findByDocument() throws DataAccessException {
        String document = console().readLine("Documento");
        customers.findByDocument(document.trim()).ifPresentOrElse(
            customer -> console().println(ConsoleFormat.customer(customer)),
            () -> console().println("No hay un cliente con ese documento."));
    }

    private void findById() throws BusinessRuleException, DataAccessException {
        console().println(ConsoleFormat.customer(customers.findById(console().readInt("Id del cliente"))));
    }

    private void update() throws ValidationException, BusinessRuleException, DataAccessException {
        int id = console().readInt("Id del cliente");
        console().println("Datos actuales: " + ConsoleFormat.customer(customers.findById(id)));
        console().println("Escriba los datos nuevos (el documento debe ser el mismo).");
        Customer customer = customers.update(id, ContactPrompt.ask(console()));
        console().println("Cliente actualizado: " + ConsoleFormat.customer(customer));
    }

    private void registerNoShow() throws BusinessRuleException, DataAccessException {
        int id = console().readInt("Id del cliente");
        customers.registerNoShow(id);
        console().println("Inasistencia registrada: " + ConsoleFormat.customer(customers.findById(id)));
    }

    private void delete() throws BusinessRuleException, DataAccessException {
        int id = console().readInt("Id del cliente");
        console().println(ConsoleFormat.customer(customers.findById(id)));
        if (console().confirm("¿Eliminar este cliente?")) {
            customers.delete(id);
            console().println("Cliente eliminado.");
        } else {
            console().println("No se eliminó nada.");
        }
    }
}
