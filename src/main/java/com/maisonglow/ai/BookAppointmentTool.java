package com.maisonglow.ai;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.service.AppointmentService;
import com.maisonglow.service.CustomerService;
import com.maisonglow.service.ProfessionalService;
import com.maisonglow.service.TreatmentService;

import java.util.Map;
import java.util.Objects;

/** Books through services only, after ToolRegistry has consumed explicit confirmation. */
public final class BookAppointmentTool implements AssistantTool {
    private final AppointmentService appointments;
    private final CustomerService customers;
    private final ProfessionalService professionals;
    private final TreatmentService treatments;

    /** Creates the booking tool using the same services as other business entry points. */
    public BookAppointmentTool(AppointmentService appointments, CustomerService customers,
                               ProfessionalService professionals, TreatmentService treatments) {
        this.appointments = Objects.requireNonNull(appointments, "appointments");
        this.customers = Objects.requireNonNull(customers, "customers");
        this.professionals = Objects.requireNonNull(professionals, "professionals");
        this.treatments = Objects.requireNonNull(treatments, "treatments");
    }

    @Override public String getName() { return "book_appointment"; }
    @Override public String getDescription() { return "Agendar una cita"; }
    @Override public Map<String, String> getParameters() {
        return Map.of("customerId", "Id del cliente registrado", "professionalId", "Id del profesional",
            "treatmentId", "Id del tratamiento", "start", "Inicio yyyy-MM-ddTHH:mm",
            "deposit", "Anticipo opcional, por defecto 0");
    }
    @Override public boolean requiresConfirmation() { return true; }

    @Override
    public String execute(Map<String, String> parameters)
        throws ValidationException, BusinessRuleException, DataAccessException {
        int customerId = ToolArguments.id(parameters, "customerId");
        int professionalId = ToolArguments.id(parameters, "professionalId");
        int treatmentId = ToolArguments.id(parameters, "treatmentId");
        var start = ToolArguments.dateTime(parameters, "start");
        var deposit = ToolArguments.deposit(parameters);
        var booked = appointments.book(customers.findById(customerId), professionals.findById(professionalId),
            treatments.findById(treatmentId), start, deposit);
        return "Cita agendada [" + booked.getId() + "]: " + booked;
    }
}
