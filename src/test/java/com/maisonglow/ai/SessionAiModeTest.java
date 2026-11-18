package com.maisonglow.ai;

import com.maisonglow.AppContext;
import com.maisonglow.TestAccounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Mode of the AI provider seen by the screens")
class SessionAiModeTest {

    private static final char[] PASSWORD = "test-only-administrator".toCharArray();

    @TempDir
    Path data;

    private AppContext context;

    @BeforeEach
    void open() throws Exception {
        AppContext.seed(data);
        context = TestAccounts.open(data);
    }

    private static SmartAssistant assistant() {
        return new SmartAssistant() {
            public AssistantReply reply(String message) {
                return new AssistantReply("respuesta");
            }

            public AssistantReply confirm(PendingAction action) {
                return new AssistantReply("ok");
            }

            public AssistantReply reject(PendingAction action) {
                return new AssistantReply("ok");
            }
        };
    }

    private static TryOnService tryOn() {
        return request -> {
            throw new UnsupportedOperationException("no se usa");
        };
    }

    private AiModule module(boolean configuredSimulated, boolean providerSimulated) {
        return new SessionAi(context.auth(), () -> new AiModule(assistant(), tryOn(), providerSimulated),
            () -> new AiModule(assistant(), tryOn(), providerSimulated), () -> configuredSimulated).module();
    }

    @Test
    @DisplayName("with no API provider configured the screens say simulated, before and after the first call")
    void simulatedWhenConfiguredSimulated() {
        AiModule ai = module(true, true);

        assertThat(ai.isSimulated()).isTrue();
        ai.assistant().reply("hola");
        assertThat(ai.isSimulated()).isTrue();
    }

    @Test
    @DisplayName("with an API provider configured the notice does not show before the first call, which is when the provider is built")
    void notSimulatedBeforeTheFirstCallWhenApiIsConfigured() {
        AiModule ai = module(false, false);

        assertThat(ai.isSimulated()).isFalse();
        ai.assistant().reply("hola");
        assertThat(ai.isSimulated()).isFalse();
    }

    @Test
    @DisplayName("an API provider that could not be reached falls back to simulation, and the mode shows it after the first call")
    void fallbackIsShownAfterTheFirstCall() {
        AiModule ai = module(false, true);
        assertThat(ai.isSimulated()).isFalse();

        ai.assistant().reply("hola");

        assertThat(ai.isSimulated()).isTrue();
    }

    @Test
    @DisplayName("a new session goes back to what the configuration says until its own provider is built")
    void newSessionStartsFromTheConfiguration() throws Exception {
        AiModule ai = module(false, true);
        ai.assistant().reply("hola");
        assertThat(ai.isSimulated()).isTrue();

        context.auth().logout();
        context.auth().login("fixture-admin", PASSWORD);

        assertThat(ai.isSimulated()).isFalse();
    }

    @Test
    @DisplayName("with no session the mode is the configured one")
    void noSession() {
        AiModule ai = module(false, true);
        context.auth().logout();

        assertThat(ai.isSimulated()).isFalse();
    }
}
