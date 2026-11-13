package com.maisonglow.view;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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
        alert.setHeaderText(null);
        alert.setGraphic(null);
        DialogPane pane = alert.getDialogPane();
        pane.setContent(layout(type, title, text));
        pane.getStylesheets().add(AlertPresenter.class.getResource(STYLES).toExternalForm());
        pane.getStyleClass().add("view-root");
        for (ButtonType choice : buttons) {
            Node button = pane.lookupButton(choice);
            button.getStyleClass().add(choice.getButtonData().isCancelButton() ? "button-secondary" : "button-primary");
            if (button instanceof ButtonBase base) {
                Animations.lift(base);
            }
        }
        Window window = owner.get();
        if (window != null) {
            alert.initOwner(window);
        }
        return alert;
    }

    /** The medallion with the icon of the kind of dialog, and next to it the title and the explanation. */
    private static HBox layout(AlertType type, String heading, String text) {
        Label title = new Label(heading);
        title.getStyleClass().add("dialog-title");
        title.setWrapText(true);
        Label body = new Label(text);
        body.getStyleClass().add("dialog-text");
        body.setWrapText(true);
        VBox words = new VBox(6, title, body);
        HBox.setHgrow(words, Priority.ALWAYS);
        HBox layout = new HBox(20, Icons.medallion(type), words);
        layout.getStyleClass().add("dialog-layout");
        layout.setAlignment(Pos.TOP_LEFT);
        layout.setPrefWidth(430);
        return layout;
    }

    private static Window windowOf(Node anchor) {
        return anchor == null || anchor.getScene() == null ? null : anchor.getScene().getWindow();
    }
}
