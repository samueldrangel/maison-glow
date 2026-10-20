package com.maisonglow.service;

import com.maisonglow.dao.AppointmentDao;
import com.maisonglow.dao.SaleDao;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.AppointmentStatus;
import com.maisonglow.model.Product;
import com.maisonglow.model.Sale;
import com.maisonglow.model.SaleLine;
import com.maisonglow.util.Validator;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes customer and administrator statistics from the sales and
 * appointments (RF-18, RF-28). Rankings are returned as maps ordered from the
 * highest to the lowest value; ties are ordered by name.
 */
public class StatisticsService {

    private final SaleDao saleDao;
    private final AppointmentDao appointmentDao;

    /**
     * Creates the service.
     *
     * @param saleDao        the sales storage
     * @param appointmentDao the appointments storage
     */
    public StatisticsService(SaleDao saleDao, AppointmentDao appointmentDao) {
        this.saleDao = saleDao;
        this.appointmentDao = appointmentDao;
    }

    /**
     * Summarizes the purchases and appointments of a customer (RF-18).
     *
     * @param customerId the customer id
     * @return the customer statistics; all zero if the customer has no activity
     * @throws DataAccessException if the data cannot be read
     */
    public CustomerStatistics customerStatistics(int customerId) throws DataAccessException {
        List<Sale> sales = saleDao.findByCustomer(customerId);
        List<Appointment> appointments = appointmentDao.findAll().stream()
            .filter(appointment -> appointment.getCustomer().getId() == customerId)
            .toList();
        double totalSpent = sales.stream().mapToDouble(Sale::calculateTotal).sum();
        return new CustomerStatistics(sales.size(), totalSpent, appointments.size(),
            count(appointments, AppointmentStatus.COMPLETED), count(appointments, AppointmentStatus.NO_SHOW));
    }

    /**
     * Ranks the best-selling products by units sold in a date range (RF-28).
     *
     * @param limit how many products to return, at least one
     * @param from  the first day of the range
     * @param to    the last day of the range, not before {@code from}
     * @return product name to units sold, highest first
     * @throws ValidationException if the limit or the range is invalid
     * @throws DataAccessException if the sales cannot be read
     */
    public Map<String, Integer> topProducts(int limit, LocalDate from, LocalDate to)
        throws ValidationException, DataAccessException {
        requireLimit(limit);
        Validator.requirePresent(from, "fecha inicial");
        Validator.requirePresent(to, "fecha final");
        if (from.isAfter(to)) {
            throw new ValidationException("La fecha inicial no puede ser posterior a la final.");
        }
        Map<String, Integer> units = new HashMap<>();
        for (Sale sale : saleDao.findBetween(from, to)) {
            for (SaleLine line : sale.getLines()) {
                if (line.getItem() instanceof Product product) {
                    units.merge(product.getName(), line.getQuantity(), Integer::sum);
                }
            }
        }
        return top(units, limit);
    }

    /**
     * Ranks the most requested treatments by appointments, ignoring the
     * cancelled ones (RF-28).
     *
     * @param limit how many treatments to return, at least one
     * @return treatment name to appointments, highest first
     * @throws ValidationException if the limit is invalid
     * @throws DataAccessException if the appointments cannot be read
     */
    public Map<String, Integer> topTreatments(int limit) throws ValidationException, DataAccessException {
        requireLimit(limit);
        Map<String, Integer> bookings = new HashMap<>();
        for (Appointment appointment : appointmentDao.findAll()) {
            if (appointment.getStatus() != AppointmentStatus.CANCELLED) {
                bookings.merge(appointment.getTreatment().getName(), 1, Integer::sum);
            }
        }
        return top(bookings, limit);
    }

    /**
     * Ranks the customers who buy most often (RF-28).
     *
     * @param limit how many customers to return, at least one
     * @return customer name to number of purchases, highest first
     * @throws ValidationException if the limit is invalid
     * @throws DataAccessException if the sales cannot be read
     */
    public Map<String, Integer> frequentCustomers(int limit) throws ValidationException, DataAccessException {
        requireLimit(limit);
        Map<String, Integer> purchases = new HashMap<>();
        for (Sale sale : saleDao.findAll()) {
            purchases.merge(sale.getCustomer().getName(), 1, Integer::sum);
        }
        return top(purchases, limit);
    }

    private static int count(List<Appointment> appointments, AppointmentStatus status) {
        return (int) appointments.stream().filter(appointment -> appointment.getStatus() == status).count();
    }

    private static void requireLimit(int limit) throws ValidationException {
        if (limit < 1) {
            throw new ValidationException("La cantidad a mostrar debe ser al menos 1.");
        }
    }

    private static Map<String, Integer> top(Map<String, Integer> values, int limit) {
        Map<String, Integer> ranking = new LinkedHashMap<>();
        values.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()))
            .limit(limit)
            .forEach(entry -> ranking.put(entry.getKey(), entry.getValue()));
        return ranking;
    }
}
