package com.maisonglow.ui.console;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import java.util.List;
import java.util.Objects;

/**
 * Shows a numbered list of options and runs the one the user chooses, until
 * the user picks {@code 0}. It is the only place that turns the business
 * exceptions into messages, so a mistake never ends the session.
 */
public final class Menu {

    private static final int EXIT_CHOICE = 0;

    private final ConsoleIo console;
    private final String title;
    private final String exitLabel;
    private final List<MenuOption> options;

    /**
     * Creates the menu.
     *
     * @param console   where the menu is shown
     * @param title     the heading
     * @param exitLabel the text of option 0 (for example {@code Volver})
     * @param options   the options, numbered from 1 in this order
     */
    public Menu(ConsoleIo console, String title, String exitLabel, List<MenuOption> options) {
        this.console = Objects.requireNonNull(console, "console");
        this.title = Objects.requireNonNull(title, "title");
        this.exitLabel = Objects.requireNonNull(exitLabel, "exitLabel");
        this.options = List.copyOf(options);
    }

    /**
     * Shows the menu again and again until the user chooses option 0.
     *
     * @throws ConsoleClosedException if the input ends while waiting for an answer
     */
    public void run() {
        int choice;
        do {
            show();
            choice = console.readInt("Opción");
            if (choice != EXIT_CHOICE) {
                choose(choice);
            }
        } while (choice != EXIT_CHOICE);
    }

    private void show() {
        console.title(title);
        for (int i = 0; i < options.size(); i++) {
            console.println("  " + (i + 1) + ". " + options.get(i).label());
        }
        console.println("  " + EXIT_CHOICE + ". " + exitLabel);
    }

    private void choose(int choice) {
        if (choice < 1 || choice > options.size()) {
            console.println("Opción no válida. Elija un número de la lista.");
            return;
        }
        execute(options.get(choice - 1));
    }

    private void execute(MenuOption option) {
        try {
            option.action().run();
        } catch (ValidationException | BusinessRuleException e) {
            console.println("No se pudo completar: " + e.getMessage());
        } catch (DataAccessException e) {
            console.println("Error de datos: " + e.getMessage());
        }
    }
}
