package com.maisonglow.ai;

import com.maisonglow.service.AppointmentService;
import com.maisonglow.service.CustomerService;
import com.maisonglow.service.ProductService;
import com.maisonglow.service.ProfessionalService;
import com.maisonglow.service.TreatmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AiProviderFactoryTest {
    @TempDir Path directory;
    private final CustomerService customers = mock(CustomerService.class);
    private final ProductService products = mock(ProductService.class);
    private final AppointmentService appointments = mock(AppointmentService.class);
    private final ProfessionalService professionals = mock(ProfessionalService.class);
    private final TreatmentService treatments = mock(TreatmentService.class);

    private AiConfig config(String contents, Map<String, String> environment) throws Exception {
        Path file = directory.resolve("config.properties");
        Files.writeString(file, contents);
        return AiConfig.load(file, environment);
    }

    private AiModule create(AiProviderFactory factory) {
        return factory.create(customers, products, appointments, professionals, treatments);
    }

    @Test
    void absentFileStartsWithBothSimulatedServices() {
        AiConfig config = AiConfig.load(directory.resolve("missing.properties"), Map.of());
        AiModule module = create(new AiProviderFactory(config));
        assertThat(config.provider()).isEqualTo(AiConfig.Provider.SIMULATED);
        assertThat(module.isSimulated()).isTrue();
        assertThat(module.assistant()).isInstanceOf(SimulatedAssistant.class);
        assertThat(module.tryOn()).isInstanceOf(SimulatedTryOnService.class);
        assertThat(module.assistant().reply("ayuda").text()).contains("find_customer", "book_appointment");
    }

    @Test
    void keyIsReadOnlyFromEnvironmentAndNeverPrinted() throws Exception {
        AiConfig noKey = config("ai.provider=api\nai.api_key_env=TEST_AI_KEY\n", Map.of());
        assertThat(noKey.provider()).isEqualTo(AiConfig.Provider.SIMULATED);
        AiConfig configured = AiConfig.load(directory.resolve("config.properties"),
            Map.of("TEST_AI_KEY", "secret-for-test"));
        assertThat(configured.provider()).isEqualTo(AiConfig.Provider.API);
        assertThat(configured.apiKey()).isEqualTo("secret-for-test");
        assertThat(configured.toString()).doesNotContain("secret-for-test");
        assertThat(Files.readString(directory.resolve("config.properties"))).doesNotContain("secret-for-test");
    }

    @Test
    void configuredApiSelectsBothAdapters() throws Exception {
        AiConfig config = config("ai.provider=api\n", Map.of("MAISON_GLOW_AI_API_KEY", "test-key"));
        SmartAssistant assistant = mock(SmartAssistant.class);
        TryOnService tryOn = mock(TryOnService.class);
        AiModule module = create(new AiProviderFactory(config, () -> true, () -> assistant, () -> tryOn));
        assertThat(module.isSimulated()).isFalse();
        assertThat(module.assistant()).isSameAs(assistant);
        assertThat(module.tryOn()).isSameAs(tryOn);
    }

    @Test
    void absentAdaptersOrNetworkFallsBackWithoutPartialApiModule() throws Exception {
        AiConfig config = config("ai.provider=api\n", Map.of("MAISON_GLOW_AI_API_KEY", "test-key"));
        assertThat(create(new AiProviderFactory(config, () -> true, null, null)).isSimulated()).isTrue();
        assertThat(create(new AiProviderFactory(config, () -> false,
            () -> mock(SmartAssistant.class), () -> mock(TryOnService.class))).isSimulated()).isTrue();
        assertThat(create(new AiProviderFactory(config, () -> true,
            () -> mock(SmartAssistant.class), () -> null)).isSimulated()).isTrue();
        assertThat(create(new AiProviderFactory(config, () -> { throw new IllegalStateException("offline"); },
            () -> mock(SmartAssistant.class), () -> mock(TryOnService.class))).isSimulated()).isTrue();
    }

    @Test
    void invalidProviderAndMissingKeyNameStaySimulated() throws Exception {
        assertThat(config("ai.provider=unknown\n", Map.of()).provider())
            .isEqualTo(AiConfig.Provider.SIMULATED);
        assertThat(config("ai.provider=api\nai.api_key_env= \n", Map.of()).provider())
            .isEqualTo(AiConfig.Provider.SIMULATED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void productionFactoryWiresRealPairWithBoundedProbeAndFallsBackOnFailure() throws Exception {
        AiConfig config = config("ai.provider=api\n", Map.of("MAISON_GLOW_AI_API_KEY", "test-key"));
        HttpClient client = mock(HttpClient.class);
        when(client.followRedirects()).thenReturn(HttpClient.Redirect.NEVER);
        HttpResponse<Void> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        AiModule module = create(new AiProviderFactory(config, client));
        assertThat(module.isSimulated()).isFalse();
        assertThat(module.assistant()).isInstanceOf(ApiAssistant.class);
        assertThat(module.tryOn()).isInstanceOf(ApiTryOnService.class);
        var captured = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(captured.capture(), any(HttpResponse.BodyHandler.class));
        assertThat(captured.getValue().method()).isEqualTo("GET");
        assertThat(captured.getValue().timeout()).contains(java.time.Duration.ofSeconds(3));
        when(response.statusCode()).thenReturn(401);
        assertThat(create(new AiProviderFactory(config, client)).isSimulated()).isTrue();
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new HttpTimeoutException("offline"));
        assertThat(create(new AiProviderFactory(config, client)).isSimulated()).isTrue();
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new InterruptedException("cancelled"));
        try {
            assertThat(create(new AiProviderFactory(config, client)).isSimulated()).isTrue();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally { Thread.interrupted(); }
    }

    @Test
    void simulatedConfigurationNeverContactsProvider() {
        HttpClient client = mock(HttpClient.class);
        assertThat(create(new AiProviderFactory(AiConfig.simulated(), client)).isSimulated()).isTrue();
        verifyNoInteractions(client);
    }
}
