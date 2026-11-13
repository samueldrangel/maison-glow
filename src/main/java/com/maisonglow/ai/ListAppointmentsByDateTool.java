package com.maisonglow.ai;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.service.AppointmentService;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Lists the day's appointments through AppointmentService. */
public final class ListAppointmentsByDateTool implements AssistantTool {
    private final AppointmentService appointments;

    /** Creates a read-only schedule tool. */
    public ListAppointmentsByDateTool(AppointmentService appointments) {
        this.appointments = Objects.requireNonNull(appointments, "appointments");
    }

    @Override public String getName() { return "list_appointments_by_date"; }
    @Override public String getDescription() { return "Consultar citas de una fecha"; }
    @Override public Map<String, String> getParameters() { return Map.of("date", "Fecha yyyy-MM-dd"); }
    @Override public boolean requiresConfirmation() { return false; }

    @Override
    public String execute(Map<String, String> parameters) throws ValidationException, DataAccessException {
        var found = appointments.listByDate(ToolArguments.date(parameters, "date"));
        return found.isEmpty() ? "No hay citas para esa fecha."
            : found.stream().map(appointment -> "[" + appointment.getId() + "] " + appointment
                + " — " + appointment.getCustomer().getName()).collect(Collectors.joining("\n"));
    }
}
