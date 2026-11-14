package com.maisonglow.view;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.Objects;

/**
 * A brief notice that does not interrupt: it slides in at the bottom right of
 * the window, stays a moment and fades away by itself. It is for good news
 * ("Venta registrada") where a dialog that asks to be dismissed would be too much.
 */
public final class Toast {

    private static final String STYLES = "/com/maisonglow/view/styles.css";
    private static final Duration ENTER = Duration.millis(260);
    private static final Duration STAY = Duration.millis(2800);
    private static final Duration LEAVE = Duration.millis(320);
    private static final double RISE = 18;
    private static final double MARGIN_RIGHT = 28;
    private static final double MARGIN_BOTTOM = 36;

    private static Popup current;

    private Toast() {
    }

    /**
     * Shows a notice over the bottom right corner of a window; a notice that is still showing is replaced.
     *
     * @param owner the window; nothing is shown if it is null
     * @param text what to tell the user
     */
    public static void show(Window owner, String text) {
        Objects.requireNonNull(text, "text");
        if (owner == null) {
            return;
        }
        if (current != null) {
            current.hide();
        }
        Node content = build(text);
        Popup popup = new Popup();
        popup.getContent().add(content);
        popup.show(owner);
        content.applyCss();
        double width = content.getLayoutBounds().getWidth();
        double height = content.getLayoutBounds().getHeight();
        popup.setX(owner.getX() + owner.getWidth() - width - MARGIN_RIGHT);
        popup.setY(owner.getY() + owner.getHeight() - height - MARGIN_BOTTOM);
        current = popup;

        content.setOpacity(0);
        content.setTranslateY(RISE);
        FadeTransition fadeIn = new FadeTransition(ENTER, content);
        fadeIn.setToValue(1);
        TranslateTransition rise = new TranslateTransition(ENTER, content);
        rise.setToY(0);
        rise.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition fadeOut = new FadeTransition(LEAVE, content);
        fadeOut.setToValue(0);
        SequentialTransition life = new SequentialTransition(new ParallelTransition(fadeIn, rise), new PauseTransition(STAY), fadeOut);
        life.setOnFinished(event -> {
            popup.hide();
            if (current == popup) {
                current = null;
            }
        });
        life.play();
    }

    /** The notice itself: a check mark and the text, on the dark ink of the palette. */
    static Node build(String text) {
        Label message = new Label(Objects.requireNonNull(text, "text"));
        message.getStyleClass().add("toast-text");
        HBox toast = new HBox(12, Icons.small(Icons.Kind.SUCCESS, 22), message);
        toast.getStyleClass().addAll("view-root", "toast");
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.getStylesheets().add(Toast.class.getResource(STYLES).toExternalForm());
        return toast;
    }
}
