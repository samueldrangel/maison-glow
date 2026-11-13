package com.maisonglow.ai;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.service.AppointmentService;
import com.maisonglow.service.ProfessionalService;

import java.util.Map;
import java.util.Objects;

/** Checks an interval against the professional's current schedule. */
public final class CheckAvailabilityTool implements AssistantTool {
    private final AppointmentService appointments;
    private final ProfessionalService professionals;

    /** Creates a read-only availability tool. */
    public CheckAvailabilityTool(AppointmentService appointments, ProfessionalService professionals) {
        this.appointments = Objects.requireNonNull(appointments, "appointments");
        this.professionals = Objects.requireNonNull(professionals, "professionals");
    }

    @Override public String getName() { return "check_availability"; }
    @Override public String getDescription() { return "Verificar disponibilidad de un profesional"; }
    @Override public Map<String, String> getParameters() {
        return Map.of("professionalId", "Id del profesional", "start", "Inicio yyyy-MM-ddTHH:mm",
            "end", "Fin yyyy-MM-ddTHH:mm");
    }
    @Override public boolean requiresConfirmation() { return false; }

    @Override
    public String execute(Map<String, String> parameters)
        throws ValidationException, BusinessRuleException, DataAccessException {
        int id = ToolArguments.id(parameters, "professionalId");
        var start = ToolArguments.dateTime(parameters, "start");
        var end = ToolArguments.dateTime(parameters, "end");
        if (!end.isAfter(start)) throw new ValidationException("El fin debe ser posterior al inicio.");
        boolean available = appointments.checkAvailability(professionals.findById(id), start, end);
        return available ? "El profesional está disponible en ese horario."
            : "El profesional no está disponible en ese horario.";
    }
}
