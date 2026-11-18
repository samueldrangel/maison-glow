package com.maisonglow.view;

import com.maisonglow.AppContext;
import com.maisonglow.view.controller.ChangePasswordController;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Objects;

/**
 * The window to change the password of the open session, shared by the side menu and by "Mi perfil". The service ends every
 * session of the account when the password changes, so after a change the window of the application closes the session and
 * goes back to the sign-in: that is what {@link #onChanged} sets, once, from {@code AppWindow}.
 */
public final class ChangePasswordDialog {

    private static Runnable afterChange = () -> { };

    private ChangePasswordDialog() {
    }

    /**
     * Sets what happens after the password was changed.
     *
     * @param action closes the session and goes back to the sign-in
     */
    public static void onChanged(Runnable action) {
        afterChange = Objects.requireNonNull(action, "action");
    }

    /**
     * Opens the window to change the password.
     *
     * @param owner   the window it belongs to
     * @param context services shared by every view
     */
    public static void show(Window owner, AppContext context) {
        ViewLoader.Loaded<ChangePasswordController> loaded = ViewLoader.loadWithController(
            ChangePasswordDialog.class.getResource("/com/maisonglow/view/ChangePasswordView.fxml"), context);
        Stage dialog = new Stage();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Cambiar contraseña");
        dialog.setResizable(false);
        loaded.controller().onCancel(dialog::close);
        loaded.controller().onDone(() -> {
            dialog.close();
            afterChange.run();
        });
        dialog.setScene(new Scene(loaded.root()));
        dialog.show();
    }
}
