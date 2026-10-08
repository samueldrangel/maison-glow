package com.maisonglow.exception;

/**
 * Signals a failure while reading from or writing to the data store.
 * Data access classes wrap low-level errors (for example, {@code SQLException})
 * in this exception so that upper layers do not depend on the storage technology.
 */
public class DataAccessException extends Exception {

    /**
     * Creates an exception with the given message.
     *
     * @param message a description of the failed data operation
     */
    public DataAccessException(String message) {
        super(message);
    }

    /**
     * Creates an exception with the given message and underlying cause.
     *
     * @param message a description of the failed data operation
     * @param cause   the low-level exception that triggered this one
     */
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
