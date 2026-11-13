package com.maisonglow.ai;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Immutable snapshot of a proposed tool call; its id belongs to one registry. */
public record PendingAction(UUID id, String toolName, Map<String, String> parameters) {
    /** Copies parameters so that a displayed proposal cannot change before confirmation. */
    public PendingAction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(toolName, "toolName");
        parameters = Map.copyOf(parameters);
    }
}
