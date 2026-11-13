package com.maisonglow;

import com.maisonglow.ai.AiConfig;
import com.maisonglow.service.ContactData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@DisplayName("AppContext wiring tests")
class AppContextTest {

    @TempDir
    Path data;

    @BeforeEach
    void seedFolder() throws IOException {
        AppContext.seed(data);
    }

    @Test
    @DisplayName("seed copies the preloaded settings and categories")
    void seedCopiesPreloadedFiles() {
        assertThat(data.resolve("settings.txt")).exists();
        assertThat(data.resolve("categories.txt")).exists();
    }

    @Test
    @DisplayName("builds every service the views need")
    void buildsEveryService() {
        AppContext context = new AppContext(data, Clock.systemDefaultZone(), AiConfig.simulated());

        assertThat(context.customers()).isNotNull();
        assertThat(context.professionals()).isNotNull();
        assertThat(context.treatments()).isNotNull();
        assertThat(context.products()).isNotNull();
        assertThat(context.appointments()).isNotNull();
        assertThat(context.sales()).isNotNull();
        assertThat(context.settings()).isNotNull();
        assertThat(context.statistics()).isNotNull();
        assertThat(context.lowRotation()).isNotNull();
        assertThat(context.promotions()).isNotNull();
    }

    @Test
    @DisplayName("starts the AI module in simulated mode without configuration")
    void startsSimulatedAi() {
        AppContext context = new AppContext(data, Clock.systemDefaultZone(), AiConfig.simulated());

        assertThat(context.ai().isSimulated()).isTrue();
        assertThat(context.ai().assistant()).isNotNull();
        assertThat(context.ai().tryOn()).isNotNull();
    }

    @Test
    @DisplayName("keeps the data in the folder between contexts")
    void keepsDataBetweenContexts() throws Exception {
        new AppContext(data, Clock.systemDefaultZone(), AiConfig.simulated()).customers()
            .register(new ContactData("1065123456", "Ana Pérez", "3001234567", "ana@correo.com"));

        AppContext reopened = new AppContext(data, Clock.systemDefaultZone(), AiConfig.simulated());

        assertThat(reopened.customers().listAll()).hasSize(1);
    }

    @Test
    @DisplayName("rejects a missing data folder")
    void rejectsMissingFolder() {
        assertThatNullPointerException().isThrownBy(() -> new AppContext(null));
    }
}
