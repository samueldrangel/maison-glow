package com.maisonglow.service;

import com.maisonglow.dao.*;
import com.maisonglow.exception.*;
import com.maisonglow.model.*;
import com.maisonglow.model.Commerce.ProfessionalOption;
import com.maisonglow.service.rule.DepositRule;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Self-service API: the authenticated account supplies the customer identity. */
public final class CustomerPortalService {
    private final AuthenticationService auth;
    private final CustomerService customers;
    private final TreatmentService treatments;
    private final ProfessionalService professionals;
    private final AppointmentService appointments;
    private final AppointmentDao appointmentDao;
    private final SaleDao sales;
    private final StatisticsService statistics;
    private final SettingsService settings;
    private final TransactionRunner transactions;
    private final CommerceDao commerce;
    private final Clock clock;
    public CustomerPortalService(AuthenticationService auth,CustomerService customers,TreatmentService treatments,
        ProfessionalService professionals,AppointmentService appointments,AppointmentDao appointmentDao,SaleDao sales,
        StatisticsService statistics,SettingsService settings,TransactionRunner transactions,CommerceDao commerce,Clock clock) {
        this.auth=auth;this.customers=customers;this.treatments=treatments;this.professionals=professionals;this.appointments=appointments;
        this.appointmentDao=appointmentDao;this.sales=sales;this.statistics=statistics;this.settings=settings;this.transactions=transactions;this.commerce=commerce;this.clock=clock;
    }
    public Customer profile() throws BusinessRuleException,DataAccessException {return customers.findById(auth.currentCustomerId());}
    public Customer updateContact(String name,String phone,String email)throws ValidationException,BusinessRuleException,DataAccessException {
        return transactions.execute(() -> {Customer own=profile();return customers.update(own.getId(),new ContactData(own.getDocument(),name,phone,email));});
    }
    public List<Treatment> treatments()throws BusinessRuleException,DataAccessException {auth.requireAuthenticated();return treatments.listAll().stream().filter(Treatment::isActive).toList();}
    public List<ProfessionalOption> professionals(int treatmentId)throws BusinessRuleException,DataAccessException {
        auth.requireAuthenticated();Treatment treatment=treatments.findById(treatmentId);
        if(!treatment.isActive())throw new BusinessRuleException("El tratamiento no está disponible.");
        return professionals.listAll().stream().filter(p -> p.isActive()&&ProfessionalService.offers(p,treatment)).map(p -> new ProfessionalOption(p.getId(),p.getName(),p.getSpecialty())).toList();
    }
    public boolean available(int professionalId,int treatmentId,LocalDateTime start)throws ValidationException,BusinessRuleException,DataAccessException {
        auth.requireAuthenticated();if(start==null)throw new ValidationException("Selecciona un horario.");
        var professional=professionals.findById(professionalId);var treatment=treatments.findById(treatmentId);
        return treatment.isActive()&&ProfessionalService.offers(professional,treatment)
            &&!start.isBefore(LocalDateTime.now(clock).plusHours(settings.minBookingNoticeHours()))
            &&appointments.checkAvailability(professional,start,AppointmentService.calculateEnd(treatment,start));
    }
    /** Records a pending reservation with ZERO paid deposit. Only the administrator records money received. */
    public Appointment book(int professionalId,int treatmentId,LocalDateTime start)throws ValidationException,BusinessRuleException,DataAccessException {
        return transactions.execute(() -> {
            Customer customer=profile();
            return appointments.book(customer,professionals.findById(professionalId),treatments.findById(treatmentId),start,BigDecimal.ZERO);
        });
    }
    public List<Appointment> myAppointments()throws BusinessRuleException,DataAccessException {
        int id=auth.currentCustomerId();return appointmentDao.findAll().stream().filter(a -> a.getCustomer().getId()==id).sorted(Comparator.comparing(Appointment::getStart).reversed()).toList();
    }
    public Appointment reschedule(int id,LocalDateTime start)throws ValidationException,BusinessRuleException,DataAccessException {
        return transactions.execute(() -> {requireEditable(id);return appointments.reschedule(id,start);});
    }
    public void cancelAppointment(int id)throws ValidationException,BusinessRuleException,DataAccessException {
        transactions.execute(() -> {requireEditable(id);appointments.cancel(id);return null;});
    }
    public BigDecimal requiredDeposit(int id)throws ValidationException,BusinessRuleException,DataAccessException {
        var appointment=owned(id);return new DepositRule(settings).requiredDeposit(appointment);
    }
    /** Cumulative amount makes repeating a double-click idempotent; customers cannot call this operation. */
    public Appointment recordDeposit(int id,BigDecimal cumulativeAmount,String method)throws ValidationException,BusinessRuleException,DataAccessException {
        auth.requireAdmin();
        if(cumulativeAmount==null||cumulativeAmount.signum()<=0||!Set.of("CASH","CARD","TRANSFER").contains(method==null?"":method))throw new ValidationException("Revisa el anticipo y el método de pago.");
        return transactions.execute(() -> {
            auth.requireAdmin();var appointment=find(id);
            if(!AppointmentService.blocksSchedule(appointment))throw new BusinessRuleException("La cita ya está cerrada.");
            BigDecimal difference=cumulativeAmount.subtract(appointment.getDeposit());
            if(difference.signum()<0||cumulativeAmount.compareTo(appointment.getTreatment().getPrice())>0)throw new BusinessRuleException("El anticipo no puede reducirse ni superar el precio del tratamiento.");
            if(difference.signum()==0)return appointment;
            commerce.recordDeposit(id,method,difference,LocalDateTime.now(clock));
            appointment.setDeposit(cumulativeAmount);appointmentDao.update(appointment);return appointment;
        });
    }
    public List<Sale> myPurchases()throws BusinessRuleException,DataAccessException {return sales.findByCustomer(auth.currentCustomerId());}
    public CustomerStatistics summary()throws BusinessRuleException,DataAccessException {return statistics.customerStatistics(auth.currentCustomerId());}
    private Appointment owned(int id)throws BusinessRuleException,DataAccessException {var a=find(id);if(a.getCustomer().getId()!=auth.currentCustomerId())throw new AuthorizationException("La cita no pertenece a tu cuenta.");return a;}
    private Appointment find(int id)throws BusinessRuleException,DataAccessException {return appointmentDao.findById(id).orElseThrow(() -> new BusinessRuleException("La cita no existe."));}
    private void requireEditable(int id)throws ValidationException,BusinessRuleException,DataAccessException {
        var a=owned(id);if(a.getStart().isBefore(LocalDateTime.now(clock).plusHours(settings.minBookingNoticeHours())))throw new BusinessRuleException("El plazo para modificar esta cita ya terminó.");
    }
}
