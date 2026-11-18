package com.maisonglow.view.access;

import com.maisonglow.exception.BusinessRuleException;

/**
 * Thrown by the account service when something is asked without an open session or after it ended (it timed out, the
 * account was turned off or the password changed). The screens do not handle it themselves: showing it through the
 * {@code AlertPresenter} explains what happened and sends the person back to the sign-in.
 */
public class SessionExpiredException extends BusinessRuleException {

    /** The message shown to the person. */
    public static final String MESSAGE = "Tu sesión terminó. Inicia sesión de nuevo.";

    /** Creates the exception with the standard message. */
    public SessionExpiredException() {
        super(MESSAGE);
    }
}
