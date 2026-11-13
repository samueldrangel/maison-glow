package com.maisonglow.model;

import java.math.BigDecimal;
/** Data for treatment; business rules belong to services. */
public class Treatment implements Sellable {
    private int id;
    private String name;
    private String description;
    private BigDecimal price;
    private int durationMinutes;
    private boolean active = true;

    public Treatment(int id, String name, String description, BigDecimal price, int durationMinutes) {
        this.id = id; this.name = name; this.description = description;
        this.price = price; this.durationMinutes = durationMinutes;
    }
    public Treatment(int id, String name, String description, double price, int durationMinutes) {
        this(id, name, description, BigDecimal.valueOf(price), durationMinutes);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof Treatment value && id > 0 && id == value.id;
    }

    // Stable while persistence assigns an id: safe for HashSet/HashMap keys.
    @Override
    public int hashCode() { return Treatment.class.hashCode(); }
    public void setPrice(double price) { setPrice(BigDecimal.valueOf(price)); }
    @Override public String toString() { return name + " (" + durationMinutes + " min, $" + price + ")"; }

}
