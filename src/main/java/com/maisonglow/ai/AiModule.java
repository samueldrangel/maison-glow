package com.maisonglow.ai;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Provider pair with a live mode indicator for account-scoped, lazily initialized providers. */
public final class AiModule {
    private final SmartAssistant assistant;
    private final TryOnService tryOn;
    private final BooleanSupplier simulated;
    public AiModule(SmartAssistant assistant,TryOnService tryOn,boolean simulated) {this(assistant,tryOn,() -> simulated);}
    public AiModule(SmartAssistant assistant,TryOnService tryOn,BooleanSupplier simulated) {
        this.assistant=Objects.requireNonNull(assistant,"assistant");
        this.tryOn=Objects.requireNonNull(tryOn,"tryOn");
        this.simulated=Objects.requireNonNull(simulated,"simulated");
    }
    public SmartAssistant assistant(){return assistant;}
    public TryOnService tryOn(){return tryOn;}
    public boolean isSimulated(){return simulated.getAsBoolean();}
}
