package com.maisonglow.view.customer;

import com.maisonglow.model.Customer;
import com.maisonglow.service.CustomerStatistics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import java.util.OptionalDouble;

/**
 * What the statistics panel says about a customer, worked out from the customer and the figures of
 * {@link CustomerStatistics}: the initials for the medallion, the average ticket, how often the
 * customer shows up and whether the customer is new.
 *
 * @param customer   the customer
 * @param statistics the purchases and appointments of the customer
 * @param today      the day "new" and "since" are counted from
 */
public record CustomerProfile(Customer customer, CustomerStatistics statistics, LocalDate today) {

    /** @return up to two capital letters taken from the first two words of the name */
    public String initials() {
        return Arrays.stream(customer.getName().trim().split("\\s+"))
            .filter(word -> !word.isEmpty())
            .limit(2)
            .map(word -> word.substring(0, 1).toUpperCase(Locale.ROOT))
            .reduce("", String::concat);
    }

    /**
     * The appointments the customer missed, as the table of customers shows them. It is the counter of the
     * customer, the one the penalty rule uses, so the panel and the table never disagree.
     *
     * @return zero or more appointments
     */
    public int noShows() {
        return customer.getNoShowCount();
    }

    /** @return what the customer spends per purchase; zero when there are no purchases */
    public BigDecimal averageTicket() {
        if (statistics.purchases() == 0) {
            return BigDecimal.ZERO;
        }
        return statistics.totalSpent().divide(BigDecimal.valueOf(statistics.purchases()), 0, RoundingMode.HALF_UP);
    }

    /**
     * Appointments that took place against the ones that were missed. Appointments still to come
     * count for neither.
     *
     * @return the share of appointments the customer showed up to, from 0 to 1; empty when none has been decided yet
     */
    public OptionalDouble attendance() {
        int decided = statistics.completedAppointments() + noShows();
        return decided == 0 ? OptionalDouble.empty() : OptionalDouble.of((double) statistics.completedAppointments() / decided);
    }

    /** @return true if the customer registered in the last {@link CustomerFilter#NEW_DAYS} days */
    public boolean isNew() {
        return !customer.getRegisteredAt().isBefore(today.minusDays(CustomerFilter.NEW_DAYS));
    }
}
