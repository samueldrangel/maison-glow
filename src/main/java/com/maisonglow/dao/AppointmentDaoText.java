package com.maisonglow.dao;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.AppointmentStatus;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Stores appointments in a text file with the columns of the
 * {@code appointment} table:
 * {@code id|customer_id|professional_id|treatment_id|start_at|end_at|status|deposit|reminder_at|created_at}.
 * Related entities are stored by id and rebuilt with the DAOs received in
 * the constructor.
 */
public class AppointmentDaoText extends TextFileDao<Appointment> implements AppointmentDao {

    private static final int ID = 0;
    private static final int CUSTOMER_ID = 1;
    private static final int PROFESSIONAL_ID = 2;
    private static final int TREATMENT_ID = 3;
    private static final int START_AT = 4;
    private static final int END_AT = 5;
    private static final int STATUS = 6;
    private static final int DEPOSIT = 7;
    private static final int REMINDER_AT = 8;
    private static final int CREATED_AT = 9;
    private static final int FIELD_COUNT = 10;
    private static final String NO_VALUE = "";
    private static final Comparator<Appointment> BY_START = Comparator.comparing(Appointment::getStart);

    private final CrudDao<Customer> customers;
    private final CrudDao<Professional> professionals;
    private final CrudDao<Treatment> treatments;

    /**
     * Creates the DAO over the given file.
     *
     * @param file          the appointments file, usually {@code data/appointments.txt}
     * @param customers     where to find the customer of each appointment
     * @param professionals where to find the professional of each appointment
     * @param treatments    where to find the treatment of each appointment
     */
    public AppointmentDaoText(Path file, CrudDao<Customer> customers, CrudDao<Professional> professionals,
                              CrudDao<Treatment> treatments) {
        super(file);
        this.customers = customers;
        this.professionals = professionals;
        this.treatments = treatments;
    }

    @Override
    public List<Appointment> findByProfessional(int professionalId) throws DataAccessException {
        return findSortedByStart(appointment -> appointment.getProfessional().getId() == professionalId);
    }

    @Override
    public List<Appointment> findByDate(LocalDate date) throws DataAccessException {
        return findSortedByStart(appointment -> appointment.getStart().toLocalDate().equals(date));
    }

    private List<Appointment> findSortedByStart(Predicate<Appointment> condition) throws DataAccessException {
        return findAll().stream().filter(condition).sorted(BY_START).toList();
    }

    @Override
    protected String[] toFields(Appointment appointment) {
        String[] fields = new String[FIELD_COUNT];
        fields[ID] = String.valueOf(appointment.getId());
        fields[CUSTOMER_ID] = String.valueOf(appointment.getCustomer().getId());
        fields[PROFESSIONAL_ID] = String.valueOf(appointment.getProfessional().getId());
        fields[TREATMENT_ID] = String.valueOf(appointment.getTreatment().getId());
        fields[START_AT] = appointment.getStart().toString();
        fields[END_AT] = appointment.getEnd().toString();
        fields[STATUS] = appointment.getStatus().name();
        fields[DEPOSIT] = String.valueOf(appointment.getDeposit());
        fields[REMINDER_AT] = formatOptional(appointment.getReminderAt());
        fields[CREATED_AT] = appointment.getCreatedAt().toString();
        return fields;
    }

    @Override
    protected Appointment fromFields(String[] fields) throws DataAccessException {
        if (fields.length != FIELD_COUNT) {
            throw new DataAccessException("Se esperaban " + FIELD_COUNT + " campos y hay " + fields.length + ".");
        }
        Appointment appointment = Appointment.restore(
            Integer.parseInt(fields[ID]),
            findRelated(customers, fields[CUSTOMER_ID], "cliente"),
            findRelated(professionals, fields[PROFESSIONAL_ID], "profesional"),
            findRelated(treatments, fields[TREATMENT_ID], "tratamiento"),
            LocalDateTime.parse(fields[START_AT]),
            LocalDateTime.parse(fields[END_AT]),
            AppointmentStatus.valueOf(fields[STATUS]),
            LocalDateTime.parse(fields[CREATED_AT]));
        restoreDeposit(appointment, Double.parseDouble(fields[DEPOSIT]));
        appointment.setReminderAt(parseOptional(fields[REMINDER_AT]));
        return appointment;
    }

    @Override
    protected int getId(Appointment appointment) {
        return appointment.getId();
    }

    @Override
    protected void setId(Appointment appointment, int id) {
        appointment.setId(id);
    }

    private static <E> E findRelated(CrudDao<E> dao, String idField, String entityName) throws DataAccessException {
        int id = Integer.parseInt(idField);
        return dao.findById(id).orElseThrow(() ->
            new DataAccessException("La cita referencia un " + entityName + " con id " + id + " que no existe."));
    }

    private static void restoreDeposit(Appointment appointment, double deposit) throws DataAccessException {
        try {
            appointment.setDeposit(deposit);
        } catch (BusinessRuleException e) {
            throw new DataAccessException("El anticipo guardado no es válido: " + deposit, e);
        }
    }

    private static String formatOptional(LocalDateTime value) {
        return value == null ? NO_VALUE : value.toString();
    }

    private static LocalDateTime parseOptional(String field) {
        return field.isEmpty() ? null : LocalDateTime.parse(field);
    }
}
