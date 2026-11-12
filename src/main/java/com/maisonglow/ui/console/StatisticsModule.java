package com.maisonglow.ui.console;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.service.AdminNotifier;
import com.maisonglow.service.CustomerStatistics;
import com.maisonglow.service.LowRotationService;
import com.maisonglow.service.PromotionService;
import com.maisonglow.service.PromotionSuggestion;
import com.maisonglow.service.StatisticsService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Console module for statistics, low-rotation products and promotion
 * suggestions. It only reads: nothing typed here changes the stored data.
 */
public final class StatisticsModule extends MenuModule {

    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");

    private final StatisticsService statistics;
    private final LowRotationService lowRotation;
    private final PromotionService promotions;
    private final AdminNotifier adminNotifier;
    private final Clock clock;

    /**
     * Creates the module.
     *
     * @param console       where the module talks to the user
     * @param statistics    the statistics service
     * @param lowRotation   the low-rotation service
     * @param promotions    the promotion suggestion service
     * @param adminNotifier tells the administrator which products rotate little
     * @param clock         gives today's date
     */
    public StatisticsModule(ConsoleIo console, StatisticsService statistics, LowRotationService lowRotation,
                            PromotionService promotions, AdminNotifier adminNotifier, Clock clock) {
        super("Estadísticas y promociones", console);
        this.statistics = Objects.requireNonNull(statistics, "statistics");
        this.lowRotation = Objects.requireNonNull(lowRotation, "lowRotation");
        this.promotions = Objects.requireNonNull(promotions, "promotions");
        this.adminNotifier = Objects.requireNonNull(adminNotifier, "adminNotifier");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    protected List<MenuOption> options() {
        return List.of(
            new MenuOption("Estadísticas de un cliente", this::customerStatistics),
            new MenuOption("Productos más vendidos en un periodo", this::topProducts),
            new MenuOption("Tratamientos más solicitados", this::topTreatments),
            new MenuOption("Clientes frecuentes", this::frequentCustomers),
            new MenuOption("Productos de baja rotación (avisa al administrador)", this::lowRotationProducts),
            new MenuOption("Promociones sugeridas para un cliente", this::suggestPromotions));
    }

    private void customerStatistics() throws DataAccessException {
        CustomerStatistics result = statistics.customerStatistics(console().readInt("Id del cliente"));
        console().println(result.purchases() + " compra(s) por " + ConsoleFormat.money(result.totalSpent()) + ", "
            + result.appointments() + " cita(s), " + result.noShows() + " inasistencia(s)");
    }

    private void topProducts() throws ValidationException, DataAccessException {
        int limit = console().readInt("Cuántos productos mostrar");
        LocalDate from = console().readDate("Desde");
        LocalDate to = console().readDate("Hasta");
        printRanking(statistics.topProducts(limit, from, to));
    }

    private void topTreatments() throws ValidationException, DataAccessException {
        printRanking(statistics.topTreatments(console().readInt("Cuántos tratamientos mostrar")));
    }

    private void frequentCustomers() throws ValidationException, DataAccessException {
        printRanking(statistics.frequentCustomers(console().readInt("Cuántos clientes mostrar")));
    }

    private void lowRotationProducts() throws ValidationException, DataAccessException {
        LocalDate today = LocalDate.now(clock);
        if (lowRotation.notifyAdministrator(adminNotifier, today).isEmpty()) {
            console().println("No hay productos de baja rotación.");
        }
    }

    private void suggestPromotions() throws ValidationException, DataAccessException {
        int customerId = console().readInt("Id del cliente");
        List<PromotionSuggestion> suggestions = promotions.suggestPromotions(customerId, LocalDate.now(clock));
        if (suggestions.isEmpty()) {
            console().println("No hay promociones para sugerir a este cliente.");
            return;
        }
        for (PromotionSuggestion suggestion : suggestions) {
            console().println("  - " + suggestion.productName() + ": " + ConsoleFormat.money(suggestion.regularPrice())
                + " -> " + ConsoleFormat.money(suggestion.promotionalPrice())
                + String.format(COLOMBIA, " (%.0f %% de descuento)", suggestion.discountPercentage()));
        }
    }

    private void printRanking(Map<String, Integer> ranking) {
        if (ranking.isEmpty()) {
            console().println("Todavía no hay datos para este reporte.");
            return;
        }
        ranking.forEach((name, count) -> console().println("  " + name + ": " + count));
    }
}
