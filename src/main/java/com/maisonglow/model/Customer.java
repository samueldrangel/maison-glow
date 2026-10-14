package com.maisonglow.model;

import java.time.LocalDate;

/**
 * Represents a customer of the business, who books appointments and buys
 * products. Keeps track of missed appointments so that the booking rules
 * can penalize repeated no-shows.
 */
public class Customer extends Person {

    private final LocalDate registeredAt;
    private int noShowCount;

    /**
     * Creates a customer registered today, with no missed appointments.
     *
     * @param id       the identifier; 0 if the customer is not stored yet
     * @param document the identity document
     * @param name     the full name
     * @param phone    the contact phone number
     * @param email    the email address where invoices are sent
     */
    public Customer(int id, String document, String name, String phone, String email) {
        this(id, document, name, phone, email, LocalDate.now(), 0);
    }

    /**
     * Creates a customer with its full history, as loaded from the data store.
     *
     * @param id           the identifier
     * @param document     the identity document
     * @param name         the full name
     * @param phone        the contact phone number
     * @param email        the email address where invoices are sent
     * @param registeredAt the day the customer was registered
     * @param noShowCount  the number of appointments the customer missed
     */
    public Customer(int id, String document, String name, String phone, String email,
                    LocalDate registeredAt, int noShowCount) {
        super(id, document, name, phone, email);
        this.registeredAt = registeredAt;
        this.noShowCount = noShowCount;
    }

    /**
     * Records that the customer missed an appointment.
     */
    public void registerNoShow() {
        noShowCount++;
    }

    /**
     * Returns the number of appointments the customer missed.
     *
     * @return the no-show count
     */
    public int getNoShowCount() {
        return noShowCount;
    }

    /**
     * Returns the day the customer was registered.
     *
     * @return the registration date
     */
    public LocalDate getRegisteredAt() {
        return registeredAt;
    }

    @Override
    public String getRoleDescription() {
        return "Cliente";
    }
}
