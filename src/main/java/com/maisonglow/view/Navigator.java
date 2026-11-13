package com.maisonglow.view;

import com.maisonglow.AppContext;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;

/**
 * Shows one {@link View} at a time in the central area of the main window.
 * Screens that are not built yet appear as an "En construcción" notice, so the
 * whole menu can be walked from the first day.
 */
public final class Navigator {

    private final StackPane content;
    private final AppContext context;
    private View current;

    /**
     * Creates a navigator over the central area.
     *
     * @param content the container that holds the current screen
     * @param context services handed to every controller
     */
    public Navigator(StackPane content, AppContext context) {
        this.content = Objects.requireNonNull(content, "content");
        this.context = Objects.requireNonNull(context, "context");
    }

    /**
     * Replaces the current screen.
     *
     * @param view the screen to show
     */
    public void show(View view) {
        Objects.requireNonNull(view, "view");
        URL fxml = Navigator.class.getResource(view.resource());
        Node screen = fxml == null ? underConstruction(view) : ViewLoader.load(fxml, context);
        content.getChildren().setAll(screen);
        if (screen instanceof Parent parent) {
            Animations.installButtonMotion(parent);
        }
        Animations.enter(screen);
        current = view;
    }

    /** @return the screen being shown, empty before the first {@link #show(View)} */
    public Optional<View> current() {
        return Optional.ofNullable(current);
    }

    private static Node underConstruction(View view) {
        Label title = new Label(view.title());
        title.getStyleClass().add("view-title");
        Label notice = new Label("En construcción");
        notice.getStyleClass().add("placeholder-notice");
        VBox box = new VBox(8, title, notice);
        box.setAlignment(Pos.CENTER);
        return box;
    }
}
