package com.maisonglow.ui.console;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.TreatmentData;
import com.maisonglow.service.TreatmentService;
import java.util.List;
import java.util.Objects;

/**
 * Console module for the treatment catalog: register, update, list, activate
 * and deactivate. It delegates every rule to {@link TreatmentService}.
 */
public final class TreatmentModule extends MenuModule {

    private static final String NONE_FOUND = "No hay tratamientos que mostrar.";

    private final TreatmentService treatments;

    /**
     * Creates the module.
     *
     * @param console    where the module talks to the user
     * @param treatments the treatment service
     */
    public TreatmentModule(ConsoleIo console, TreatmentService treatments) {
        super("Tratamientos", console);
        this.treatments = Objects.requireNonNull(treatments, "treatments");
    }

    @Override
    protected List<MenuOption> options() {
        return List.of(
            new MenuOption("Registrar tratamiento", this::register),
            new MenuOption("Listar todos los tratamientos", this::listAll),
            new MenuOption("Listar tratamientos activos", this::listActive),
            new MenuOption("Ver tratamiento por id", this::findById),
            new MenuOption("Actualizar tratamiento", this::update),
            new MenuOption("Activar tratamiento", this::activate),
            new MenuOption("Desactivar tratamiento", this::deactivate));
    }

    private TreatmentData askData() {
        String name = console().readLine("Nombre");
        String description = console().readLine("Descripción (opcional)");
        double price = console().readDouble("Precio");
        int minutes = console().readInt("Duración en minutos");
        return new TreatmentData(name, description, price, minutes);
    }

    private void register() throws ValidationException, BusinessRuleException, DataAccessException {
        Treatment treatment = treatments.register(askData());
        console().println("Tratamiento registrado: " + ConsoleFormat.treatment(treatment));
    }

    private void listAll() throws DataAccessException {
        ConsoleFormat.printAll(console(), NONE_FOUND,
            treatments.listAll().stream().map(ConsoleFormat::treatment).toList());
    }

    private void listActive() throws DataAccessException {
        ConsoleFormat.printAll(console(), NONE_FOUND,
            treatments.listActive().stream().map(ConsoleFormat::treatment).toList());
    }

    private void findById() throws BusinessRuleException, DataAccessException {
        console().println(ConsoleFormat.treatment(treatments.findById(console().readInt("Id del tratamiento"))));
    }

    private void update() throws ValidationException, BusinessRuleException, DataAccessException {
        int id = console().readInt("Id del tratamiento");
        console().println("Datos actuales: " + ConsoleFormat.treatment(treatments.findById(id)));
        Treatment treatment = treatments.update(id, askData());
        console().println("Tratamiento actualizado: " + ConsoleFormat.treatment(treatment));
    }

    private void activate() throws BusinessRuleException, DataAccessException {
        int id = console().readInt("Id del tratamiento");
        treatments.activate(id);
        console().println("Tratamiento activado: " + ConsoleFormat.treatment(treatments.findById(id)));
    }

    private void deactivate() throws BusinessRuleException, DataAccessException {
        int id = console().readInt("Id del tratamiento");
        treatments.deactivate(id);
        console().println("Tratamiento desactivado: " + ConsoleFormat.treatment(treatments.findById(id)));
    }
}
