package com.maisonglow.ai;

import com.maisonglow.dao.AppointmentDao;
import com.maisonglow.dao.CustomerDao;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Product;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.AppointmentService;
import com.maisonglow.service.CustomerService;
import com.maisonglow.service.ProductService;
import com.maisonglow.service.ProfessionalService;
import com.maisonglow.service.TreatmentService;
import com.maisonglow.service.rule.AppointmentRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SimulatedAssistantTest {
    private static final LocalDateTime START = LocalDateTime.of(2026, 12, 1, 9, 0);
    private static final String BOOK = "book_appointment customerId=1; professionalId=2; treatmentId=3; start=2026-12-01T09:00; deposit=10000.50";

    private final Customer customer = new Customer(1, "1001", "Ana Ruiz", "3001112222", "ana@example.com");
    private final Professional professional = new Professional(2, "2001", "Laura", "3001112223",
        "laura@example.com", "Cabello", LocalTime.of(8, 0), LocalTime.of(18, 0));
    private final Treatment treatment = new Treatment(3, "Corte", "Corte y secado", 35000, 45);

    private CustomerService customers;
    private ProductService products;
    private ProfessionalService professionals;
    private TreatmentService treatments;
    private AppointmentDao appointmentDao;
    private AppointmentService appointments;
    private AppointmentRule rule;
    private SmartAssistant assistant;

    @BeforeEach
    void setUp() {
        customers = mock(CustomerService.class);
        products = mock(ProductService.class);
        professionals = mock(ProfessionalService.class);
        treatments = mock(TreatmentService.class);
        appointmentDao = mock(AppointmentDao.class);
        rule = mock(AppointmentRule.class);
        appointments = new AppointmentService(appointmentDao, mock(CustomerDao.class), List.of(rule));
        ProfessionalService.addTreatment(professional, treatment);
        assistant = new SimulatedAssistant(new ToolRegistry(List.of(new FindCustomerTool(customers),
            new ListLowStockTool(products), new ListAppointmentsByDateTool(appointments),
            new CheckAvailabilityTool(appointments, professionals),
            new BookAppointmentTool(appointments, customers, professionals, treatments))));
    }

    private void bookingLookups() throws Exception {
        when(customers.findById(1)).thenReturn(customer);
        when(professionals.findById(2)).thenReturn(professional);
        when(treatments.findById(3)).thenReturn(treatment);
        when(appointmentDao.findByProfessional(2)).thenReturn(List.of());
        when(appointmentDao.create(any())).thenAnswer(invocation -> {
            Appointment appointment = invocation.getArgument(0);
            appointment.setId(7);
            return appointment;
        });
    }

    @Test
    void offlineQueriesUseServicesAndDescribeResults() throws Exception {
        when(customers.searchByName("Ana Ruiz")).thenReturn(List.of(customer));
        when(products.listLowStock()).thenReturn(List.of(new Product(4, "Shampoo", null, 10000, 2, 5)));
        when(appointmentDao.findByDate(LocalDate.of(2026, 12, 1))).thenReturn(List.of(
            AppointmentService.newAppointment(7, customer, professional, treatment, START)));

        assertThat(assistant.reply("buscar cliente Ana Ruiz").text()).contains("[1] Ana Ruiz");
        assertThat(assistant.reply("stock bajo").text()).contains("Shampoo", "2 unidades", "mínimo 5");
        assertThat(assistant.reply("citas 2026-12-01").text()).contains("[7]", "Ana Ruiz", "Corte");
        verify(customers).searchByName("Ana Ruiz");
        verify(products).listLowStock();
        verify(appointmentDao, never()).create(any());
    }

    @Test
    void emptyQueriesAndHelpExplainWhatTheUserCanDo() {
        assertThat(assistant.reply("stock bajo").text()).contains("No hay productos");
        assertThat(assistant.reply("buscar cliente Ana").text()).contains("No se encontraron");
        assertThat(assistant.reply("citas 2026-12-01").text()).contains("No hay citas");
        assertThat(assistant.reply("ayuda").text()).contains("simulado", "book_appointment", "customerId");
        assertThat(assistant.reply("hola").pendingConfirmation()).isEmpty();
    }

    @Test
    void bookingWaitsForConfirmationThenUsesBusinessServiceOnce() throws Exception {
        bookingLookups();
        AssistantReply proposal = assistant.reply(BOOK);
        PendingAction action = proposal.pendingConfirmation().orElseThrow();
        assertThat(proposal.text()).contains("Confirma", "customerId=1", "10000.50");
        verifyNoInteractions(customers, professionals, treatments, rule);
        verify(appointmentDao, never()).create(any());

        assertThat(assistant.confirm(action).text()).contains("Cita agendada [7]");
        verify(appointmentDao).create(argThat(appointment -> appointment.getCustomer() == customer
            && appointment.getProfessional() == professional && appointment.getTreatment() == treatment
            && appointment.getEnd().equals(START.plusMinutes(45))
            && appointment.getDeposit().compareTo(new BigDecimal("10000.50")) == 0));
        verify(rule).apply(any());
        assertThat(assistant.confirm(action).text()).contains("no está pendiente");
        verify(appointmentDao, times(1)).create(any());
    }

    @Test
    void cancellationNeverBooks() {
        PendingAction action = assistant.reply(BOOK).pendingConfirmation().orElseThrow();
        assertThat(assistant.reject(action).text()).contains("cancelada");
        assertThat(assistant.confirm(action).text()).contains("no está pendiente");
        verifyNoInteractions(appointmentDao, customers, professionals, treatments);
    }

    @Test
    void conflictCreatedAfterProposalIsRejectedAtConfirmation() throws Exception {
        bookingLookups();
        PendingAction action = assistant.reply(BOOK).pendingConfirmation().orElseThrow();
        when(appointmentDao.findByProfessional(2)).thenReturn(List.of(
            AppointmentService.newAppointment(8, customer, professional, treatment, START.plusMinutes(15))));

        assertThat(assistant.confirm(action).text()).contains("ya tiene una cita en ese horario");
        assertThat(assistant.confirm(action).text()).contains("no está pendiente");
        verify(appointmentDao, never()).create(any());
    }

    @Test
    void missingCustomerAndBookingRulesAreNotBypassed() throws Exception {
        bookingLookups();
        when(customers.findById(1)).thenThrow(new BusinessRuleException("No existe el cliente con id 1."));
        assertThat(assistant.confirm(assistant.reply(BOOK).pendingConfirmation().orElseThrow()).text())
            .contains("No existe el cliente");
        doReturn(customer).when(customers).findById(1);
        doThrow(new BusinessRuleException("No cumple la regla de anticipo.")).when(rule).apply(any());
        assertThat(assistant.confirm(assistant.reply(BOOK).pendingConfirmation().orElseThrow()).text())
            .contains("regla de anticipo");
        verify(appointmentDao, never()).create(any());
    }

    @Test
    void availabilityUsesCurrentProfessionalAndSchedule() throws Exception {
        when(professionals.findById(2)).thenReturn(professional);
        String command = "check_availability professionalId=2; start=2026-12-01T09:00; end=2026-12-01T09:45";
        assertThat(assistant.reply(command).text()).isEqualTo("El profesional está disponible en ese horario.");
        when(appointmentDao.findByProfessional(2)).thenReturn(List.of(
            AppointmentService.newAppointment(8, customer, professional, treatment, START)));
        assertThat(assistant.reply(command).text()).contains("no está disponible");
        assertThat(assistant.reply(command.replace("09:45", "08:45")).text()).contains("posterior");
        verify(appointmentDao, never()).create(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"citas 2026-02-30", "citas mañana", "find_customer", "find_customer name=",
        "find_customer name=Ana; name=Laura", "find_customer unknown=Ana", "list_low_stock;",
        "check_availability professionalId=0; start=2026-12-01T09:00; end=2026-12-01T10:00",
        "check_availability professionalId=abc; start=2026-12-01T09:00; end=2026-12-01T10:00",
        "check_availability professionalId=2; start=ayer; end=2026-12-01T10:00"})
    void invalidMessagesReturnReadableErrorsWithoutServiceCalls(String message) {
        AssistantReply result = assistant.reply(message);
        assertThat(result.text()).isNotBlank();
        assertThat(result.pendingConfirmation()).isEmpty();
        verifyNoInteractions(customers, products, professionals, treatments, appointmentDao);
    }

    @Test
    void blankAndUnknownRequestsReturnValidationMessages() {
        assertThat(assistant.reply(null).text()).contains("obligatorio");
        assertThat(assistant.reply("  ").text()).contains("obligatorio");
        assertThat(assistant.reply("delete_all").text()).contains("no registrada");
        assertThat(assistant.confirm(null).text()).contains("no está pendiente");
        assertThat(assistant.reject(null).text()).contains("no está pendiente");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "NaN", "Infinity", "abc"})
    void invalidDepositsNeverReachServices(String deposit) {
        PendingAction action = assistant.reply(BOOK.replace("10000.50", deposit)).pendingConfirmation().orElseThrow();
        assertThat(assistant.confirm(action).text()).contains("anticipo");
        verifyNoInteractions(customers, professionals, treatments, appointmentDao);
    }

    @Test
    void omittedDepositDefaultsToZero() throws Exception {
        bookingLookups();
        PendingAction action = assistant.reply(BOOK.replace("; deposit=10000.50", ""))
            .pendingConfirmation().orElseThrow();
        assertThat(assistant.confirm(action).text()).contains("agendada");
        verify(appointmentDao).create(argThat(appointment -> appointment.getDeposit().signum() == 0));
    }

    @Test
    void storageErrorsAreControlledForQueriesAndConfirmedBookings() throws Exception {
        when(products.listLowStock()).thenThrow(new DataAccessException("ruta interna sensible"));
        assertThat(assistant.reply("stock bajo").text()).contains("No se pudo").doesNotContain("ruta interna");
        bookingLookups();
        doThrow(new DataAccessException("ruta interna sensible")).when(appointmentDao).create(any());
        PendingAction action = assistant.reply(BOOK).pendingConfirmation().orElseThrow();
        assertThat(assistant.confirm(action).text()).contains("No se pudo").doesNotContain("ruta interna");
        assertThat(assistant.confirm(action).text()).contains("no está pendiente");
    }
}
