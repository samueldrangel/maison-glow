package com.maisonglow.view.assistant;

import com.maisonglow.ai.PendingAction;
import com.maisonglow.util.CurrencyFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PendingActionRows tests")
class PendingActionRowsTest {

    private final PendingActionRows rows = new PendingActionRows(id -> id == 1 ? "Ana García" : null,
        id -> id == 2 ? "Camila Ruiz" : null, id -> id == 3 ? "Corte y peinado" : null);

    private static PendingAction booking(Map<String, String> parameters) {
        return new PendingAction(UUID.randomUUID(), "book_appointment", parameters);
    }

    @Test
    @DisplayName("the title of a known action is in words and an unknown one keeps its tool name")
    void titles() {
        assertThat(PendingActionRows.titleOf(booking(Map.of()))).isEqualTo("Agendar cita");
        assertThat(PendingActionRows.titleOf(new PendingAction(UUID.randomUUID(), "other_tool", Map.of()))).isEqualTo("other_tool");
    }

    @Test
    @DisplayName("shows the names of the customer, the professional and the treatment instead of their numbers, in a fixed order")
    void namesInsteadOfNumbers() {
        var result = rows.rowsOf(booking(Map.of("start", "2026-11-17T10:00", "treatmentId", "3", "customerId", "1",
            "professionalId", "2", "deposit", "16500")));

        assertThat(result).extracting(PendingActionRows.Row::label)
            .containsExactly("Cliente", "Profesional", "Tratamiento", "Inicio", "Anticipo");
        assertThat(result).extracting(PendingActionRows.Row::value)
            .containsExactly("Ana García", "Camila Ruiz", "Corte y peinado", "2026-11-17 10:00",
                CurrencyFormatter.format(new java.math.BigDecimal("16500")));
    }

    @Test
    @DisplayName("a number that matches nobody is shown as it came, and a value that is not a number is not changed")
    void unknownNumbers() {
        var result = rows.rowsOf(booking(Map.of("customerId", "99", "professionalId", "abc")));

        assertThat(result).extracting(PendingActionRows.Row::value).containsExactly("n.º 99", "abc");
    }

    @Test
    @DisplayName("parameters of another tool are listed after the known ones, by name")
    void otherParameters() {
        var result = rows.rowsOf(new PendingAction(UUID.randomUUID(), "other_tool", Map.of("b", "2", "a", "1")));

        assertThat(result).extracting(PendingActionRows.Row::label).containsExactly("a", "b");
    }
}
