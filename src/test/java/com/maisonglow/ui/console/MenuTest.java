package com.maisonglow.ui.console;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Menu tests")
class MenuTest {

    private final ByteArrayOutputStream printed = new ByteArrayOutputStream();
    private final List<String> calls = new ArrayList<>();

    private ConsoleIo consoleWith(String... answers) {
        byte[] input = (String.join("\n", answers) + "\n").getBytes(StandardCharsets.UTF_8);
        return new ConsoleIo(new ByteArrayInputStream(input), StandardCharsets.UTF_8,
            new PrintStream(printed, true, StandardCharsets.UTF_8));
    }

    private String output() {
        return printed.toString(StandardCharsets.UTF_8);
    }

    private Menu menu(ConsoleIo console, MenuOption... options) {
        return new Menu(console, "Prueba", "Volver", List.of(options));
    }

    @Test
    @DisplayName("shows the numbered options and runs the chosen one until 0")
    void runsTheChosenOption() {
        Menu menu = menu(consoleWith("2", "1", "2", "0"),
            new MenuOption("Primera", () -> calls.add("primera")),
            new MenuOption("Segunda", () -> calls.add("segunda")));

        menu.run();

        assertThat(calls).containsExactly("segunda", "primera", "segunda");
        assertThat(output()).contains("=== Prueba ===", "1. Primera", "2. Segunda", "0. Volver");
    }

    @Test
    @DisplayName("an option number outside the list is reported and the menu keeps going")
    void invalidOptionIsReported() {
        Menu menu = menu(consoleWith("7", "0"), new MenuOption("Única", () -> calls.add("única")));

        menu.run();

        assertThat(calls).isEmpty();
        assertThat(output()).contains("Opción no válida");
    }

    @Test
    @DisplayName("business and data errors are shown and do not end the menu")
    void errorsAreShownAndTheMenuContinues() {
        Menu menu = menu(consoleWith("1", "2", "3", "4", "0"),
            new MenuOption("Valida", () -> {
                throw new ValidationException("dato inválido");
            }),
            new MenuOption("Regla", () -> {
                throw new BusinessRuleException("regla rota");
            }),
            new MenuOption("Datos", () -> {
                throw new DataAccessException("disco lleno");
            }),
            new MenuOption("Bien", () -> calls.add("bien")));

        menu.run();

        assertThat(calls).containsExactly("bien");
        assertThat(output()).contains("No se pudo completar: dato inválido", "No se pudo completar: regla rota",
            "Error de datos: disco lleno");
    }

    @Test
    @DisplayName("the menu stops with ConsoleClosedException when the input ends")
    void endOfInputStopsTheMenu() {
        Menu menu = menu(consoleWith("1"), new MenuOption("Única", () -> calls.add("única")));

        assertThatThrownBy(menu::run).isInstanceOf(ConsoleClosedException.class);
        assertThat(calls).containsExactly("única");
    }
}
