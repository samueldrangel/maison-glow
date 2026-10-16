package com.maisonglow.dao;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Product;
import com.maisonglow.model.Sale;
import com.maisonglow.model.Treatment;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Stores sales in a text file with the columns of the {@code sale} table,
 * {@code id|customer_id|appointment_id|sold_at|total}, and their lines in a
 * second file with the columns of {@code sale_line}. A sale is always saved,
 * loaded and removed together with its lines.
 *
 * <p>The total is written to match the schema and calculated again from the
 * lines when loading. There are no transactions between the two files: if
 * the lines of a new sale cannot be saved, the sale is removed again.</p>
 */
public class SaleDaoText extends TextFileDao<Sale> implements SaleDao {

    private static final int ID = 0;
    private static final int CUSTOMER_ID = 1;
    private static final int APPOINTMENT_ID = 2;
    private static final int SOLD_AT = 3;
    private static final int TOTAL = 4;
    private static final int FIELD_COUNT = 5;
    private static final int UNSAVED_ID = 0;
    private static final String NO_VALUE = "";
    private static final Comparator<Sale> BY_SOLD_AT = Comparator.comparing(Sale::getSoldAt);

    private final CrudDao<Customer> customers;
    private final CrudDao<Appointment> appointments;
    private final SaleLineDaoText lines;

    /**
     * Creates the DAO over the given files. Each related DAO rebuilds the
     * entities that sales and lines reference by id (ADR-0012).
     *
     * @param salesFile    the sales file, usually {@code data/sales.txt}
     * @param linesFile    the sale lines file, usually {@code data/sale_lines.txt}
     * @param customers    where to find the customer of each sale
     * @param appointments where to find the appointment a sale charges
     * @param products     where to find the products sold
     * @param treatments   where to find the treatments sold
     */
    public SaleDaoText(Path salesFile, Path linesFile, CrudDao<Customer> customers,
                       CrudDao<Appointment> appointments, CrudDao<Product> products,
                       CrudDao<Treatment> treatments) {
        super(salesFile);
        this.customers = customers;
        this.appointments = appointments;
        this.lines = new SaleLineDaoText(linesFile, products, treatments);
    }

    @Override
    public Sale create(Sale sale) throws DataAccessException {
        Sale created = super.create(sale);
        try {
            lines.addLinesOf(created.getId(), created.getLines());
        } catch (DataAccessException e) {
            undoCreate(created, e);
            throw e;
        }
        return created;
    }

    private void undoCreate(Sale sale, DataAccessException cause) {
        try {
            lines.deleteLinesOf(sale.getId());
            super.delete(sale.getId());
            sale.setId(UNSAVED_ID);
        } catch (DataAccessException undoError) {
            cause.addSuppressed(undoError);
        }
    }

    @Override
    public void update(Sale sale) throws DataAccessException {
        super.update(sale);
        lines.deleteLinesOf(sale.getId());
        lines.addLinesOf(sale.getId(), sale.getLines());
    }

    @Override
    public void delete(int id) throws DataAccessException {
        lines.deleteLinesOf(id);
        super.delete(id);
    }

    @Override
    public List<Sale> findByCustomer(int customerId) throws DataAccessException {
        return findSortedBySoldAt(sale -> sale.getCustomer().getId() == customerId);
    }

    @Override
    public List<Sale> findBetween(LocalDate from, LocalDate to) throws DataAccessException {
        return findSortedBySoldAt(sale -> isBetween(sale.getSoldAt().toLocalDate(), from, to));
    }

    private static boolean isBetween(LocalDate day, LocalDate from, LocalDate to) {
        return !day.isBefore(from) && !day.isAfter(to);
    }

    private List<Sale> findSortedBySoldAt(Predicate<Sale> condition) throws DataAccessException {
        return findAll().stream().filter(condition).sorted(BY_SOLD_AT).toList();
    }

    @Override
    protected String[] toFields(Sale sale) {
        String[] fields = new String[FIELD_COUNT];
        fields[ID] = String.valueOf(sale.getId());
        fields[CUSTOMER_ID] = String.valueOf(sale.getCustomer().getId());
        fields[APPOINTMENT_ID] = sale.getAppointment().map(appointment -> String.valueOf(appointment.getId()))
            .orElse(NO_VALUE);
        fields[SOLD_AT] = sale.getSoldAt().toString();
        fields[TOTAL] = String.valueOf(sale.calculateTotal());
        return fields;
    }

    @Override
    protected Sale fromFields(String[] fields) throws DataAccessException {
        if (fields.length != FIELD_COUNT) {
            throw new DataAccessException("Se esperaban " + FIELD_COUNT + " campos y hay " + fields.length + ".");
        }
        int id = Integer.parseInt(fields[ID]);
        Sale sale = Sale.restore(id,
            findRelated(customers, fields[CUSTOMER_ID], "cliente"),
            LocalDateTime.parse(fields[SOLD_AT]),
            lines.findLinesOf(id));
        restoreAppointment(sale, fields[APPOINTMENT_ID]);
        return sale;
    }

    private void restoreAppointment(Sale sale, String appointmentId) throws DataAccessException {
        if (appointmentId.isEmpty()) {
            return;
        }
        try {
            sale.linkAppointment(findRelated(appointments, appointmentId, "cita"));
        } catch (BusinessRuleException e) {
            throw new DataAccessException("La cita guardada de la venta no es válida: " + e.getMessage(), e);
        }
    }

    @Override
    protected int getId(Sale sale) {
        return sale.getId();
    }

    @Override
    protected void setId(Sale sale, int id) {
        sale.setId(id);
    }
}
