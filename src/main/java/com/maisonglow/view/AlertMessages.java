package com.maisonglow.view;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import javafx.scene.control.Alert.AlertType;

import java.util.Objects;

/**
 * Turns an exception into the text the user reads. The three exceptions of the
 * services already carry a message in Spanish that is meant to be shown, except
 * the data one, which is technical and is replaced by a clear sentence. A stack
 * trace never reaches the screen.
 */
public final class AlertMessages {

    private static final String DATA_ERROR = "No se pudo leer o guardar la información. "
        + "Revisa que la carpeta de datos esté disponible e inténtalo de nuevo.";
    private static final String UNEXPECTED_ERROR = "Ocurrió un problema inesperado. "
        + "Inténtalo de nuevo; si continúa, avisa al equipo.";

    private AlertMessages() {
    }

    /**
     * What a dialog shows.
     *
     * @param type kind of dialog (warning for what the user can fix, error for what they cannot)
     * @param title short title of the dialog
     * @param text explanation in Spanish
     */
    public record Message(AlertType type, String title, String text) {
    }

    /**
     * @param error any exception thrown while calling a service
     * @return the dialog that explains it
     */
    public static Message of(Exception error) {
        Objects.requireNonNull(error, "error");
        if (error instanceof ValidationException) {
            return new Message(AlertType.WARNING, "Datos no válidos", error.getMessage());
        }
        if (error instanceof BusinessRuleException) {
            return new Message(AlertType.WARNING, "No se pudo completar la acción", error.getMessage());
        }
        if (error instanceof DataAccessException) {
            return new Message(AlertType.ERROR, "Error de datos", DATA_ERROR);
        }
        return new Message(AlertType.ERROR, "Error inesperado", UNEXPECTED_ERROR);
    }
}
