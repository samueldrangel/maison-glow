package com.maisonglow.ai;

import java.util.Objects;
import java.util.Optional;

/** Text for the chat and, when required, the action to show before confirmation. */
public record AssistantReply(String text, Optional<PendingAction> pendingConfirmation) {
    /** Creates a reply with non-null text and confirmation container. */
    public AssistantReply {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(pendingConfirmation, "pendingConfirmation");
    }

    /** Creates a reply that does not require confirmation. */
    public AssistantReply(String text) {
        this(text, Optional.empty());
    }
}
