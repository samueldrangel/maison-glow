package com.maisonglow.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a beauty service offered by the business, such as a haircut or
 * a manicure. Its duration determines how long it blocks a professional's
 * schedule, and it can be sold like any other {@link Sellable} item.
 */
public class BeautyService implements Sellable {

    private int id;
    private String name;
    private String description;
    private double price;
    private int durationMinutes;
    private boolean active;

    /**
     * Creates an active beauty service.
     *
     * @param id              the identifier; 0 if the service is not stored yet
     * @param name            the service name
     * @param description     a short description of the service
     * @param price           the service price
     * @param durationMinutes how long the service takes, in minutes
     */
    public BeautyService(int id, String name, String description, double price, int durationMinutes) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.durationMinutes = durationMinutes;
        this.active = true;
    }

    /**
     * Calculates when the service ends if it starts at the given moment.
     *
     * @param start the start of the service
     * @return the start plus the service duration
     */
    public LocalDateTime calculateEnd(LocalDateTime start) {
        return start.plusMinutes(durationMinutes);
    }

    /**
     * Makes the service available for new appointments and sales.
     */
    public void activate() {
        active = true;
    }

    /**
     * Withdraws the service from the catalog without deleting its history.
     */
    public void deactivate() {
        active = false;
    }

    @Override
    public int getId() {
        return id;
    }

    /**
     * Assigns the identifier given by the data store.
     *
     * @param id the new identifier
     */
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Changes the service name.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the description of the service.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Changes the description of the service.
     *
     * @param description the new description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public double getPrice() {
        return price;
    }

    /**
     * Changes the service price.
     *
     * @param price the new price
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * Returns how long the service takes.
     *
     * @return the duration in minutes
     */
    public int getDurationMinutes() {
        return durationMinutes;
    }

    /**
     * Changes how long the service takes.
     *
     * @param durationMinutes the new duration in minutes
     */
    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    /**
     * Indicates whether the service is currently offered.
     *
     * @return {@code true} if the service is active
     */
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BeautyService service)) {
            return false;
        }
        return id == service.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name + " (" + durationMinutes + " min, $" + price + ")";
    }
}
