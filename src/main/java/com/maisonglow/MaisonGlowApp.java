package com.maisonglow;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX entry point of Maison Glow. It builds the {@link AppContext} once and
 * keeps it for the views; the main window and the navigation arrive in I-13.
 * Start it with {@code mvn javafx:run}; the console demo keeps running with
 * {@code mvn exec:java}.
 */
public final class MaisonGlowApp extends Application {

    private static final String TITLE = "Maison Glow";
    private static final double WIDTH = 1100;
    private static final double HEIGHT = 700;

    private AppContext context;

    /**
     * Opens the empty window over the session data folder.
     *
     * @param stage primary stage provided by JavaFX
     */
    @Override
    public void start(Stage stage) {
        try {
            context = new AppContext(AppContext.sessionFolder());
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "No se pudo preparar la carpeta de datos: " + e.getMessage()).showAndWait();
            Platform.exit();
            return;
        }
        stage.setTitle(TITLE);
        stage.setScene(new Scene(new StackPane(), WIDTH, HEIGHT));
        stage.show();
    }

    /** @return the services shared by every view */
    public AppContext context() {
        return context;
    }

    /**
     * Launches the JavaFX application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
