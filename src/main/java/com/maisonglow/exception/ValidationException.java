package com.maisonglow.exception;

/**
 * Signals that a piece of input data is invalid, such as an empty required
 * field, a malformed email, or a negative price.
 */
public class ValidationException extends Exception {

    /**
     * Creates an exception with the given message.
     *
     * @param message a user-readable description of the invalid data
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Creates an exception with the given message and underlying cause.
     *
     * @param message a user-readable description of the invalid data
     * @param cause   the exception that triggered this one
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
