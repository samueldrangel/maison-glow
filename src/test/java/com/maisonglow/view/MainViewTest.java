package com.maisonglow.view;

import com.maisonglow.AppContext;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
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
        onFxThread(() -> {
            setHover(sidebar, true);
            return null;
        });
        double hovered = await(() -> widthOf(sidebar, root), width -> width == 240);
        double contentLeftWhileHovered = onFxThread(() -> content.getBoundsInParent().getMinX());
        onFxThread(() -> {
            setHover(sidebar, false);
            return null;
        });
        double left = await(() -> widthOf(sidebar, root), width -> width == 76);

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
    @DisplayName("the sidebar grows smoothly instead of jumping to its final width")
    void growsSmoothly() throws Exception {
        assumeTrue(toolkitAvailable, "JavaFX toolkit needs a display");
        Parent root = shownMainView();
        Region sidebar = (Region) root.lookup(".sidebar");

        double justAfterEntering = onFxThread(() -> {
            setHover(sidebar, true);
            return widthOf(sidebar, root);
        });
        double finalWidth = await(() -> widthOf(sidebar, root), width -> width == 240);

        assertThat(justAfterEntering).as("it starts from the collapsed width").isLessThan(240);
        assertThat(finalWidth).isEqualTo(240);
    }

    @Test
    @DisplayName("shows the sub-entries of Citas only while one of them is the current screen")
    void opensTheGroupOfTheCurrentScreen() throws Exception {
        assumeTrue(toolkitAvailable, "JavaFX toolkit needs a display");
        Parent root = shownMainView();
        VBox subEntries = (VBox) root.lookup(".nav-sub-list");
        ToggleButton agenda = entry(root, "Agenda");
        ToggleButton customers = entry(root, "Clientes");

        assertThat(onFxThread(subEntries::isVisible)).as("closed while on Inicio").isFalse();

        onFxThread(() -> {
            agenda.fire();
            return null;
        });
        assertThat(await(subEntries::isVisible, visible -> visible)).as("open on Agenda").isTrue();
        assertThat(await(subEntries::getMaxHeight, height -> height < 0)).as("fully open").isNegative();

        onFxThread(() -> {
            customers.fire();
            return null;
        });
        assertThat(await(subEntries::isVisible, visible -> !visible)).as("closed again on Clientes").isFalse();
    }

    @Test
    @DisplayName("the Citas header leads to Agenda")
    void theHeaderLeadsToTheFirstSubEntry() throws Exception {
        assumeTrue(toolkitAvailable, "JavaFX toolkit needs a display");
        Parent root = shownMainView();
        Button header = (Button) root.lookup(".nav-group");
        ToggleButton agenda = entry(root, "Agenda");

        onFxThread(() -> {
            header.fire();
            return null;
        });

        Toggle selected = onFxThread(() -> agenda.getToggleGroup().getSelectedToggle());
        assertThat(((Node) selected).getUserData()).isEqualTo("APPOINTMENTS");
    }

    @Test
    @DisplayName("shows the logo in the sidebar")
    void showsTheLogos() throws Exception {
        assumeTrue(toolkitAvailable, "JavaFX toolkit needs a display");
        Parent root = shownMainView();

        assertThat(((ImageView) root.lookup("#logoFull")).getImage().isError()).isFalse();
        assertThat(((ImageView) root.lookup("#logoMark")).getImage().isError()).isFalse();
    }

    private Parent shownMainView() throws Exception {
        AppContext.seed(data);
        AppContext context = new AppContext(data);
        return onFxThread(() -> {
            Parent loaded = ViewLoader.load(MainViewTest.class.getResource("/com/maisonglow/view/MainView.fxml"),
                context);
            new Scene(loaded, 1100, 720);
            loaded.applyCss();
            loaded.layout();
            return loaded;
        });
    }

    private static ToggleButton entry(Parent root, String text) {
        return root.lookupAll(".nav-button").stream().filter(ToggleButton.class::isInstance)
            .map(ToggleButton.class::cast).filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
    }

    /** Reads a value on the JavaFX thread until it satisfies the condition (the motions take a moment). */
    private static <T> T await(Supplier<T> reading, Predicate<T> done) throws Exception {
        T value = onFxThread(reading);
        for (int attempt = 0; attempt < 40 && !done.test(value); attempt++) {
            Thread.sleep(50);
            value = onFxThread(reading);
        }
        return value;
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
