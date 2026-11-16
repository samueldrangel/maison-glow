package com.maisonglow.view.statistics;

import com.maisonglow.model.Appointment;
import com.maisonglow.model.AppointmentStatus;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Sale;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * The figures of the summary of the statistics for a range of days: what was sold, how many appointments there
 * were and how they ended, how often customers missed them, how many customers are new and on which days of the
 * week the appointments fall. It is worked out from the lists the screen already reads, with no new queries.
 *
 * @param revenue          the total of the sales of the range
 * @param sales            how many sales there were
 * @param appointments     how many appointments there were, in any state
 * @param byStatus         how many appointments there were in each state
 * @param noShowRate       the share of appointments that were missed among those that took place or were missed;
 *                         empty when none has been decided yet
 * @param newCustomers     how many customers registered in the range
 * @param byWeekday        how many appointments, cancelled ones apart, fall on each day of the week
 */
public record RangeSummary(BigDecimal revenue, int sales, int appointments, Map<AppointmentStatus, Integer> byStatus,
                           OptionalDouble noShowRate, int newCustomers, Map<DayOfWeek, Integer> byWeekday) {

    /**
     * @param salesOfTheRange        the sales whose day is in the range
     * @param appointmentsOfTheRange the appointments whose day is in the range
     * @param customers              every customer
     * @param from                   the first day of the range, included
     * @param to                     the last day of the range, included
     * @return the summary of that range
     */
    public static RangeSummary of(List<Sale> salesOfTheRange, List<Appointment> appointmentsOfTheRange,
                                  List<Customer> customers, LocalDate from, LocalDate to) {
        Map<AppointmentStatus, Integer> byStatus = new EnumMap<>(AppointmentStatus.class);
        Map<DayOfWeek, Integer> byWeekday = new EnumMap<>(DayOfWeek.class);
        for (AppointmentStatus status : AppointmentStatus.values()) {
            byStatus.put(status, 0);
        }
        for (DayOfWeek day : DayOfWeek.values()) {
            byWeekday.put(day, 0);
        }
        for (Appointment appointment : appointmentsOfTheRange) {
            byStatus.merge(appointment.getStatus(), 1, Integer::sum);
            if (appointment.getStatus() != AppointmentStatus.CANCELLED) {
                byWeekday.merge(appointment.getStart().getDayOfWeek(), 1, Integer::sum);
            }
        }
        int decided = byStatus.get(AppointmentStatus.COMPLETED) + byStatus.get(AppointmentStatus.NO_SHOW);
        OptionalDouble noShowRate = decided == 0 ? OptionalDouble.empty()
            : OptionalDouble.of((double) byStatus.get(AppointmentStatus.NO_SHOW) / decided);
        BigDecimal revenue = salesOfTheRange.stream().map(Sale::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        int newCustomers = (int) customers.stream()
            .filter(customer -> !customer.getRegisteredAt().isBefore(from) && !customer.getRegisteredAt().isAfter(to)).count();
        return new RangeSummary(revenue, salesOfTheRange.size(), appointmentsOfTheRange.size(), byStatus, noShowRate,
            newCustomers, byWeekday);
    }
}
