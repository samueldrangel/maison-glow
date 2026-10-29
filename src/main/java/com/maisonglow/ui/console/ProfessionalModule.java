package com.maisonglow.ui.console;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Professional;
import com.maisonglow.service.ProfessionalService;
import com.maisonglow.service.WorkSchedule;
import java.util.List;
import java.util.Objects;

/**
 * Console module for professionals: register, update, change the working
 * hours, assign treatments, list and activate or deactivate. It delegates
 * every rule to {@link ProfessionalService}.
 */
public final class ProfessionalModule extends MenuModule {

    private static final String NONE_FOUND = "No hay profesionales que mostrar.";

    private final ProfessionalService professionals;

    /**
     * Creates the module.
     *
     * @param console       where the module talks to the user
     * @param professionals the professional service
     */
    public ProfessionalModule(ConsoleIo console, ProfessionalService professionals) {
        super("Profesionales", console);
        this.professionals = Objects.requireNonNull(professionals, "professionals");
    }

    @Override
    protected List<MenuOption> options() {
        return List.of(
            new MenuOption("Registrar profesional", this::register),
            new MenuOption("Listar profesionales", this::listAll),
            new MenuOption("Ver profesional por id", this::findById),
            new MenuOption("Actualizar datos de un profesional", this::update),
            new MenuOption("Cambiar la jornada de trabajo", this::changeWorkingHours),
            new MenuOption("Asignar un tratamiento", this::assignTreatment),
            new MenuOption("Quitar un tratamiento", this::removeTreatment),
            new MenuOption("Listar profesionales que ofrecen un tratamiento", this::listByTreatment),
            new MenuOption("Activar profesional", this::activate),
            new MenuOption("Desactivar profesional", this::deactivate));
    }

    private WorkSchedule askSchedule() {
        return new WorkSchedule(console().readTime("Hora de inicio de la jornada"),
            console().readTime("Hora de fin de la jornada"));
    }

    private void register() throws ValidationException, BusinessRuleException, DataAccessException {
        var contact = ContactPrompt.ask(console());
        String specialty = console().readLine("Especialidad");
        Professional professional = professionals.register(contact, specialty, askSchedule());
        console().println("Profesional registrado: " + ConsoleFormat.professional(professional));
    }

    private void listAll() throws DataAccessException {
        ConsoleFormat.printAll(console(), NONE_FOUND,
            professionals.listAll().stream().map(ConsoleFormat::professional).toList());
    }

    private void findById() throws BusinessRuleException, DataAccessException {
        console().println(ConsoleFormat.professional(professionals.findById(console().readInt("Id del profesional"))));
    }

    private void update() throws ValidationException, BusinessRuleException, DataAccessException {
        int id = console().readInt("Id del profesional");
        console().println("Datos actuales: " + ConsoleFormat.professional(professionals.findById(id)));
        console().println("Escriba los datos nuevos (el documento debe ser el mismo).");
        var contact = ContactPrompt.ask(console());
        String specialty = console().readLine("Especialidad");
        Professional professional = professionals.update(id, contact, specialty);
        console().println("Profesional actualizado: " + ConsoleFormat.professional(professional));
    }

    private void changeWorkingHours() throws ValidationException, BusinessRuleException, DataAccessException {
        int id = console().readInt("Id del profesional");
        professionals.changeWorkingHours(id, askSchedule());
        console().println("Jornada actualizada: " + ConsoleFormat.professional(professionals.findById(id)));
    }

    private void assignTreatment() throws BusinessRuleException, DataAccessException {
        int professionalId = console().readInt("Id del profesional");
        professionals.assignTreatment(professionalId, console().readInt("Id del tratamiento"));
        console().println("Tratamiento asignado: " + ConsoleFormat.professional(professionals.findById(professionalId)));
    }

    private void removeTreatment() throws BusinessRuleException, DataAccessException {
        int professionalId = console().readInt("Id del profesional");
        professionals.removeTreatment(professionalId, console().readInt("Id del tratamiento"));
        console().println("Tratamiento quitado: " + ConsoleFormat.professional(professionals.findById(professionalId)));
    }

    private void listByTreatment() throws DataAccessException {
        int treatmentId = console().readInt("Id del tratamiento");
        ConsoleFormat.printAll(console(), NONE_FOUND,
            professionals.listByTreatment(treatmentId).stream().map(ConsoleFormat::professional).toList());
    }

    private void activate() throws BusinessRuleException, DataAccessException {
        int id = console().readInt("Id del profesional");
        professionals.activate(id);
        console().println("Profesional activado: " + ConsoleFormat.professional(professionals.findById(id)));
    }

    private void deactivate() throws BusinessRuleException, DataAccessException {
        int id = console().readInt("Id del profesional");
        professionals.deactivate(id);
        console().println("Profesional desactivado: " + ConsoleFormat.professional(professionals.findById(id)));
    }
}
