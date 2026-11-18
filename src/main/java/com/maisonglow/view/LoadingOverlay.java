package com.maisonglow.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * A veil with a spinner and a short text that covers a screen while something slow runs, and keeps the clicks from
 * reaching what is behind it. Add it as the last child of a {@link StackPane} (see {@link #over}) and call
 * {@link #show} / {@link #hide}.
 */
public final class LoadingOverlay extends StackPane {

    private final Label text = new Label();

    /** Creates a hidden overlay. */
    public LoadingOverlay() {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(44, 44);
        text.getStyleClass().add("loading-text");
        VBox box = new VBox(10, spinner, text);
        box.setAlignment(Pos.CENTER);
        getChildren().add(box);
        getStyleClass().add("loading-overlay");
        setAlignment(Pos.CENTER);
        setVisible(false);
        setManaged(false);
    }

    /**
     * Adds an overlay over the content of a container.
     *
     * @param host the container to cover
     * @return the overlay, hidden
     */
    public static LoadingOverlay over(StackPane host) {
        LoadingOverlay overlay = new LoadingOverlay();
        Objects.requireNonNull(host, "host").getChildren().add(overlay);
        return overlay;
    }

    /**
     * Covers the screen.
     *
     * @param message what is being done, for example "Guardando la foto…"
     */
    public void show(String message) {
        text.setText(Objects.requireNonNull(message, "message"));
        setManaged(true);
        setVisible(true);
        toFront();
    }

    /** Uncovers the screen. */
    public void hide() {
        setVisible(false);
        setManaged(false);
    }

    /** @return true while the screen is covered */
    public boolean isShown() {
        return isVisible();
    }
}
