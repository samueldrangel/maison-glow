package com.maisonglow.view;

import com.maisonglow.AppContext;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Main window tests")
class MainViewTest {

    private static boolean toolkitAvailable;

    @TempDir
    Path data;

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // The toolkit is shared by every test of the run.
        } catch (UnsupportedOperationException noDisplay) {
            toolkitAvailable = false;
            return;
        }
        toolkitAvailable = true;
    }

    private static <T> T onFxThread(Supplier<T> action) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(action.get());
            } catch (RuntimeException e) {
                result.completeExceptionally(e);
            }
        });
        return result.get(10, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("keeps the palette when the view sits inside another container, as in Scene Builder")
    void keepsPaletteInsideAnotherContainer() throws Exception {
        assumeTrue(toolkitAvailable, "JavaFX toolkit needs a display");
        AppContext.seed(data);
        AppContext context = new AppContext(data);

        Region sidebar = onFxThread(() -> {
            Parent root = ViewLoader.load(MainViewTest.class.getResource("/com/maisonglow/view/MainView.fxml"),
                context);
            Scene scene = new Scene(new StackPane(root));
            scene.getRoot().applyCss();
            scene.getRoot().layout();
            return (Region) root.lookup(".sidebar");
        });

        assertThat(sidebar.getBackground().getFills().get(0).getFill()).isEqualTo(Color.web("#b08187"));
    }

    @Test
    @DisplayName("starts collapsed and expands while the cursor is over the sidebar")
    void expandsWithTheCursor() throws Exception {
        assumeTrue(toolkitAvailable, "JavaFX toolkit needs a display");
        AppContext.seed(data);
        AppContext context = new AppContext(data);
        Parent root = onFxThread(() -> {
            Parent loaded = ViewLoader.load(MainViewTest.class.getResource("/com/maisonglow/view/MainView.fxml"),
                context);
            new Scene(loaded, 1100, 720);
            return loaded;
        });
        Region sidebar = (Region) root.lookup(".sidebar");
        StackPane content = (StackPane) root.lookup(".content-area");

        double collapsed = onFxThread(() -> widthOf(sidebar, root));
        double contentLeft = onFxThread(() -> content.getBoundsInParent().getMinX());
        double hovered = onFxThread(() -> {
            setHover(sidebar, true);
            return widthOf(sidebar, root);
        });
        double contentLeftWhileHovered = onFxThread(() -> content.getBoundsInParent().getMinX());
        double left = onFxThread(() -> {
            setHover(sidebar, false);
            return widthOf(sidebar, root);
        });

        assertThat(collapsed).isEqualTo(76);
        assertThat(hovered).isEqualTo(240);
        assertThat(left).isEqualTo(76);
        assertThat(contentLeftWhileHovered).as("the sidebar floats, it does not push the content")
            .isEqualTo(contentLeft);
    }

    private static double widthOf(Region sidebar, Parent root) {
        root.applyCss();
        root.layout();
        return sidebar.getWidth();
    }

    /** JavaFX only sets :hover from real mouse events, so the test sets it the same way the scene does. */
    private static void setHover(Node node, boolean hover) {
        try {
            Method setter = Node.class.getDeclaredMethod("setHover", boolean.class);
            setter.setAccessible(true);
            setter.invoke(node, hover);
        } catch (ReflectiveOperationException | RuntimeException e) {
            assumeTrue(false, "cannot simulate the cursor: " + e);
        }
    }

    @Test
    @DisplayName("opens on Inicio and walks every menu entry without errors")
    void walksEveryMenuEntry() throws Exception {
        assumeTrue(toolkitAvailable, "JavaFX toolkit needs a display");
        AppContext.seed(data);
        AppContext context = new AppContext(data);

        Parent root = onFxThread(() -> ViewLoader.load(MainViewTest.class.getResource(
            "/com/maisonglow/view/MainView.fxml"), context));

        StackPane content = (StackPane) root.lookup(".content-area");
        assertThat(content.getChildren()).hasSize(1);
        for (ToggleButton entry : root.lookupAll(".nav-button").stream().filter(ToggleButton.class::isInstance)
            .map(ToggleButton.class::cast).toList()) {
            onFxThread(() -> {
                entry.fire();
                return null;
            });
            assertThat(content.getChildren()).hasSize(1);
            assertThat(entry.isSelected()).isTrue();
        }
    }
}
