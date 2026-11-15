package com.maisonglow.view.sales;

import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Objects;

/** The invoice of a registered sale, with the option to send it to the customer by e-mail. */
public final class InvoiceDialog {

    /** Sends the invoice to the customer. */
    public static final ButtonType SEND = new ButtonType("Enviar por correo", ButtonData.OK_DONE);
    /** Closes the invoice without sending it. */
    public static final ButtonType CLOSE = new ButtonType("Cerrar", ButtonData.CANCEL_CLOSE);

    private static final String STYLES = "/com/maisonglow/view/styles.css";

    private InvoiceDialog() {
    }

    /**
     * Shows the invoice and waits until it is closed.
     *
     * @param owner the window the dialog opens over, or null
     * @param invoice the text made by {@code InvoiceFormatter}
     * @return true if the user chose to send it by e-mail
     */
    public static boolean show(Window owner, String invoice) {
        Dialog<ButtonType> dialog = build(invoice);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        return dialog.showAndWait().filter(SEND::equals).isPresent();
    }

    static Dialog<ButtonType> build(String invoice) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Maison Glow");
        Label title = new Label("Venta registrada");
        title.getStyleClass().add("dialog-title");
        Label text = new Label(Objects.requireNonNull(invoice, "invoice"));
        text.getStyleClass().add("invoice-text");
        DialogPane pane = dialog.getDialogPane();
        pane.setContent(new VBox(12, title, text));
        pane.getButtonTypes().addAll(SEND, CLOSE);
        pane.getStylesheets().add(InvoiceDialog.class.getResource(STYLES).toExternalForm());
        pane.getStyleClass().add("view-root");
        pane.lookupButton(SEND).getStyleClass().add("button-primary");
        pane.lookupButton(CLOSE).getStyleClass().add("button-secondary");
        return dialog;
    }
}
