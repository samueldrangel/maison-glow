package com.maisonglow.ai;

/** Provider-independent assistant contract for the future chat view. */
public interface SmartAssistant {
    /** Answers a message without executing changes that need confirmation. */
    AssistantReply reply(String message);

    /** Executes an action previously offered by this assistant, at most once. */
    AssistantReply confirm(PendingAction action);

    /** Discards an action without changing business data. */
    AssistantReply reject(PendingAction action);
}
