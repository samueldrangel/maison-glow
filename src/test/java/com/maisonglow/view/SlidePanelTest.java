package com.maisonglow.view;

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("SlidePanel tests")
class SlidePanelTest {

    private static boolean toolkit;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    private static <T> T eventually(Supplier<T> reading, Predicate<T> done) throws Exception {
        T value = FxTest.onFxThread(reading);
        for (int attempt = 0; attempt < 80 && !done.test(value); attempt++) {
            Thread.sleep(25);
            value = FxTest.onFxThread(reading);
        }
        return value;
    }

    private ScrollPane panel;
    private SlidePanel slide;

    /** Runs what the user would do with the panel, on the FX thread. */
    private static void run(Runnable action) throws Exception {
        FxTest.onFxThread(() -> {
            action.run();
            return null;
        });
    }

    private void create() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        run(() -> {
            VBox content = new VBox();
            panel = new ScrollPane(content);
            slide = new SlidePanel(panel, content, 300);
        });
    }

    @Test
    @DisplayName("starts closed and takes no room in the layout")
    void startsClosed() throws Exception {
        create();

        assertThat(slide.isOpen()).isFalse();
        assertThat(panel.isVisible()).isFalse();
        assertThat(panel.isManaged()).isFalse();
    }

    @Test
    @DisplayName("opening shows it at once and grows it to its full width")
    void opens() throws Exception {
        create();

        run(slide::open);

        assertThat(slide.isOpen()).isTrue();
        assertThat(panel.isManaged()).isTrue();
        assertThat(eventually(panel::getPrefWidth, width -> width == 300)).isEqualTo(300);
    }

    @Test
    @DisplayName("closing shrinks it and then takes it out of the layout")
    void closes() throws Exception {
        create();
        run(slide::open);
        eventually(panel::getPrefWidth, width -> width == 300);

        run(slide::close);

        assertThat(slide.isOpen()).isFalse();
        assertThat(eventually(panel::isManaged, managed -> !managed)).isFalse();
        assertThat(panel.isVisible()).isFalse();
        assertThat(panel.getPrefWidth()).isZero();
    }

    @Test
    @DisplayName("opening again while it closes keeps it open")
    void reopensWhileClosing() throws Exception {
        create();
        run(() -> {
            slide.open();
            slide.close();
            slide.open();
        });

        assertThat(eventually(panel::getPrefWidth, width -> width == 300)).isEqualTo(300);
        assertThat(panel.isManaged()).isTrue();
        assertThat(slide.isOpen()).isTrue();
    }
}
