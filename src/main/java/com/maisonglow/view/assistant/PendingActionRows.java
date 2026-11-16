package com.maisonglow.view.assistant;

import com.maisonglow.ai.PendingAction;
import com.maisonglow.util.CurrencyFormatter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * How an action that waits for confirmation is shown to the user: a title in words and one row per parameter,
 * with the names of the customer, the professional and the treatment instead of their numbers, so the person can
 * check what is about to happen before pressing "Confirmar".
 */
public final class PendingActionRows {

    /** One line of the table of the confirmation: what it is and its value. */
    public record Row(String label, String value) { }

    private static final Map<String, String> TITLES = Map.of("book_appointment", "Agendar cita");
    private static final Map<String, String> LABELS = Map.of("customerId", "Cliente", "professionalId", "Profesional",
        "treatmentId", "Tratamiento", "start", "Inicio", "deposit", "Anticipo");

    private final IntFunction<String> customerName;
    private final IntFunction<String> professionalName;
    private final IntFunction<String> treatmentName;

    /**
     * @param customerName     the name of a customer from its number, or null if it cannot be found
     * @param professionalName the name of a professional from its number, or null if it cannot be found
     * @param treatmentName    the name of a treatment from its number, or null if it cannot be found
     */
    public PendingActionRows(IntFunction<String> customerName, IntFunction<String> professionalName,
                             IntFunction<String> treatmentName) {
        this.customerName = customerName;
        this.professionalName = professionalName;
        this.treatmentName = treatmentName;
    }

    /**
     * @param action the action that waits for confirmation
     * @return its title: the name of the action in words, or the name of the tool if it has none
     */
    public static String titleOf(PendingAction action) {
        return TITLES.getOrDefault(action.toolName(), action.toolName());
    }

    /**
     * @param action the action that waits for confirmation
     * @return one row per parameter, in a fixed order, with the names found for the numbers
     */
    public List<Row> rowsOf(PendingAction action) {
        List<Row> rows = new ArrayList<>();
        for (String key : List.of("customerId", "professionalId", "treatmentId", "start", "deposit")) {
            if (action.parameters().containsKey(key)) {
                rows.add(new Row(LABELS.get(key), valueOf(key, action.parameters().get(key))));
            }
        }
        action.parameters().entrySet().stream().filter(entry -> !LABELS.containsKey(entry.getKey()))
            .sorted(Map.Entry.comparingByKey()).forEach(entry -> rows.add(new Row(entry.getKey(), entry.getValue())));
        return rows;
    }

    private String valueOf(String key, String raw) {
        try {
            String name = switch (key) {
                case "customerId" -> customerName.apply(Integer.parseInt(raw.trim()));
                case "professionalId" -> professionalName.apply(Integer.parseInt(raw.trim()));
                case "treatmentId" -> treatmentName.apply(Integer.parseInt(raw.trim()));
                case "deposit" -> CurrencyFormatter.format(new BigDecimal(raw.trim()));
                default -> null;
            };
            return name == null ? (key.endsWith("Id") ? "n.º " + raw.trim() : raw.replace('T', ' ')) : name;
        } catch (NumberFormatException e) {
            return raw;
        }
    }
}
