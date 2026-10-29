package com.maisonglow.ui.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Reads typed answers from the user and prints text, so the menus never touch
 * the standard streams directly. Numbers, dates and times are asked again
 * until they are valid; text is returned as typed and the services validate it.
 */
public final class ConsoleIo {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> YES_ANSWERS = Set.of("s", "si", "sí", "y", "yes");

    private final BufferedReader input;
    private final PrintStream output;

    /**
     * Creates the console.
     *
     * @param input   where the answers are read from
     * @param charset how the bytes of the input are decoded
     * @param output  where the text is printed
     */
    public ConsoleIo(InputStream input, Charset charset, PrintStream output) {
        this.input = new BufferedReader(new InputStreamReader(Objects.requireNonNull(input, "input"),
            Objects.requireNonNull(charset, "charset")));
        this.output = Objects.requireNonNull(output, "output");
    }

    /**
     * Creates the console over the keyboard and the screen. The keyboard is
     * decoded with the terminal encoding when the JVM reports it, so accents
     * typed in a Windows terminal are read correctly.
     *
     * @return the console of the running program
     */
    public static ConsoleIo system() {
        Charset terminal = Charset.forName(System.getProperty("stdin.encoding", Charset.defaultCharset().name()));
        return new ConsoleIo(System.in, terminal, System.out);
    }

    /**
     * Prints a line.
     *
     * @param text the text to print
     */
    public void println(String text) {
        output.println(text);
    }

    /**
     * Prints an empty line.
     */
    public void println() {
        output.println();
    }

    /**
     * Prints a title surrounded by a blank line and marks.
     *
     * @param text the title
     */
    public void title(String text) {
        output.println();
        output.println("=== " + text + " ===");
    }

    /**
     * Asks for a line of text.
     *
     * @param prompt what is asked
     * @return the answer without surrounding spaces, possibly empty
     * @throws ConsoleClosedException if the input has ended
     */
    public String readLine(String prompt) {
        output.print(prompt + ": ");
        output.flush();
        try {
            String line = input.readLine();
            if (line == null) {
                throw new ConsoleClosedException();
            }
            return line.trim();
        } catch (IOException e) {
            throw new ConsoleClosedException();
        }
    }

    /**
     * Asks for a whole number until a valid one is typed.
     *
     * @param prompt what is asked
     * @return the number
     * @throws ConsoleClosedException if the input has ended
     */
    public int readInt(String prompt) {
        while (true) {
            String answer = readLine(prompt);
            try {
                return Integer.parseInt(answer);
            } catch (NumberFormatException e) {
                output.println("Ingrese un número entero válido.");
            }
        }
    }

    /**
     * Asks for a decimal number until a valid one is typed. A comma is
     * accepted as the decimal separator.
     *
     * @param prompt what is asked
     * @return the number
     * @throws ConsoleClosedException if the input has ended
     */
    public double readDouble(String prompt) {
        while (true) {
            String answer = readLine(prompt).replace(',', '.');
            try {
                return Double.parseDouble(answer);
            } catch (NumberFormatException e) {
                output.println("Ingrese un número válido, por ejemplo 28000 o 12.5.");
            }
        }
    }

    /**
     * Asks for a date written as {@code yyyy-MM-dd} until a valid one is typed.
     *
     * @param prompt what is asked
     * @return the date
     * @throws ConsoleClosedException if the input has ended
     */
    public LocalDate readDate(String prompt) {
        while (true) {
            try {
                return LocalDate.parse(readLine(prompt + " (aaaa-mm-dd)"), DATE_FORMAT);
            } catch (DateTimeParseException e) {
                output.println("Fecha no válida. Use el formato aaaa-mm-dd, por ejemplo 2026-10-29.");
            }
        }
    }

    /**
     * Asks for a time written as {@code HH:mm} until a valid one is typed.
     *
     * @param prompt what is asked
     * @return the time
     * @throws ConsoleClosedException if the input has ended
     */
    public LocalTime readTime(String prompt) {
        while (true) {
            try {
                return LocalTime.parse(readLine(prompt + " (hh:mm)"), TIME_FORMAT);
            } catch (DateTimeParseException e) {
                output.println("Hora no válida. Use el formato hh:mm, por ejemplo 08:30.");
            }
        }
    }

    /**
     * Asks for a date and time written as {@code yyyy-MM-dd HH:mm} until a
     * valid one is typed.
     *
     * @param prompt what is asked
     * @return the date and time
     * @throws ConsoleClosedException if the input has ended
     */
    public LocalDateTime readDateTime(String prompt) {
        while (true) {
            try {
                return LocalDateTime.parse(readLine(prompt + " (aaaa-mm-dd hh:mm)"), DATE_TIME_FORMAT);
            } catch (DateTimeParseException e) {
                output.println("Fecha y hora no válidas. Use el formato aaaa-mm-dd hh:mm, por ejemplo 2026-10-29 10:00.");
            }
        }
    }

    /**
     * Asks a yes or no question.
     *
     * @param prompt what is asked
     * @return {@code true} if the answer is yes ({@code s}, {@code si}, {@code y})
     * @throws ConsoleClosedException if the input has ended
     */
    public boolean confirm(String prompt) {
        return YES_ANSWERS.contains(readLine(prompt + " (s/n)").toLowerCase(Locale.ROOT));
    }
}
