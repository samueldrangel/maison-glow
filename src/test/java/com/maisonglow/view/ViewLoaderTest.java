package com.maisonglow.view;

import com.maisonglow.AppContext;
import com.maisonglow.view.controller.MainController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("ViewLoader tests")
class ViewLoaderTest {

    private static boolean toolkit;

    @TempDir
    Path data;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @Test
    @DisplayName("returns the controller built for the screen along with its root")
    void returnsTheController() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        AppContext.seed(data);
        AppContext context = com.maisonglow.TestAccounts.open(data);

        ViewLoader.Loaded<MainController> loaded = FxTest.onFxThread(() ->
            ViewLoader.loadWithController(ViewLoaderTest.class.getResource("/com/maisonglow/view/MainView.fxml"), context));

        assertThat(loaded.controller()).isInstanceOf(MainController.class);
        assertThat(loaded.root().lookup(".sidebar")).isNotNull();
    }
}
