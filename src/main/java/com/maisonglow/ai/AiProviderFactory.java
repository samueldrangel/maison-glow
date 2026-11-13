package com.maisonglow.ai;

import com.maisonglow.service.AppointmentService;
import com.maisonglow.service.CustomerService;
import com.maisonglow.service.ProductService;
import com.maisonglow.service.ProfessionalService;
import com.maisonglow.service.TreatmentService;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Chooses a complete API pair when available, otherwise wires the offline pair. */
public final class AiProviderFactory {
    private final AiConfig config;
    private final BooleanSupplier apiReachable;
    private final Supplier<SmartAssistant> apiAssistant;
    private final Supplier<TryOnService> apiTryOn;

    /** Creates the offline-capable factory used until both API adapters exist. */
    public AiProviderFactory(AiConfig config) {
        this(config, () -> false, null, null);
    }

    /** Accepts the future API adapters and a bounded connectivity probe. */
    public AiProviderFactory(AiConfig config, BooleanSupplier apiReachable,
                             Supplier<SmartAssistant> apiAssistant, Supplier<TryOnService> apiTryOn) {
        this.config = Objects.requireNonNull(config, "config");
        this.apiReachable = Objects.requireNonNull(apiReachable, "apiReachable");
        this.apiAssistant = apiAssistant;
        this.apiTryOn = apiTryOn;
    }

    /** Wires registered service tools and selects the provider for both features. */
    public AiModule create(CustomerService customers, ProductService products,
                           AppointmentService appointments, ProfessionalService professionals,
                           TreatmentService treatments) {
        Objects.requireNonNull(customers, "customers");
        Objects.requireNonNull(products, "products");
        Objects.requireNonNull(appointments, "appointments");
        Objects.requireNonNull(professionals, "professionals");
        Objects.requireNonNull(treatments, "treatments");
        if (config.provider() == AiConfig.Provider.API && apiAssistant != null && apiTryOn != null) {
            try {
                if (apiReachable.getAsBoolean()) {
                    return new AiModule(apiAssistant.get(), apiTryOn.get(), false);
                }
            } catch (RuntimeException e) {
                // Connectivity or adapter startup failure keeps the business application usable.
            }
        }
        ToolRegistry registry = new ToolRegistry(List.of(new FindCustomerTool(customers),
            new ListLowStockTool(products), new ListAppointmentsByDateTool(appointments),
            new CheckAvailabilityTool(appointments, professionals),
            new BookAppointmentTool(appointments, customers, professionals, treatments)));
        return new AiModule(new SimulatedAssistant(registry), new SimulatedTryOnService(customers), true);
    }
}
