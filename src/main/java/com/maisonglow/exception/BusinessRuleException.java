package com.maisonglow.exception;

/**
 * Signals that an operation violates a business rule, such as booking an
 * appointment in an occupied time slot or selling more units than the
 * available stock.
 */
public class BusinessRuleException extends Exception {

    /**
     * Creates an exception with the given message.
     *
     * @param message a user-readable description of the violated rule
     */
    public BusinessRuleException(String message) {
        super(message);
    }

    /**
     * Creates an exception with the given message and underlying cause.
     *
     * @param message a user-readable description of the violated rule
     * @param cause   the exception that triggered this one
     */
    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
