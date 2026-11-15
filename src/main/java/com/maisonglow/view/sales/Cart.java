package com.maisonglow.view.sales;

import com.maisonglow.model.Customer;
import com.maisonglow.model.Sale;
import com.maisonglow.model.SaleLine;
import com.maisonglow.model.Sellable;
import com.maisonglow.service.SaleService;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * The lines of the sale being prepared. Adding something that is already there raises its
 * quantity instead of repeating the line, and the total follows every change, so the screen
 * only has to show it. Stock is not checked here: {@link SaleService#register} is the one
 * that rejects a sale that asks for more than there is.
 */
public final class Cart {

    private final ObservableList<SaleLine> lines = FXCollections.observableArrayList();
    private final ObjectBinding<BigDecimal> total = Bindings.createObjectBinding(this::sum, lines);

    /** @return the lines, in the order they were added; the table of the screen shows this list */
    public ObservableList<SaleLine> lines() {
        return lines;
    }

    /** @return the total of every line, which changes as lines are added, changed or removed */
    public ObjectBinding<BigDecimal> total() {
        return total;
    }

    /**
     * Adds one unit of a product or treatment.
     *
     * @param item what is sold
     */
    public void add(Sellable item) {
        Objects.requireNonNull(item, "item");
        for (int index = 0; index < lines.size(); index++) {
            SaleLine line = lines.get(index);
            if (line.getItem().equals(item)) {
                change(index, line.getQuantity() + 1);
                return;
            }
        }
        lines.add(new SaleLine(item, 1, item.getPrice()));
    }

    /**
     * Adds one more unit to a line.
     *
     * @param line a line of this cart
     */
    public void increase(SaleLine line) {
        change(lines.indexOf(line), line.getQuantity() + 1);
    }

    /**
     * Takes one unit from a line; a line never goes below one unit, it is removed instead.
     *
     * @param line a line of this cart
     */
    public void decrease(SaleLine line) {
        if (line.getQuantity() > 1) {
            change(lines.indexOf(line), line.getQuantity() - 1);
        }
    }

    /**
     * Removes a line.
     *
     * @param line a line of this cart
     */
    public void remove(SaleLine line) {
        lines.remove(line);
    }

    /** Empties the cart. */
    public void clear() {
        lines.clear();
    }

    /**
     * Builds the sale to register, with copies of the lines so that the cart can be emptied afterwards.
     *
     * @param customer who buys
     * @return a sale that has not been registered yet
     */
    public Sale toSale(Customer customer) {
        Sale sale = new Sale(customer);
        sale.setLines(lines.stream().map(line -> new SaleLine(line.getItem(), line.getQuantity(), line.getUnitPrice())).toList());
        sale.setTotal(sum());
        return sale;
    }

    private void change(int index, int quantity) {
        SaleLine line = lines.get(index);
        line.setQuantity(quantity);
        lines.set(index, line);
    }

    private BigDecimal sum() {
        return lines.stream().map(SaleService::calculateSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
