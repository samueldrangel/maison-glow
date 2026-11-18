package com.maisonglow.exception;

/** An unauthenticated, expired or unauthorized operation; safe to present to the user. */
public final class AuthorizationException extends BusinessRuleException {
    public AuthorizationException(String message) { super(message); }
}
