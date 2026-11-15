package com.maisonglow.view.customer;

import com.maisonglow.model.Customer;
import com.maisonglow.util.TextSearch;

import java.time.LocalDate;

/**
 * What the customers screen is showing: the words typed in the search box and the chip chosen. The
 * search looks in the name, the document and the phone, ignoring case and accents. It is a value:
 * changing one thing gives a new filter.
 *
 * @param text  the search text; empty means no search
 * @param group which customers the chip lets through
 * @param today the day "new" is counted from
 */
public record CustomerFilter(String text, Group group, LocalDate today) {

    /** Days after registering during which a customer counts as new. */
    public static final int NEW_DAYS = 30;

    /** The chips of the screen. */
    public enum Group { ALL, WITH_NO_SHOWS, NEW }

    /**
     * @param today the current day
     * @return a filter that lets every customer through
     */
    public static CustomerFilter all(LocalDate today) {
        return new CustomerFilter("", Group.ALL, today);
    }

    /** @return the same filter with another search text */
    public CustomerFilter withText(String newText) {
        return new CustomerFilter(newText == null ? "" : newText, group, today);
    }

    /** @return the same filter for another chip */
    public CustomerFilter withGroup(Group newGroup) {
        return new CustomerFilter(text, newGroup, today);
    }

    /**
     * @param customer the customer to check
     * @return true if the customer passes the search and the chip
     */
    public boolean matches(Customer customer) {
        boolean byText = TextSearch.contains(customer.getName(), text)
            || TextSearch.contains(customer.getDocument(), text)
            || TextSearch.contains(customer.getPhone(), text);
        boolean byGroup = switch (group) {
            case ALL -> true;
            case WITH_NO_SHOWS -> customer.getNoShowCount() > 0;
            case NEW -> !customer.getRegisteredAt().isBefore(today.minusDays(NEW_DAYS));
        };
        return byText && byGroup;
    }
}
