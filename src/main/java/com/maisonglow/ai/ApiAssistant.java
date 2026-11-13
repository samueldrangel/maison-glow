package com.maisonglow.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.util.Validator;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** OpenAI Responses adapter; all business operations stay behind ToolRegistry. */
public final class ApiAssistant implements SmartAssistant {
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final int MAX_ROUNDS = 3;
    private static final String CONNECTION_ERROR = "El asistente en línea no está disponible. Intenta de nuevo más tarde.";
    private final HttpClient client;
    private final URI endpoint;
    private final String model;
    private final String apiKey;
    private final ToolRegistry registry;
    private final ObjectMapper json = new ObjectMapper();

    /** Creates an adapter with injectable transport and endpoint for deterministic tests. */
    public ApiAssistant(HttpClient client, URI endpoint, String model, String apiKey, ToolRegistry registry) {
        this.client = Objects.requireNonNull(client, "client");
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        if (!"https".equalsIgnoreCase(endpoint.getScheme())
            || !"api.openai.com".equalsIgnoreCase(endpoint.getHost())) {
            throw new IllegalArgumentException("El endpoint debe ser api.openai.com por HTTPS.");
        }
        this.model = Objects.requireNonNull(model, "model");
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.registry = Objects.requireNonNull(registry, "registry");
        if (model.isBlank() || apiKey.isBlank()) throw new IllegalArgumentException("Modelo y clave son obligatorios.");
    }

    @Override
    public AssistantReply reply(String message) {
        try {
            Validator.requireText(message, "mensaje");
            return request(Map.of("model", model, "input", message.trim(), "tools", toolDefinitions()), 0);
        } catch (ValidationException | BusinessRuleException e) {
            return new AssistantReply(e.getMessage());
        } catch (IOException | InterruptedException | DataAccessException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return new AssistantReply(CONNECTION_ERROR);
        }
    }

    @Override
    public AssistantReply confirm(PendingAction action) {
        try {
            return registry.confirm(action);
        } catch (ValidationException | BusinessRuleException e) {
            return new AssistantReply(e.getMessage());
        } catch (DataAccessException e) {
            return new AssistantReply("No se pudo guardar la operación. Revisa los datos antes de intentar de nuevo.");
        }
    }

    @Override
    public AssistantReply reject(PendingAction action) {
        try {
            return registry.reject(action);
        } catch (ValidationException e) {
            return new AssistantReply(e.getMessage());
        }
    }

    private AssistantReply request(Map<String, ?> body, int round)
        throws IOException, InterruptedException, ValidationException, BusinessRuleException, DataAccessException {
        if (round >= MAX_ROUNDS) return new AssistantReply("La respuesta requiere demasiadas consultas. Reformula tu solicitud.");
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(TIMEOUT)
            .header("Authorization", "Bearer " + apiKey).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) return new AssistantReply(CONNECTION_ERROR);
        JsonNode root = json.readTree(response.body());
        JsonNode output = root.path("output");
        if (!output.isArray()) throw new IOException("Respuesta sin lista de salida.");
        List<Map<String, String>> results = new ArrayList<>();
        for (JsonNode item : output) {
            if ("function_call".equals(item.path("type").asText())) {
                String name = item.path("name").asText();
                String callId = item.path("call_id").asText();
                if (callId.isBlank()) throw new IOException("Llamada sin identificador.");
                Map<String, String> parameters = parseArguments(item.path("arguments").asText());
                AssistantReply toolResult = registry.execute(name, parameters);
                if (toolResult.pendingConfirmation().isPresent()) return toolResult;
                results.add(Map.of("type", "function_call_output", "call_id", callId,
                    "output", toolResult.text()));
            }
        }
        if (!results.isEmpty()) {
            String responseId = root.path("id").asText();
            if (responseId.isBlank()) throw new IOException("Respuesta sin identificador.");
            return request(Map.of("model", model, "previous_response_id", responseId, "input", results,
                "tools", toolDefinitions()), round + 1);
        }
        for (JsonNode item : output) {
            if ("message".equals(item.path("type").asText())) {
                for (JsonNode content : item.path("content")) {
                    if ("output_text".equals(content.path("type").asText())) {
                        return new AssistantReply(content.path("text").asText());
                    }
                }
            }
        }
        return new AssistantReply("El asistente no devolvió una respuesta de texto.");
    }

    private Map<String, String> parseArguments(String raw) throws IOException, ValidationException {
        JsonNode node = json.readTree(raw);
        if (node == null || !node.isObject()) throw new ValidationException("Parámetros de herramienta inválidos.");
        Map<String, String> values = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> values.put(entry.getKey(), entry.getValue().asText()));
        return values;
    }

    private List<Map<String, Object>> toolDefinitions() {
        List<Map<String, Object>> definitions = new ArrayList<>();
        for (AssistantTool tool : registry.getTools()) {
            Map<String, Object> properties = new LinkedHashMap<>();
            List<String> required = new ArrayList<>();
            tool.getParameters().forEach((name, description) -> {
                properties.put(name, Map.of("type", "string", "description", description));
                if (!description.toLowerCase(java.util.Locale.ROOT).contains("opcional")) required.add(name);
            });
            definitions.add(Map.of("type", "function", "name", tool.getName(),
                "description", tool.getDescription(), "parameters", Map.of(
                    "type", "object", "properties", properties, "required", required,
                    "additionalProperties", false)));
        }
        return definitions;
    }
}
