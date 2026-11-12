package com.maisonglow;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.ui.console.ConsoleIo;
import com.maisonglow.ui.console.MainMenu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Interactive console tests")
class InteractiveConsoleTest {

    private static final String[] ANA = {"1065123456", "Ana Pérez", "3001234567", "ana@correo.com"};

    @TempDir
    Path data;

    @BeforeEach
    void copyPreloadedFiles() throws IOException {
        Files.copy(Path.of("data", "settings.txt"), data.resolve("settings.txt"));
        Files.copy(Path.of("data", "categories.txt"), data.resolve("categories.txt"));
    }

    private String session(String... answers) throws ValidationException, DataAccessException {
        byte[] input = (String.join("\n", answers) + "\n").getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream printed = new ByteArrayOutputStream();
        ConsoleIo console = new ConsoleIo(new ByteArrayInputStream(input), StandardCharsets.UTF_8,
            new PrintStream(printed, true, StandardCharsets.UTF_8));
        new MainMenu(console, new Main(data).consoleModules(console)).run();
        return printed.toString(StandardCharsets.UTF_8);
    }

    private static String[] lines(String[]... parts) {
        return java.util.Arrays.stream(parts).flatMap(java.util.Arrays::stream).toArray(String[]::new);
    }

    @Test
    @DisplayName("registers a customer, lists it and rejects the same document twice")
    void registersAndRejectsDuplicates() throws Exception {
        String output = session(lines(
            new String[] {"1", "1"}, ANA, new String[] {"1"}, ANA, new String[] {"2", "0", "0"}));

        assertThat(output).contains("Cliente registrado: [1] Ana Pérez");
        assertThat(output).contains("No se pudo completar:");
        assertThat(output).contains("  [1] Ana Pérez · doc. 1065123456");
        assertThat(output).contains("Hasta pronto.");
    }

    @Test
    @DisplayName("data typed in one session is still there in the next one")
    void dataSurvivesBetweenSessions() throws Exception {
        session(lines(new String[] {"1", "1"}, ANA, new String[] {"0", "0"}));

        String output = session("1", "2", "0", "0");

        assertThat(output).contains("[1] Ana Pérez");
    }

    @Test
    @DisplayName("a sale typed in the console deducts the stock and sends the invoice")
    void saleDeductsStock() throws Exception {
        String output = session(lines(
            new String[] {"1", "1"}, ANA, new String[] {"0"},
            new String[] {"4", "1", "Shampoo hidratante", "1", "28000", "2", "5", "0"},
            new String[] {"6", "1", "1", "1", "1", "3", "0", "s", "0"},
            new String[] {"4", "2", "0", "0"}));

        assertThat(output).contains("Producto registrado: [1] Shampoo hidratante");
        assertThat(output).contains("3 x Shampoo hidratante a $ 28.000 = $ 84.000");
        assertThat(output).contains("Venta registrada: [1]");
        assertThat(output).contains("[1] Shampoo hidratante · Cabello · $ 28.000 · 2 und. (mínimo 2)");
    }

    @Test
    @DisplayName("a sale with more units than the stock is rejected and the stock does not change")
    void saleWithoutStockIsRejected() throws Exception {
        String output = session(lines(
            new String[] {"1", "1"}, ANA, new String[] {"0"},
            new String[] {"4", "1", "Esmalte rojo", "3", "12000", "1", "2", "0"},
            new String[] {"6", "1", "1", "1", "1", "5", "0", "s", "0"},
            new String[] {"4", "2", "0", "0"}));

        assertThat(output).contains("No se pudo completar:");
        assertThat(output).contains("[1] Esmalte rojo · Uñas · $ 12.000 · 2 und. (mínimo 1)");
        assertThat(output).doesNotContain("Venta registrada");
    }

    @Test
    @DisplayName("the console ends cleanly when the input ends in the middle of a menu")
    void endsCleanlyWhenInputEnds() throws Exception {
        String output = session("1");

        assertThat(output).contains("=== Clientes ===");
        assertThat(output).contains("Hasta pronto.");
    }
}
