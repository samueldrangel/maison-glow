package com.maisonglow.ui.console;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ConsoleIo tests")
class ConsoleIoTest {

    private final ByteArrayOutputStream printed = new ByteArrayOutputStream();

    private ConsoleIo consoleWith(String... answers) {
        byte[] input = (String.join("\n", answers) + "\n").getBytes(StandardCharsets.UTF_8);
        return new ConsoleIo(new ByteArrayInputStream(input), StandardCharsets.UTF_8,
            new PrintStream(printed, true, StandardCharsets.UTF_8));
    }

    private String output() {
        return printed.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("readLine trims the answer and keeps accents")
    void readLineTrimsTheAnswer() {
        ConsoleIo console = consoleWith("  Ana Pérez  ");

        assertThat(console.readLine("Nombre")).isEqualTo("Ana Pérez");
        assertThat(output()).isEqualTo("Nombre: ");
    }

    @Test
    @DisplayName("readInt asks again until the answer is a whole number")
    void readIntAsksAgain() {
        ConsoleIo console = consoleWith("abc", "4.5", "12");

        assertThat(console.readInt("Id")).isEqualTo(12);
        assertThat(output()).contains("Ingrese un número entero válido.");
    }

    @Test
    @DisplayName("readDouble accepts a comma as the decimal separator")
    void readDoubleAcceptsComma() {
        ConsoleIo console = consoleWith("no", "12,5");

        assertThat(console.readDouble("Precio")).isEqualTo(12.5);
    }

    @Test
    @DisplayName("readDate, readTime and readDateTime ask again until the format is right")
    void readDatesAskAgain() {
        ConsoleIo console = consoleWith("29/10/2026", "2026-10-29", "8am", "08:30", "ayer", "2026-10-29 10:00");

        assertThat(console.readDate("Fecha")).isEqualTo(LocalDate.of(2026, 10, 29));
        assertThat(console.readTime("Hora")).isEqualTo(LocalTime.of(8, 30));
        assertThat(console.readDateTime("Inicio")).isEqualTo(LocalDateTime.of(2026, 10, 29, 10, 0));
        assertThat(output()).contains("Fecha no válida", "Hora no válida", "Fecha y hora no válidas");
    }

    @Test
    @DisplayName("confirm is true only for an affirmative answer")
    void confirmAcceptsOnlyYes() {
        ConsoleIo console = consoleWith("s", "Sí", "n", "quizás");

        assertThat(console.confirm("¿Seguro?")).isTrue();
        assertThat(console.confirm("¿Seguro?")).isTrue();
        assertThat(console.confirm("¿Seguro?")).isFalse();
        assertThat(console.confirm("¿Seguro?")).isFalse();
    }

    @Test
    @DisplayName("reading past the end of the input throws ConsoleClosedException")
    void endOfInputThrows() {
        ConsoleIo console = consoleWith("1");
        console.readInt("Id");

        assertThatThrownBy(() -> console.readInt("Id")).isInstanceOf(ConsoleClosedException.class);
    }
}
