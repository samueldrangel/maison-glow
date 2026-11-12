package com.maisonglow.ui.console;

/**
 * A part of the business that the main menu can open (customers, products,
 * appointments...). A new module is added as a new class, without changing
 * the main menu.
 */
public interface ConsoleModule {

    /**
     * Returns the name shown in the main menu.
     *
     * @return the module name
     */
    String getName();

    /**
     * Opens the module menu and returns when the user leaves it.
     *
     * @throws ConsoleClosedException if the input ends while waiting for an answer
     */
    void open();
}
