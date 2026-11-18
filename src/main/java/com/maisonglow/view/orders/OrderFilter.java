package com.maisonglow.view.orders;

import com.maisonglow.util.TextSearch;

import java.util.Objects;

/**
 * What the administrator chose to see in the list of orders: a group of states and a text that is looked for in the
 * name of the customer and in the number of the order.
 *
 * @param group which orders to show
 * @param text  what the person typed; empty matches everything
 */
public record OrderFilter(Group group, String text) {

    /** The groups of states offered as chips. */
    public enum Group {
        /** Orders waiting for payment: the ones that need attention. */
        PENDING("Pendientes de pago"),
        /** Orders that became a sale. */
        PAID("Pagados"),
        /** Orders that no longer hold stock: cancelled or expired. */
        CLOSED("Cancelados y vencidos"),
        /** Every order. */
        ALL("Todos");

        private final String label;

        Group(String label) {
            this.label = label;
        }

        /** @return the text of the chip */
        public String label() {
            return label;
        }

        boolean contains(OrderState state) {
            return switch (this) {
                case PENDING -> state == OrderState.PENDING_PAYMENT;
                case PAID -> state == OrderState.PAID;
                case CLOSED -> state == OrderState.CANCELLED || state == OrderState.EXPIRED;
                case ALL -> true;
            };
        }
    }

    public OrderFilter {
        Objects.requireNonNull(group, "group");
        text = text == null ? "" : text.trim();
    }

    /** @return the filter the screen opens with: the orders waiting for payment */
    public static OrderFilter initial() {
        return new OrderFilter(Group.PENDING, "");
    }

    /**
     * @param newGroup the chip pressed
     * @return the same text over another group
     */
    public OrderFilter withGroup(Group newGroup) {
        return new OrderFilter(newGroup, text);
    }

    /**
     * @param newText what was typed
     * @return the same group with another text
     */
    public OrderFilter withText(String newText) {
        return new OrderFilter(group, newText);
    }

    /**
     * @param order a row of the list
     * @return true if the row is in the group and matches the text (ignoring case and accents)
     */
    public boolean matches(OrderSummary order) {
        return group.contains(order.state())
            && (text.isEmpty() || TextSearch.contains(order.customerName(), text) || String.valueOf(order.id()).contains(text));
    }
}
