package com.maisonglow.view;

import com.maisonglow.AppContext;
import com.maisonglow.view.access.AccessRole;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Shows one {@link View} at a time in the central area of the main window.
 * Screens that are not built yet appear as an "En construcción" notice, so the
 * whole menu can be walked from the first day. A screen that keeps what the user was doing (the chat, the try-on)
 * is loaded once and shown again as it was left.
 */
public final class Navigator {

    private final StackPane content;
    private final AppContext context;
    private final AccessRole role;
    private final Map<View, Node> kept = new EnumMap<>(View.class);
    private final Map<View, RefreshableView> refreshable = new EnumMap<>(View.class);
    private View current;

    /**
     * Creates a navigator over the central area for one role. A screen the role may not open is refused, even if
     * something asks for it directly, because hiding the menu entry is not authorization.
     *
     * @param content the container that holds the current screen
     * @param context services handed to every controller
     * @param role    the role of the open session
     */
    public Navigator(StackPane content, AppContext context, AccessRole role) {
        this.content = Objects.requireNonNull(content, "content");
        this.context = Objects.requireNonNull(context, "context");
        this.role = Objects.requireNonNull(role, "role");
    }

    /**
     * Replaces the current screen, unless the role of the session may not open it: then nothing is loaded and the
     * area says so.
     *
     * @param view the screen to show
     * @return false if the screen was refused
     */
    public boolean show(View view) {
        Objects.requireNonNull(view, "view");
        if (!view.allowedFor(role)) {
            content.getChildren().setAll(forbidden());
            return false;
        }
        Node screen = kept.get(view);
        if (screen == null) {
            URL fxml = Navigator.class.getResource(view.resource());
            if (fxml == null) {
                screen = underConstruction(view);
            } else {
                var loaded = ViewLoader.loadWithController(fxml, context);
                screen = loaded.root();
                if (loaded.controller() instanceof RefreshableView controller) refreshable.put(view, controller);
            }
            if (screen instanceof Parent parent) {
                Animations.installButtonMotion(parent);
            }
            if (view.keepsState()) {
                kept.put(view, screen);
            }
        }
        content.getChildren().setAll(screen);
        if (refreshable.containsKey(view)) refreshable.get(view).refresh();
        Animations.enter(screen);
        current = view;
        return true;
    }

    /** @return the screen being shown, empty before the first {@link #show(View)} */
    public Optional<View> current() {
        return Optional.ofNullable(current);
    }

    private static Node forbidden() {
        Label title = new Label("Sin permiso");
        title.getStyleClass().add("view-title");
        Label notice = new Label("Tu cuenta no puede abrir esta pantalla.");
        notice.getStyleClass().add("placeholder-notice");
        VBox box = new VBox(8, title, notice);
        box.setAlignment(Pos.CENTER);
        return box;
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
