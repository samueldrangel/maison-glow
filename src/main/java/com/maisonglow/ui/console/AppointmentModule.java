package com.maisonglow.ui.console;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.AppointmentService;
import com.maisonglow.service.CustomerService;
import com.maisonglow.service.ProfessionalService;
import com.maisonglow.service.TreatmentService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Console module for appointments: book, check availability, list by day and
 * change the state of an appointment. The business rules (schedule conflicts,
 * deposit, notice) are applied by {@link AppointmentService}; this class only
 * asks for the data and shows the result.
 */
public final class AppointmentModule extends MenuModule {

    private final AppointmentService appointments;
    private final CustomerService customers;
    private final ProfessionalService professionals;
    private final TreatmentService treatments;

    /**
     * Creates the module.
     *
     * @param console       where the module talks to the user
     * @param appointments  the appointment service
     * @param customers     the customer service, to find who books
     * @param professionals the professional service, to find who attends
     * @param treatments    the treatment service, to find what is booked
     */
    public AppointmentModule(ConsoleIo console, AppointmentService appointments, CustomerService customers,
                             ProfessionalService professionals, TreatmentService treatments) {
        super("Citas", console);
        this.appointments = Objects.requireNonNull(appointments, "appointments");
        this.customers = Objects.requireNonNull(customers, "customers");
        this.professionals = Objects.requireNonNull(professionals, "professionals");
        this.treatments = Objects.requireNonNull(treatments, "treatments");
    }

    @Override
    protected List<MenuOption> options() {
        return List.of(
            new MenuOption("Agendar cita", this::book),
            new MenuOption("Ver la agenda de un día", this::listByDate),
            new MenuOption("Verificar disponibilidad de un profesional", this::checkAvailability),
            new MenuOption("Reprogramar cita", this::reschedule),
            new MenuOption("Confirmar cita", this::confirm),
            new MenuOption("Cancelar cita", this::cancel),
            new MenuOption("Marcar cita como completada", this::complete),
            new MenuOption("Marcar inasistencia del cliente", this::markNoShow));
    }

    private void book() throws ValidationException, BusinessRuleException, DataAccessException {
        Customer customer = customers.findById(console().readInt("Id del cliente"));
        Professional professional = professionals.findById(console().readInt("Id del profesional"));
        Treatment treatment = treatments.findById(console().readInt("Id del tratamiento"));
        LocalDateTime start = console().readDateTime("Fecha y hora de inicio");
        double deposit = console().readDouble("Anticipo pagado (con 0 el sistema indica el mínimo exigido)");
        Appointment appointment = appointments.book(customer, professional, treatment, start, deposit);
        console().println("Cita agendada: " + ConsoleFormat.appointment(appointment));
        console().println("Recordatorio programado para " + ConsoleFormat.dateTime(appointment.getReminderAt()));
    }

    private void listByDate() throws ValidationException, DataAccessException {
        ConsoleFormat.printAll(console(), "No hay citas ese día.",
            appointments.listByDate(console().readDate("Fecha")).stream().map(ConsoleFormat::appointment).toList());
    }

    private void checkAvailability() throws BusinessRuleException, DataAccessException, ValidationException {
        Professional professional = professionals.findById(console().readInt("Id del profesional"));
        Treatment treatment = treatments.findById(console().readInt("Id del tratamiento"));
        LocalDateTime start = console().readDateTime("Fecha y hora de inicio");
        boolean free = appointments.checkAvailability(professional, start, treatment.calculateEnd(start));
        console().println(free ? "El profesional está disponible en ese horario."
            : "El profesional NO está disponible en ese horario.");
    }

    private void reschedule() throws ValidationException, BusinessRuleException, DataAccessException {
        int id = console().readInt("Id de la cita");
        Appointment moved = appointments.reschedule(id, console().readDateTime("Nueva fecha y hora de inicio"));
        console().println("Cita reprogramada: " + ConsoleFormat.appointment(moved));
    }

    private void confirm() throws BusinessRuleException, DataAccessException {
        int id = console().readInt("Id de la cita");
        appointments.confirm(id);
        console().println("Cita " + id + " confirmada.");
    }

    private void cancel() throws BusinessRuleException, DataAccessException {
        int id = console().readInt("Id de la cita");
        appointments.cancel(id);
        console().println("Cita " + id + " cancelada.");
    }

    private void complete() throws BusinessRuleException, DataAccessException {
        int id = console().readInt("Id de la cita");
        appointments.complete(id);
        console().println("Cita " + id + " completada.");
    }

    private void markNoShow() throws BusinessRuleException, DataAccessException {
        int id = console().readInt("Id de la cita");
        appointments.markNoShow(id);
        console().println("Cita " + id + " marcada como inasistencia.");
    }
}
