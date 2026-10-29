package com.maisonglow.ui.console;

import java.util.List;
import java.util.Objects;

/**
 * The main menu of the interactive console. It lists the modules it receives
 * and opens the one the user chooses, so adding a module never changes this
 * class.
 */
public final class MainMenu {

    private static final String TITLE = "Maison Glow - Menú principal";
    private static final String EXIT_LABEL = "Salir";

    private final ConsoleIo console;
    private final List<ConsoleModule> modules;

    /**
     * Creates the main menu.
     *
     * @param console where the menu is shown
     * @param modules the modules, numbered from 1 in this order
     */
    public MainMenu(ConsoleIo console, List<ConsoleModule> modules) {
        this.console = Objects.requireNonNull(console, "console");
        this.modules = List.copyOf(modules);
    }

    /**
     * Runs the menu until the user chooses to leave or the input ends.
     */
    public void run() {
        List<MenuOption> options = modules.stream()
            .map(module -> new MenuOption(module.getName(), module::open))
            .toList();
        try {
            new Menu(console, TITLE, EXIT_LABEL, options).run();
        } catch (ConsoleClosedException e) {
            console.println();
        }
        console.println("Hasta pronto.");
    }
}
