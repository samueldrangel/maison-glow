package com.maisonglow.ai;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.util.Validator;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Deterministic offline assistant with small Spanish commands and explicit tool syntax. */
public final class SimulatedAssistant implements SmartAssistant {
    private final ToolRegistry registry;

    /** Creates a session backed only by the supplied whitelist, without network access. */
    public SimulatedAssistant(ToolRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public AssistantReply reply(String message) {
        try {
            Validator.requireText(message, "mensaje");
            String trimmed = message.trim();
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.equals("ayuda") || lower.equals("hola")) {
                String help = registry.getTools().stream().map(tool -> tool.getName() + ": "
                    + tool.getDescription() + " " + tool.getParameters()).collect(Collectors.joining("\n"));
                return new AssistantReply("Asistente simulado. Usa herramienta parámetro=valor; parámetro=valor.\n" + help);
            }
            if (lower.equals("stock bajo")) return registry.execute("list_low_stock", Map.of());
            if (lower.startsWith("buscar cliente ")) {
                return registry.execute("find_customer", Map.of("name", trimmed.substring(15).trim()));
            }
            if (lower.startsWith("citas ")) {
                return registry.execute("list_appointments_by_date", Map.of("date", trimmed.substring(6).trim()));
            }
            String[] parts = trimmed.split("\\s+", 2);
            return registry.execute(parts[0], parseParameters(parts.length == 2 ? parts[1] : ""));
        } catch (ValidationException | BusinessRuleException e) {
            return new AssistantReply(e.getMessage());
        } catch (DataAccessException e) {
            return storageError();
        }
    }

    @Override
    public AssistantReply confirm(PendingAction action) {
        try {
            return registry.confirm(action);
        } catch (ValidationException | BusinessRuleException e) {
            return new AssistantReply(e.getMessage());
        } catch (DataAccessException e) {
            return storageError();
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

    private Map<String, String> parseParameters(String text) throws ValidationException {
        Map<String, String> parameters = new LinkedHashMap<>();
        if (text.isBlank()) return parameters;
        for (String item : text.split(";", -1)) {
            String[] pair = item.trim().split("=", 2);
            if (pair.length != 2 || pair[0].isBlank() || pair[1].isBlank()) {
                throw new ValidationException("Usa parámetros con formato nombre=valor, separados por punto y coma.");
            }
            if (parameters.putIfAbsent(pair[0].trim(), pair[1].trim()) != null) {
                throw new ValidationException("Parámetro repetido: " + pair[0].trim() + ".");
            }
        }
        return parameters;
    }

    private AssistantReply storageError() {
        return new AssistantReply("No se pudo consultar o guardar la información. Revisa los datos antes de intentar de nuevo.");
    }
}
