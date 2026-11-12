package com.maisonglow.ui.console;

/**
 * Signals that the console input ended (end of file, Ctrl+Z or Ctrl+D), so no
 * further answers can be read and the session must finish.
 */
public class ConsoleClosedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     */
    public ConsoleClosedException() {
        super("La entrada de la consola se cerró.");
    }
}
