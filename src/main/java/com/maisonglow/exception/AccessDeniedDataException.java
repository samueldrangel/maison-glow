package com.maisonglow.exception;

/** Preserves existing DAO/service checked contracts while denying unauthorized legacy entry points. */
public final class AccessDeniedDataException extends DataAccessException {
    private final boolean sessionExpired;
    public AccessDeniedDataException(String message,Throwable cause) {this(message,cause,false);}
    public AccessDeniedDataException(String message,Throwable cause,boolean sessionExpired) {super(message,cause);this.sessionExpired=sessionExpired;}
    public boolean isSessionExpired(){return sessionExpired;}
}
