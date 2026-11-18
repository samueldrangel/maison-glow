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
        Navigator navigator = new Navigator(content, com.maisonglow.TestAccounts.open(data, Clock.systemDefaultZone(), AiConfig.simulated()), com.maisonglow.view.access.AccessRole.ADMIN);

        FxTest.onFxThread(() -> { navigator.show(View.ASSISTANT); return null; });
        Node chat = shown(content);
        FxTest.onFxThread(() -> { navigator.show(View.HOME); return null; });
        Node home = shown(content);
        FxTest.onFxThread(() -> { navigator.show(View.ASSISTANT); return null; });

        assertThat(shown(content)).isSameAs(chat);
        FxTest.onFxThread(() -> { navigator.show(View.HOME); return null; });
        assertThat(shown(content)).isNotSameAs(home);
    }

    @Test
    @DisplayName("every screen belongs to exactly one role, and each role starts on a screen it may open")
    void everyScreenHasOneRole() {
        for (View view : View.values()) {
            long roles = java.util.Arrays.stream(com.maisonglow.view.access.AccessRole.values()).filter(view::allowedFor).count();
            assertThat(roles).as(view.name()).isEqualTo(1);
        }
        for (com.maisonglow.view.access.AccessRole role : com.maisonglow.view.access.AccessRole.values()) {
            assertThat(View.startFor(role).allowedFor(role)).isTrue();
        }
    }

    @Test
    @DisplayName("a screen of the other role is refused when asked for directly: nothing is loaded and the area says so")
    void refusesAScreenOfAnotherRole() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        AppContext.seed(data);
        StackPane content = FxTest.onFxThread(StackPane::new);
        AppContext context = com.maisonglow.TestAccounts.open(data, Clock.systemDefaultZone(), AiConfig.simulated());
        Navigator asCustomer = new Navigator(content, context, com.maisonglow.view.access.AccessRole.CUSTOMER);

        boolean opened = FxTest.onFxThread(() -> asCustomer.show(View.PRODUCTS));

        assertThat(opened).isFalse();
        assertThat(asCustomer.current()).isEmpty();
        assertThat(FxTest.onFxThread(() -> content.lookupAll(".view-title").stream().map(node -> ((javafx.scene.control.Label) node).getText()).toList()))
            .containsExactly("Sin permiso");
        assertThat(FxTest.onFxThread(() -> asCustomer.show(View.CUSTOMER_HOME))).isTrue();
        Navigator asAdmin = new Navigator(content, context, com.maisonglow.view.access.AccessRole.ADMIN);
        assertThat(FxTest.onFxThread(() -> asAdmin.show(View.SHOP))).isFalse();
        assertThat(FxTest.onFxThread(() -> asAdmin.show(View.SETTINGS))).isTrue();
    }
}

