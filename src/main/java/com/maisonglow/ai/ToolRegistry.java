package com.maisonglow.ai;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.util.Validator;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Fixed whitelist and confirmation boundary shared by simulated and API assistants. */
public final class ToolRegistry {
    private final Map<String, AssistantTool> tools = new LinkedHashMap<>();
    private final Map<UUID, PendingAction> pending = new LinkedHashMap<>();

    /** Builds a whitelist; duplicate names are rejected instead of replacing a tool. */
    public ToolRegistry(Collection<? extends AssistantTool> tools) {
        for (AssistantTool tool : tools) {
            if (this.tools.putIfAbsent(tool.getName(), tool) != null) {
                throw new IllegalArgumentException("Herramienta duplicada: " + tool.getName());
            }
        }
    }

    /** Returns registered tool metadata in registration order. */
    public List<AssistantTool> getTools() {
        return List.copyOf(tools.values());
    }

    /** Executes a query or returns an immutable proposal for a modifying operation. */
    public synchronized AssistantReply execute(String name, Map<String, String> parameters)
        throws ValidationException, BusinessRuleException, DataAccessException {
        AssistantTool tool = tools.get(name);
        if (tool == null) {
            throw new ValidationException("Herramienta no registrada: " + name + ".");
        }
        Validator.requirePresent(parameters, "parámetros");
        for (var entry : parameters.entrySet()) {
            Validator.requireText(entry.getKey(), "nombre del parámetro");
            if (!tool.getParameters().containsKey(entry.getKey())) {
                throw new ValidationException("Parámetro desconocido: " + entry.getKey() + ".");
            }
            Validator.requireText(entry.getValue(), entry.getKey());
        }
        Map<String, String> snapshot = Map.copyOf(parameters);
        if (!tool.requiresConfirmation()) {
            return new AssistantReply(tool.execute(snapshot));
        }
        PendingAction action = new PendingAction(UUID.randomUUID(), name, snapshot);
        pending.put(action.id(), action);
        return new AssistantReply("Confirma la operación: " + tool.getDescription() + " " + snapshot,
            Optional.of(action));
    }

    /** Consumes only an unchanged proposal issued here; failed operations cannot be replayed. */
    public synchronized AssistantReply confirm(PendingAction action)
        throws ValidationException, BusinessRuleException, DataAccessException {
        PendingAction issued = requirePending(action);
        pending.remove(issued.id());
        return new AssistantReply(tools.get(issued.toolName()).execute(issued.parameters()));
    }

    /** Consumes a proposal without invoking its tool. */
    public synchronized AssistantReply reject(PendingAction action) throws ValidationException {
        PendingAction issued = requirePending(action);
        pending.remove(issued.id());
        return new AssistantReply("Operación cancelada. No se hicieron cambios.");
    }

    private PendingAction requirePending(PendingAction action) throws ValidationException {
        if (action == null || !action.equals(pending.get(action.id()))) {
            throw new ValidationException("La acción no está pendiente en este asistente.");
        }
        return pending.get(action.id());
    }
}
