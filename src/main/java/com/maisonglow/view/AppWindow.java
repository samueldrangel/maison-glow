package com.maisonglow.view;

import com.maisonglow.AppContext;
import com.maisonglow.view.access.AccessSession;
import com.maisonglow.view.controller.ChangePasswordController;
import com.maisonglow.view.controller.LoginController;
import com.maisonglow.view.controller.MainController;
import com.maisonglow.view.controller.RegisterController;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Decides what the window shows from the session: the sign-in when nobody is in, the sign-up when asked, and the
 * area of the account once someone signs in. Closing the session throws the whole area away, so the chat, the
 * cart, the photos and any other screen kept by the {@link Navigator} are never seen by the next person.
 * Both roles use the same window, with the menu and the screens of their role: a customer never reaches the
 * administration screens, and the screens of the customer area are "En construcción" until the next block of the roadmap.
 */
public final class AppWindow {

    private static final String BASE = "/com/maisonglow/view/";

    private final Stage stage;
    private final AppContext context;
    private final Consumer<Stage> reveal;
    private MainController area;
    private Scene scene;

    /**
     * Creates the window handler.
     *
     * @param stage   the primary stage
     * @param context services shared by every view
     */
    public AppWindow(Stage stage, AppContext context) {
        this(stage, context, Stage::show);
    }

    /**
     * Creates the window handler with its own way of showing the stage, which the tests use so that no real window
     * opens.
     *
     * @param stage   the primary stage
     * @param context services shared by every view
     * @param reveal  makes the stage visible the first time a screen is ready
     */
    public AppWindow(Stage stage, AppContext context, Consumer<Stage> reveal) {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.context = Objects.requireNonNull(context, "context");
        this.reveal = Objects.requireNonNull(reveal, "reveal");
    }

    /** @return the root of what the window shows now, or null before the first screen */
    public Parent root() {
        return scene == null ? null : scene.getRoot();
    }

    /** Shows the first screen: the area of the open session if there is one, the sign-in otherwise. */
    public void start() {
        context.access().current().ifPresentOrElse(this::showArea, this::showLogin);
    }

    /** Shows the sign-in screen. */
    public void showLogin() {
        ViewLoader.Loaded<LoginController> loaded = ViewLoader.loadWithController(resource("LoginView.fxml"), context);
        loaded.controller().onSignedIn(this::showArea);
        loaded.controller().onRegister(this::showRegister);
        show(loaded.root());
    }

    /** Shows the sign-up screen. */
    public void showRegister() {
        ViewLoader.Loaded<RegisterController> loaded = ViewLoader.loadWithController(resource("RegisterView.fxml"), context);
        loaded.controller().onSignedIn(this::showArea);
        loaded.controller().onLogin(this::showLogin);
        show(loaded.root());
    }

    /**
     * Shows what the role of the session may see.
     *
     * @param session the open session
     */
    public void showArea(AccessSession session) {
        Objects.requireNonNull(session, "session");
        ViewLoader.Loaded<MainController> loaded = ViewLoader.loadWithController(resource("MainView.fxml"), context);
        area = loaded.controller();
        loaded.controller().showSession(session);
        loaded.controller().onLogoutRequested(this::logout);
        loaded.controller().onChangePasswordRequested(this::openChangePassword);
        show(loaded.root());
    }

    /** Closes the session and goes back to the sign-in; the screens of the account are discarded. */
    public void logout() {
        closeArea();
        context.access().logout();
        showLogin();
    }

    /** Throws away the screens of the open area, so nothing of one account is seen by the next. */
    private void closeArea() {
        if (area != null) {
            area.close();
            area = null;
        }
    }

    private void openChangePassword() {
        ViewLoader.Loaded<ChangePasswordController> loaded =
            ViewLoader.loadWithController(resource("ChangePasswordView.fxml"), context);
        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Cambiar contraseña");
        dialog.setResizable(false);
        loaded.controller().onCancel(dialog::close);
        loaded.controller().onDone(() -> {
            dialog.close();
            new AlertPresenter(scene.getRoot()).toast("Contraseña actualizada.");
        });
        dialog.setScene(new Scene(loaded.root()));
        dialog.show();
    }

    private void show(Parent root) {
        if (scene == null) {
            scene = new Scene(root);
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
        if (!stage.isShowing()) {
            reveal.accept(stage);
        }
    }

    private static URL resource(String file) {
        return Objects.requireNonNull(AppWindow.class.getResource(BASE + file), file);
    }
}
