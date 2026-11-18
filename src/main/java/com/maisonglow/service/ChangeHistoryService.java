package com.maisonglow.service;

import com.maisonglow.dao.*;
import com.maisonglow.exception.*;
import com.maisonglow.model.*;
import com.maisonglow.model.Commerce.*;
import java.nio.file.Path;
import java.time.*;
import java.util.List;

/** Private photo history; administrative access requires the customer's current opt-in. */
public final class ChangeHistoryService {
    private final AuthenticationService auth;
    private final CommerceDao store;
    private final AppointmentDao appointments;
    private final PhotoStore photos;
    private final TransactionRunner transactions;
    private final Clock clock;
    public ChangeHistoryService(AuthenticationService auth,CommerceDao store,AppointmentDao appointments,PhotoStore photos,TransactionRunner transactions,Clock clock) {
        this.auth=auth;this.store=store;this.appointments=appointments;this.photos=photos;this.transactions=transactions;this.clock=clock;
    }
    public void allowStaffAccess(boolean allowed)throws BusinessRuleException,DataAccessException {store.setStaffAccess(auth.currentCustomerId(),allowed);}
    public boolean staffAccessAllowed()throws BusinessRuleException,DataAccessException {return store.staffAccess(auth.currentCustomerId());}
    public List<Change> mine()throws BusinessRuleException,DataAccessException {return store.changes(auth.currentCustomerId());}
    public List<Change> forCustomer(int customerId)throws BusinessRuleException,DataAccessException {requireAccess(customerId);return store.changes(customerId);}
    public Change addOwn(ChangeKind kind,String notes,LocalDate date,Path source,Path result)throws ValidationException,BusinessRuleException,DataAccessException {
        int customer=auth.currentCustomerId();
        if(kind==null||kind==ChangeKind.SERVICE)throw new ValidationException("Selecciona cambio personal o simulación.");
        return save(customer,null,kind,notes,date,source,result);
    }
    public Change addService(int appointmentId,String notes,Path source,Path result)throws ValidationException,BusinessRuleException,DataAccessException {
        auth.requireAdmin();var appointment=appointments.findById(appointmentId).orElseThrow(() -> new BusinessRuleException("La cita no existe."));
        if(appointment.getStatus()!=AppointmentStatus.COMPLETED)throw new BusinessRuleException("Completa la cita antes de registrar el servicio.");
        return save(appointment.getCustomer().getId(),appointmentId,ChangeKind.SERVICE,notes,appointment.getStart().toLocalDate(),source,result);
    }
    private Change save(int customer,Integer appointment,ChangeKind kind,String notes,LocalDate date,Path source,Path result)throws ValidationException,BusinessRuleException,DataAccessException {
        var account=auth.requireAuthenticated();requireAccess(customer);
        var login=auth.requireSessionId();
        if(notes==null||notes.isBlank()||notes.length()>4000||date==null||date.isAfter(LocalDate.now(clock)))throw new ValidationException("Indica una descripción de hasta 4000 caracteres y una fecha no futura.");
        if(kind==ChangeKind.SIMULATION&&(source==null||result==null))throw new ValidationException("Conserva tanto el original como el resultado de la simulación.");
        String sourceKey=null,resultKey=null;
        try {
            if(source!=null)sourceKey=photos.importPhoto(source);if(result!=null)resultKey=photos.importPhoto(result);
            String original=sourceKey,generated=resultKey;
            return transactions.execute(() -> auth.inSession(login,() -> {requireAccess(customer);
                return store.insertChange(customer,account.id(),appointment,kind,notes.trim(),date,LocalDateTime.now(clock),original,generated);}));
        } catch(ValidationException|BusinessRuleException|DataAccessException|RuntimeException e) {
            try{photos.delete(sourceKey);photos.delete(resultKey);}catch(DataAccessException cleanup){e.addSuppressed(cleanup);}throw e;
        }
    }
    public Path photo(int changeId,boolean result)throws BusinessRuleException,DataAccessException {
        Change change=find(changeId);requireAccess(change.customerId());return photos.resolve(result?change.resultKey():change.sourceKey());
    }
    /** Retains the service record and author while removing its photos from the gallery and disk. */
    public void removePhotos(int changeId)throws ValidationException,BusinessRuleException,DataAccessException {
        transactions.execute(() -> {Change change=find(changeId);requireAccess(change.customerId());store.removePhotos(changeId);store.enqueuePhotoDelete(change.sourceKey());store.enqueuePhotoDelete(change.resultKey());return null;});
        photos.cleanPending(store);
    }
    private Change find(int id)throws BusinessRuleException,DataAccessException {auth.requireAuthenticated();return store.change(id).orElseThrow(() -> new BusinessRuleException("El registro no existe."));}
    private void requireAccess(int customer)throws BusinessRuleException,DataAccessException {
        var account=auth.requireAuthenticated();
        if(account.role()==UserRole.ADMIN) {if(!store.staffAccess(customer))throw new AuthorizationException("El cliente no ha autorizado consultar sus fotos e historial.");}
        else if(!Integer.valueOf(customer).equals(account.customerId()))throw new AuthorizationException("El historial no pertenece a tu cuenta.");
    }
}
