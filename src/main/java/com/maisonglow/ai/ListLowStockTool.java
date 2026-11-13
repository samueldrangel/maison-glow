package com.maisonglow.ai;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.service.ProductService;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Lists low-stock products using the inventory service's existing threshold. */
public final class ListLowStockTool implements AssistantTool {
    private final ProductService products;

    /** Creates a read-only stock tool. */
    public ListLowStockTool(ProductService products) {
        this.products = Objects.requireNonNull(products, "products");
    }

    @Override public String getName() { return "list_low_stock"; }
    @Override public String getDescription() { return "Consultar productos con stock bajo"; }
    @Override public Map<String, String> getParameters() { return Map.of(); }
    @Override public boolean requiresConfirmation() { return false; }

    @Override
    public String execute(Map<String, String> parameters) throws DataAccessException {
        var found = products.listLowStock();
        return found.isEmpty() ? "No hay productos con stock bajo."
            : found.stream().map(product -> "[" + product.getId() + "] " + product.getName()
                + ": " + product.getStock() + " unidades (mínimo " + product.getMinimumStock() + ")")
                .collect(Collectors.joining("\n"));
    }
}
