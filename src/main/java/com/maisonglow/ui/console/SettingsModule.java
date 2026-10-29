package com.maisonglow.ui.console;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.service.SettingsService;
import java.util.List;
import java.util.Objects;

/**
 * Console module for the business settings: shows the parameters that the
 * appointment rules and the statistics read, and changes one of them.
 * {@link SettingsService} validates the new value.
 */
public final class SettingsModule extends MenuModule {

    private static final List<String> KEYS = List.of(
        SettingsService.BUSINESS_NAME,
        SettingsService.DEPOSIT_PERCENTAGE,
        SettingsService.MIN_BOOKING_NOTICE_HOURS,
        SettingsService.REMINDER_HOURS_BEFORE,
        SettingsService.NO_SHOW_PENALTY_THRESHOLD,
        SettingsService.LOW_ROTATION_WINDOW_DAYS,
        SettingsService.LOW_ROTATION_TOP_N,
        SettingsService.LOW_ROTATION_DISCOUNT_PERCENTAGE);

    private final SettingsService settings;

    /**
     * Creates the module.
     *
     * @param console  where the module talks to the user
     * @param settings the settings service
     */
    public SettingsModule(ConsoleIo console, SettingsService settings) {
        super("Configuración del negocio", console);
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    @Override
    protected List<MenuOption> options() {
        return List.of(
            new MenuOption("Ver la configuración actual", this::show),
            new MenuOption("Cambiar un valor", this::change));
    }

    private void show() throws ValidationException, DataAccessException {
        console().println("  Nombre del negocio: " + settings.businessName());
        console().println("  Porcentaje de anticipo: " + settings.depositPercentage() + " %");
        console().println("  Aviso mínimo para agendar: " + settings.minBookingNoticeHours() + " h");
        console().println("  Recordatorio antes de la cita: " + settings.reminderHoursBefore() + " h");
        console().println("  Inasistencias para exigir pago completo: " + settings.noShowPenaltyThreshold());
        console().println("  Ventana de baja rotación: " + settings.lowRotationWindowDays() + " días");
        console().println("  Productos en el aviso de baja rotación: " + settings.lowRotationTopN());
        console().println("  Descuento de baja rotación: " + settings.lowRotationDiscountPercentage() + " %");
    }

    private void change() throws ValidationException, DataAccessException {
        for (int i = 0; i < KEYS.size(); i++) {
            console().println("  " + (i + 1) + ". " + KEYS.get(i));
        }
        int choice = console().readInt("Número del parámetro");
        if (choice < 1 || choice > KEYS.size()) {
            console().println("Opción no válida.");
            return;
        }
        String key = KEYS.get(choice - 1);
        settings.update(key, console().readLine("Valor nuevo para " + key));
        console().println("Valor actualizado.");
        show();
    }
}
