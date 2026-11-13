package com.maisonglow.view;

import com.maisonglow.AppContext;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Objects;

/**
 * Loads an {@code .fxml} and builds its controller with the {@link AppContext}.
 * A controller declares {@code public Controller(AppContext context)} (or a
 * no-argument constructor when it needs no services), so a controller never
 * creates a DAO or a service itself.
 */
public final class ViewLoader {

    private ViewLoader() {
    }

    /**
     * Loads a view and wires its controller.
     *
     * @param fxml location of the {@code .fxml}
     * @param context services handed to the controller
     * @return the root node of the view
     * @throws UncheckedIOException if the file cannot be read or is malformed
     */
    public static Parent load(URL fxml, AppContext context) {
        Objects.requireNonNull(fxml, "fxml");
        Objects.requireNonNull(context, "context");
        FXMLLoader loader = new FXMLLoader(fxml);
        loader.setControllerFactory(type -> createController(type, context));
        try {
            return loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load view " + fxml, e);
        }
    }

    private static Object createController(Class<?> type, AppContext context) {
        try {
            try {
                return type.getConstructor(AppContext.class).newInstance(context);
            } catch (NoSuchMethodException e) {
                return type.getConstructor().newInstance();
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create controller " + type.getName(), e);
        }
    }
}
