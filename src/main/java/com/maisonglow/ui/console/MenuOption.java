package com.maisonglow.ui.console;

import java.util.Objects;

/**
 * A numbered line of a menu and the action it runs.
 *
 * @param label  the text shown to the user
 * @param action what happens when the option is chosen
 */
public record MenuOption(String label, MenuAction action) {

    /**
     * Creates the option.
     *
     * @param label  the text shown to the user
     * @param action what happens when the option is chosen
     */
    public MenuOption {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(action, "action");
    }
}
