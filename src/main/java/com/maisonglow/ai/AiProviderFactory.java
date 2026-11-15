package com.maisonglow.ai;

import com.maisonglow.service.AppointmentService;
import com.maisonglow.service.CustomerService;
import com.maisonglow.service.ProductService;
import com.maisonglow.service.ProfessionalService;
import com.maisonglow.service.TreatmentService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Chooses a complete API pair when available, otherwise wires the offline pair. */
public final class AiProviderFactory {
    private final AiConfig config;
    private final HttpClient client;
    private final BooleanSupplier apiReachable;
    private final Supplier<SmartAssistant> apiAssistant;
    private final Supplier<TryOnService> apiTryOn;

    /** Creates the production pair, with a bounded availability check and offline fallback. */
    public AiProviderFactory(AiConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER).build());
    }

    /** Injects HTTP transport for the availability check and both production adapters. */
    public AiProviderFactory(AiConfig config, HttpClient client) {
        this.config = Objects.requireNonNull(config, "config");
        this.client = Objects.requireNonNull(client, "client");
        this.apiReachable = this::isReachable;
        this.apiAssistant = null;
        this.apiTryOn = null;
    }

    /** Accepts an explicit adapter pair and a bounded connectivity probe. */
    public AiProviderFactory(AiConfig config, BooleanSupplier apiReachable,
                             Supplier<SmartAssistant> apiAssistant, Supplier<TryOnService> apiTryOn) {
        this.config = Objects.requireNonNull(config, "config");
        this.client = null;
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
        ToolRegistry registry = new ToolRegistry(List.of(new FindCustomerTool(customers),
            new ListLowStockTool(products), new ListAppointmentsByDateTool(appointments),
            new CheckAvailabilityTool(appointments, professionals),
            new BookAppointmentTool(appointments, customers, professionals, treatments)));
        if (config.provider() == AiConfig.Provider.API && (client != null || (apiAssistant != null && apiTryOn != null))) {
            try {
                if (apiReachable.getAsBoolean()) {
                    if (client != null) {
                        return new AiModule(new ApiAssistant(client, URI.create("https://api.openai.com/v1/responses"),
                            "gpt-5.4", config.apiKey(), registry),
                            new ApiTryOnService(customers, client, URI.create("https://api.openai.com/v1/images/edits"),
                                "gpt-image-1.5", config.apiKey()), false);
                    }
                    return new AiModule(apiAssistant.get(), apiTryOn.get(), false);
                }
            } catch (RuntimeException e) {
                // Connectivity or adapter startup failure keeps the business application usable.
            }
        }
        return new AiModule(new SimulatedAssistant(registry), new SimulatedTryOnService(customers), true);
    }

    private boolean isReachable() {
        if (client.followRedirects() != HttpClient.Redirect.NEVER) return false;
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/models/gpt-image-1.5"))
            .timeout(Duration.ofSeconds(3)).header("Authorization", "Bearer " + config.apiKey()).GET().build();
        try {
            return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException e) {
            return false;
        }
    }
}
