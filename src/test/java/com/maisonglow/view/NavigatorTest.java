package com.maisonglow.view;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Navigator tests")
class NavigatorTest {

    private static boolean toolkit;

    @TempDir
    Path data;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    private Node shown(StackPane content) throws Exception {
        return FxTest.onFxThread(() -> content.getChildren().get(0));
    }

    @Test
    @DisplayName("the chat and the try-on come back as they were left; the other screens are loaded again")
    void keepsTheScreensThatHoldWhatTheUserDid() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        AppContext.seed(data);
        StackPane content = FxTest.onFxThread(StackPane::new);
        Navigator navigator = new Navigator(content, new AppContext(data, Clock.systemDefaultZone(), AiConfig.simulated()));

        FxTest.onFxThread(() -> { navigator.show(View.ASSISTANT); return null; });
        Node chat = shown(content);
        FxTest.onFxThread(() -> { navigator.show(View.HOME); return null; });
        Node home = shown(content);
        FxTest.onFxThread(() -> { navigator.show(View.ASSISTANT); return null; });

        assertThat(shown(content)).isSameAs(chat);
        FxTest.onFxThread(() -> { navigator.show(View.HOME); return null; });
        assertThat(shown(content)).isNotSameAs(home);
    }
}
