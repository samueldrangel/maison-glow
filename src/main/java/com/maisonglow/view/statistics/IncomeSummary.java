package com.maisonglow.view.statistics;

import com.maisonglow.model.Appointment;
import com.maisonglow.model.AppointmentStatus;
import com.maisonglow.model.Sale;
import com.maisonglow.service.AppointmentService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The income of a range of days, worked out only from what the app already stores: the sales of products, the
 * price of the treatments of the appointments that were completed, the deposits paid for appointments still to be
 * attended and the price of the appointments that were lost. These are income figures, not profit: the app does not
 * know what its products or its work cost, nor what the business spends.
 *
 * <p>The price of an appointment is the current price of its treatment, because the appointment does not keep the
 * price it had when it was booked.
 *
 * @param salesIncome     the total of the sales of products
 * @param treatmentIncome the price of the treatments of the completed appointments
 * @param pendingDeposits the deposits of the appointments that still hold their schedule (pending or confirmed)
 * @param lostIncome      the price of the appointments that were missed or cancelled
 * @param lostAppointments how many appointments were missed or cancelled
 * @param byProfessional  income of the completed appointments by professional, highest first
 * @param byTreatment     income of the completed appointments by treatment, highest first
 */
public record IncomeSummary(BigDecimal salesIncome, BigDecimal treatmentIncome, BigDecimal pendingDeposits,
                            BigDecimal lostIncome, int lostAppointments, Map<String, BigDecimal> byProfessional,
                            Map<String, BigDecimal> byTreatment) {

    /**
     * @param sales        the sales of the range
     * @param appointments the appointments of the range, in any state
     * @return the income of that range
     */
    public static IncomeSummary of(List<Sale> sales, List<Appointment> appointments) {
        BigDecimal salesIncome = sales.stream().map(Sale::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal treatmentIncome = BigDecimal.ZERO;
        BigDecimal deposits = BigDecimal.ZERO;
        BigDecimal lost = BigDecimal.ZERO;
        int lostCount = 0;
        Map<String, BigDecimal> professionals = new HashMap<>();
        Map<String, BigDecimal> treatments = new HashMap<>();
        for (Appointment appointment : appointments) {
            BigDecimal price = appointment.getTreatment().getPrice();
            AppointmentStatus status = appointment.getStatus();
            if (status == AppointmentStatus.COMPLETED) {
                treatmentIncome = treatmentIncome.add(price);
                professionals.merge(appointment.getProfessional().getName(), price, BigDecimal::add);
                treatments.merge(appointment.getTreatment().getName(), price, BigDecimal::add);
            } else if (AppointmentService.blocksSchedule(status)) {
                deposits = deposits.add(appointment.getDeposit());
            } else {
                lost = lost.add(price);
                lostCount++;
            }
        }
        return new IncomeSummary(salesIncome, treatmentIncome, deposits, lost, lostCount, ranked(professionals), ranked(treatments));
    }

    /** @return everything the range brought in: products sold plus treatments given */
    public BigDecimal totalIncome() {
        return salesIncome.add(treatmentIncome);
    }

    private static Map<String, BigDecimal> ranked(Map<String, BigDecimal> values) {
        Map<String, BigDecimal> ranking = new LinkedHashMap<>();
        values.entrySet().stream()
            .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
            .forEach(entry -> ranking.put(entry.getKey(), entry.getValue()));
        return ranking;
    }
}
