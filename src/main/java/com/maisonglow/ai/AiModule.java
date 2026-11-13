package com.maisonglow.ai;

import java.util.Objects;

/** The two AI services exposed to the future application context and views. */
public record AiModule(SmartAssistant assistant, TryOnService tryOn, boolean isSimulated) {
    /** Requires both provider implementations to be available together. */
    public AiModule {
        Objects.requireNonNull(assistant, "assistant");
        Objects.requireNonNull(tryOn, "tryOn");
    }
}
