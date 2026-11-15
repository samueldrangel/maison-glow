package com.maisonglow.view;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.util.Objects;

/**
 * A side panel that is hidden until it is needed. Opening it makes it grow from the right edge while
 * its content, anchored to its left edge, sweeps in; the rest of the screen gives way as it grows,
 * and closing it reverses that. Hidden, the panel takes no room at all, so the table or list next to
 * it uses the whole width. The content keeps a fixed width while the panel grows, so nothing re-wraps.
 */
public final class SlidePanel {

    private static final Duration DURATION = Duration.millis(260);

    private final Region panel;
    private final double width;
    private boolean open;
    private Timeline running;

    /**
     * Hides the panel and takes over its width.
     *
     * @param panel the node in the layout; it should clip what overflows, as a {@code ScrollPane} does
     * @param content what the panel shows; it keeps {@code width} while the panel grows
     * @param width the width of the panel when it is fully open
     */
    public SlidePanel(Region panel, Node content, double width) {
        this.panel = Objects.requireNonNull(panel, "panel");
        this.width = width;
        if (content instanceof Region region) {
            region.setMinWidth(width);
            region.setPrefWidth(width);
        }
        panel.minWidthProperty().bind(panel.prefWidthProperty());
        panel.maxWidthProperty().bind(panel.prefWidthProperty());
        panel.setPrefWidth(0);
        hide();
    }

    /** @return true if the panel is open or opening */
    public boolean isOpen() {
        return open;
    }

    /** Opens the panel; nothing happens if it is already open. */
    public void open() {
        if (open) {
            return;
        }
        open = true;
        panel.setVisible(true);
        panel.setManaged(true);
        slideTo(width, null);
    }

    /** Closes the panel; nothing happens if it is already closed. */
    public void close() {
        if (!open) {
            return;
        }
        open = false;
        slideTo(0, this::hide);
    }

    private void hide() {
        panel.setVisible(false);
        panel.setManaged(false);
    }

    private void slideTo(double target, Runnable then) {
        if (running != null) {
            running.stop();
        }
        running = new Timeline(new KeyFrame(DURATION, new KeyValue(panel.prefWidthProperty(), target, Interpolator.EASE_BOTH)));
        if (then != null) {
            running.setOnFinished(event -> then.run());
        }
        running.play();
    }
}
