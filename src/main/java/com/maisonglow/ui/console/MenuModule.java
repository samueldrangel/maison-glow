package com.maisonglow.ui.console;

import java.util.List;
import java.util.Objects;

/**
 * Base of the modules that are a menu of options. Each subclass only lists
 * its options; opening and looping is done here (template method).
 */
public abstract class MenuModule implements ConsoleModule {

    private static final String BACK_LABEL = "Volver al menú principal";

    private final String name;
    private final ConsoleIo console;

    /**
     * Creates the module.
     *
     * @param name    the name shown in the main menu and as the menu heading
     * @param console where the module talks to the user
     */
    protected MenuModule(String name, ConsoleIo console) {
        this.name = Objects.requireNonNull(name, "name");
        this.console = Objects.requireNonNull(console, "console");
    }

    /**
     * Lists the options of the module menu.
     *
     * @return the options, numbered from 1 in this order
     */
    protected abstract List<MenuOption> options();

    /**
     * Returns the console of the module.
     *
     * @return the console
     */
    protected ConsoleIo console() {
        return console;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void open() {
        new Menu(console, name, BACK_LABEL, options()).run();
    }
}
