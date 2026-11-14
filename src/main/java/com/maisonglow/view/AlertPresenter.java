package com.maisonglow.view;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The dialogs of the application. A view shows a service error, a notice or a
 * question with one line, and the dialog already follows the look of
 * styles.css:
 *
 * <pre>{@code
 * AlertPresenter alerts = new AlertPresenter(content);   // any node of the window
 * try { service.register(data); } catch (ValidationException | BusinessRuleException e) { alerts.showError(e); }
 * }</pre>
 */
public final class AlertPresenter {

    private static final String APP_TITLE = "Maison Glow";
    private static final String STYLES = "/com/maisonglow/view/styles.css";
    private static final ButtonType UNDERSTOOD = new ButtonType("Entendido", ButtonData.OK_DONE);
    private static final ButtonType CONFIRM = new ButtonType("Confirmar", ButtonData.OK_DONE);
    private static final ButtonType CANCEL = new ButtonType("Cancelar", ButtonData.CANCEL_CLOSE);

    private final Supplier<Window> owner;
    private final Function<Alert, Optional<ButtonType>> display;

    /**
     * @param anchor any node of the window the dialogs belong to; the dialogs open centred over it
     */
    public AlertPresenter(Node anchor) {
        this(() -> windowOf(anchor), Alert::showAndWait);
    }

    /** Lets the tests choose how a dialog is displayed instead of opening a real window. */
    AlertPresenter(Supplier<Window> owner, Function<Alert, Optional<ButtonType>> display) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.display = Objects.requireNonNull(display, "display");
    }

    /**
     * Explains an exception of a service in a dialog.
     *
     * @param error the exception that was caught
     */
    public void showError(Exception error) {
        AlertMessages.Message message = AlertMessages.of(error);
        display.apply(build(message.type(), message.title(), message.text(), UNDERSTOOD));
    }

    /**
     * Shows a notice that needs no answer.
     *
     * @param text what to tell the user
     */
    public void showInfo(String text) {
        display.apply(build(AlertType.INFORMATION, "Información", text, UNDERSTOOD));
    }

    /**
     * Asks the user to confirm something before doing it.
     *
     * @param question what is about to happen
     * @return true only if the user pressed "Confirmar"
     */
    public boolean confirm(String question) {
        return display.apply(build(AlertType.CONFIRMATION, "Confirmar acción", question, CONFIRM, CANCEL))
            .filter(CONFIRM::equals).isPresent();
    }

    Alert build(AlertType type, String title, String text, ButtonType... buttons) {
        Alert alert = new Alert(type, Objects.requireNonNull(text, "text"), buttons);
        alert.setTitle(APP_TITLE);
        alert.setHeaderText(title);
        alert.getDialogPane().getStylesheets().add(AlertPresenter.class.getResource(STYLES).toExternalForm());
        alert.getDialogPane().getStyleClass().add("view-root");
        Window window = owner.get();
        if (window != null) {
            alert.initOwner(window);
        }
        return alert;
    }

    private static Window windowOf(Node anchor) {
        return anchor == null || anchor.getScene() == null ? null : anchor.getScene().getWindow();
    }
}
