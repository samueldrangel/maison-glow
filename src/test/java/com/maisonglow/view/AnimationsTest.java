package com.maisonglow.view;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Animations tests")
class AnimationsTest {

    private static boolean toolkit;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    /** JavaFX only sets :hover from real mouse events, so the test sets it the way the scene does. */
    private static void setHover(Node node, boolean hover) {
        try {
            Method setter = Node.class.getDeclaredMethod("setHover", boolean.class);
            setter.setAccessible(true);
            setter.invoke(node, hover);
        } catch (ReflectiveOperationException | RuntimeException e) {
            assumeTrue(false, "cannot simulate the cursor: " + e);
        }
    }

    private static <T> T eventually(Supplier<T> reading, java.util.function.Predicate<T> done) throws Exception {
        T value = FxTest.onFxThread(reading);
        for (int attempt = 0; attempt < 40 && !done.test(value); attempt++) {
            Thread.sleep(25);
            value = FxTest.onFxThread(reading);
        }
        return value;
    }

    @Test
    @DisplayName("a button rises while the cursor is over it and comes back")
    void aButtonRisesOnHover() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        Button button = FxTest.onFxThread(() -> {
            Button created = new Button("Guardar");
            Animations.lift(created);
            return created;
        });

        FxTest.onFxThread(() -> {
            setHover(button, true);
            return null;
        });
        double raised = eventually(button::getTranslateY, y -> y <= -2);
        FxTest.onFxThread(() -> {
            setHover(button, false);
            return null;
        });
        double back = eventually(button::getTranslateY, y -> y == 0);

        assertThat(raised).isEqualTo(-2);
        assertThat(back).isZero();
    }

    @Test
    @DisplayName("the star of a gold button twinkles while the cursor is over the button")
    void theStarTwinkles() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        Button button = FxTest.onFxThread(() -> {
            Button created = new Button("Nuevo");
            created.setGraphic(Icons.sparkle());
            Animations.lift(created);
            return created;
        });

        FxTest.onFxThread(() -> {
            setHover(button, true);
            return null;
        });

        assertThat(eventually(() -> button.getGraphic().getRotate(), angle -> angle >= 90)).isEqualTo(90);
    }

    @Test
    @DisplayName("installing the motion twice does not stack it")
    void installingTwiceIsHarmless() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        Button button = FxTest.onFxThread(() -> {
            Button created = new Button("Guardar");
            Animations.lift(created);
            Animations.lift(created);
            return created;
        });

        FxTest.onFxThread(() -> {
            setHover(button, true);
            return null;
        });

        assertThat(eventually(button::getTranslateY, y -> y <= -2)).isEqualTo(-2);
    }

    @Test
    @DisplayName("gives the motion to every primary, gold and secondary button of a screen")
    void installsOnAScreen() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        Button primary = FxTest.onFxThread(Button::new);
        Button gold = FxTest.onFxThread(Button::new);
        Button plain = FxTest.onFxThread(Button::new);
        primary.getStyleClass().add("button-primary");
        gold.getStyleClass().add("button-gold");
        VBox screen = FxTest.onFxThread(() -> new VBox(primary, gold, plain));

        FxTest.onFxThread(() -> {
            Animations.installButtonMotion(screen);
            setHover(primary, true);
            setHover(gold, true);
            setHover(plain, true);
            return null;
        });

        assertThat(eventually(primary::getTranslateY, y -> y <= -2)).isEqualTo(-2);
        assertThat(eventually(gold::getTranslateY, y -> y <= -2)).isEqualTo(-2);
        assertThat(plain.getTranslateY()).as("a button without a style is left alone").isZero();
    }
}
