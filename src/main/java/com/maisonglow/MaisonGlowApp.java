package com.maisonglow;

import com.maisonglow.view.ViewLoader;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX entry point of Maison Glow. It builds the {@link AppContext} once and
 * opens the main window; every other screen is shown by the
 * {@link com.maisonglow.view.Navigator}. Start it with {@code mvn javafx:run};
 * the console demo keeps running with {@code mvn exec:java}.
 */
public final class MaisonGlowApp extends Application {

    private static final String TITLE = "Maison Glow";
    private static final String MAIN_VIEW = "/com/maisonglow/view/MainView.fxml";
    private static final double MIN_WIDTH = 960;
    private static final double MIN_HEIGHT = 600;

    private AppContext context;

    /**
     * Opens the main window over the session data folder.
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
        Parent root = ViewLoader.load(MaisonGlowApp.class.getResource(MAIN_VIEW), context);
        stage.setTitle(TITLE);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.setScene(new Scene(root));
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
