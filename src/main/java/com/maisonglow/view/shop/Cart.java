package com.maisonglow.view.shop;

import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Commerce.CatalogProduct;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/**
 * The cart of the customer who is using the application: the products and quantities they want to order. It lives in memory,
 * for this session only, and the window empties it when the session closes, so the next person never sees it. It keeps the price
 * and the availability that the shop showed when the product was added, only to show them and to notice a change; the service
 * decides the real price and stock when the order is placed, and never takes a price or a customer from here.
 *
 * <p>Confirming an order needs a request key that stays the same while the same cart is retried (a double click, a repeated
 * confirmation): {@link #requestKey()} gives it, and any change of the cart makes the next one new. Every method is meant to be
 * called from the JavaFX thread.
 */
public final class Cart {

    /** Most units of one product in an order, which is what the service accepts. */
    public static final int MAX_QUANTITY = 10_000;

    /**
     * One product in the cart.
     *
     * @param productId  the product
     * @param name       its name
     * @param unitPrice  the price the shop showed
     * @param available  the units available when it was added or last checked
     * @param quantity   how many the customer wants
     */
    public record Line(int productId, String name, BigDecimal unitPrice, int available, int quantity) {

        /** @return quantity times the unit price */
        public BigDecimal subtotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    private final Map<Integer, Line> lines = new LinkedHashMap<>();
    private String requestKey;
    private int revision;

    /**
     * Adds units of a product; adding one already in the cart adds to its quantity.
     *
     * @param product  a published product
     * @param quantity how many to add, at least one
     * @throws ValidationException if the quantity is not valid, the product is sold out or the cart would hold more than is available
     */
    public void add(CatalogProduct product, int quantity) throws ValidationException {
        Objects.requireNonNull(product, "product");
        if (quantity < 1) {
            throw new ValidationException("Elige al menos una unidad.");
        }
        if (product.availableStock() < 1) {
            throw new ValidationException(product.name() + " está agotado por ahora.");
        }
        int wanted = quantity + quantityOf(product.id());
        if (wanted > product.availableStock()) {
            throw new ValidationException("Solo hay " + product.availableStock() + (product.availableStock() == 1 ? " unidad" : " unidades")
                + " disponibles de " + product.name() + ".");
        }
        if (wanted > MAX_QUANTITY) {
            throw new ValidationException("No se pueden pedir más de " + MAX_QUANTITY + " unidades de un producto.");
        }
        lines.put(product.id(), new Line(product.id(), product.name(), product.price(), product.availableStock(), wanted));
        changed();
    }

    /**
     * Sets the quantity of a product already in the cart; zero or less takes it out.
     *
     * @param productId the product
     * @param quantity  the new quantity
     * @throws ValidationException if it is more than what was available
     */
    public void setQuantity(int productId, int quantity) throws ValidationException {
        Line line = lines.get(productId);
        if (line == null) {
            return;
        }
        if (quantity <= 0) {
            remove(productId);
            return;
        }
        if (quantity > line.available()) {
            throw new ValidationException("Solo hay " + line.available() + (line.available() == 1 ? " unidad" : " unidades")
                + " disponibles de " + line.name() + ".");
        }
        lines.put(productId, new Line(productId, line.name(), line.unitPrice(), line.available(), quantity));
        changed();
    }

    /**
     * Takes a product out of the cart.
     *
     * @param productId the product
     */
    public void remove(int productId) {
        if (lines.remove(productId) != null) {
            changed();
        }
    }

    /**
     * Replaces the lines with what the shop says now: new prices, new availability or fewer units. A line whose product is
     * missing from {@code current} (withdrawn from the shop) is dropped.
     *
     * @param current the published products as they are now
     * @return the changes the customer has to be told about, in words; empty if nothing changed
     */
    public List<String> reconcile(Map<Integer, CatalogProduct> current) {
        List<String> changes = new java.util.ArrayList<>();
        for (Line line : List.copyOf(lines.values())) {
            CatalogProduct now = current.get(line.productId());
            if (now == null || now.availableStock() < 1) {
                lines.remove(line.productId());
                changes.add(line.name() + " ya no está disponible y se quitó de tu carrito.");
                continue;
            }
            int quantity = Math.min(line.quantity(), now.availableStock());
            if (quantity < line.quantity()) {
                changes.add("De " + line.name() + " solo quedan " + now.availableStock() + ": ajustamos tu cantidad.");
            }
            if (now.price().compareTo(line.unitPrice()) != 0) {
                changes.add("El precio de " + line.name() + " cambió de " + com.maisonglow.util.CurrencyFormatter.format(line.unitPrice())
                    + " a " + com.maisonglow.util.CurrencyFormatter.format(now.price()) + ".");
            }
            lines.put(line.productId(), new Line(line.productId(), line.name(), now.price(), now.availableStock(), quantity));
        }
        if (!changes.isEmpty()) {
            changed();
        }
        return changes;
    }

    /** Empties the cart; the window calls it when the session closes. */
    public void clear() {
        if (!lines.isEmpty()) {
            lines.clear();
            changed();
        }
        requestKey = null;
    }

    /** @return the lines, by product name */
    public List<Line> lines() {
        return lines.values().stream().sorted(Comparator.comparing(Line::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    /** @return the units of a product in the cart, zero if none */
    public int quantityOf(int productId) {
        Line line = lines.get(productId);
        return line == null ? 0 : line.quantity();
    }

    /** @return what the cart adds up to, with the prices the shop showed */
    public BigDecimal total() {
        return lines.values().stream().map(Line::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** @return the number of units in the cart */
    public int itemCount() {
        return lines.values().stream().mapToInt(Line::quantity).sum();
    }

    /** @return true if there is nothing in the cart */
    public boolean isEmpty() {
        return lines.isEmpty();
    }

    /** @return the quantity of each product, which is all the service is given to place an order */
    public Map<Integer, Integer> quantities() {
        Map<Integer, Integer> quantities = new TreeMap<>();
        lines.values().forEach(line -> quantities.put(line.productId(), line.quantity()));
        return quantities;
    }

    /**
     * @return the key of the order to place: the same while the cart does not change, so a retry or a double click gives back
     *         the same order instead of a second one; a change of the cart makes the next key new
     */
    public String requestKey() {
        if (requestKey == null) {
            requestKey = UUID.randomUUID().toString();
        }
        return requestKey;
    }

    /** @return a number that grows with every change, for the screens to know when to draw again */
    public int revision() {
        return revision;
    }

    private void changed() {
        requestKey = null;
        revision++;
    }
}
