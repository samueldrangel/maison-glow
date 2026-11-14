package com.maisonglow.view;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.util.Duration;

import java.util.Objects;

/**
 * The small motions of the interface, in one place so every view moves the
 * same way. They only touch the opacity, scale and translation of a node, so
 * they never fight with the layout or with the CSS.
 */
public final class Animations {

    private static final Duration QUICK = Duration.millis(140);
    private static final Duration STANDARD = Duration.millis(220);
    private static final Duration POP = Duration.millis(260);
    private static final double RISE = 10;
    private static final double NUDGE = 3;

    private Animations() {
    }

    /**
     * A screen that has just been shown: fades in while rising a few pixels.
     *
     * @param node the screen
     */
    public static void enter(Node node) {
        Objects.requireNonNull(node, "node");
        node.setOpacity(0);
        node.setTranslateY(RISE);
        FadeTransition fade = new FadeTransition(STANDARD, node);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition rise = new TranslateTransition(STANDARD, node);
        rise.setToY(0);
        rise.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, rise).play();
    }

    /**
     * An element that appears (a badge, a notice): grows with a small bounce.
     *
     * @param node the element
     */
    public static void pop(Node node) {
        Objects.requireNonNull(node, "node");
        node.setScaleX(0.5);
        node.setScaleY(0.5);
        ScaleTransition grow = new ScaleTransition(POP, node);
        grow.setToX(1);
        grow.setToY(1);
        grow.setInterpolator(Interpolator.SPLINE(0.2, 1.6, 0.4, 1));
        grow.play();
    }

    /**
     * A field that is wrong: shakes sideways a few times.
     *
     * @param node the field
     */
    public static void shake(Node node) {
        Objects.requireNonNull(node, "node");
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), node);
        shake.setFromX(0);
        shake.setByX(6);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setOnFinished(event -> node.setTranslateX(0));
        shake.play();
    }

    /**
     * The icon of a button slides a little to the right while the cursor is over it.
     *
     * @param button a button whose graphic is its icon
     */
    public static void nudgeOnHover(Labeled button) {
        Objects.requireNonNull(button, "button");
        Node icon = button.getGraphic();
        if (icon == null) {
            return;
        }
        TranslateTransition slide = new TranslateTransition(QUICK, icon);
        slide.setInterpolator(Interpolator.EASE_OUT);
        button.hoverProperty().addListener((observable, was, hovered) -> {
            slide.stop();
            slide.setToX(hovered ? NUDGE : 0);
            slide.playFromStart();
        });
    }
}
