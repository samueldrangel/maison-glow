package com.maisonglow.ai;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.service.CustomerService;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Searches registered customers by name using CustomerService. */
public final class FindCustomerTool implements AssistantTool {
    private final CustomerService customers;

    /** Creates a read-only customer tool. */
    public FindCustomerTool(CustomerService customers) {
        this.customers = Objects.requireNonNull(customers, "customers");
    }

    @Override public String getName() { return "find_customer"; }
    @Override public String getDescription() { return "Buscar clientes por nombre"; }
    @Override public Map<String, String> getParameters() { return Map.of("name", "Nombre o parte del nombre"); }
    @Override public boolean requiresConfirmation() { return false; }

    @Override
    public String execute(Map<String, String> parameters) throws ValidationException, DataAccessException {
        var found = customers.searchByName(ToolArguments.text(parameters, "name"));
        return found.isEmpty() ? "No se encontraron clientes."
            : found.stream().map(customer -> "[" + customer.getId() + "] " + customer.getName())
                .collect(Collectors.joining("\n"));
    }
}
