package com.maisonglow.view.orders;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Order filter tests")
class OrderFilterTest {

    private static OrderSummary order(int id, String customer, OrderState state) {
        return new OrderSummary(id, customer, LocalDateTime.of(2026, 11, 17, 9, 0), BigDecimal.TEN, state, null);
    }

    @Test
    @DisplayName("each group takes only its states, and the first thing the administrator sees is what waits for payment")
    void groupsByState() {
        OrderSummary pending = order(1, "Ana", OrderState.PENDING_PAYMENT);
        OrderSummary paid = order(2, "Ana", OrderState.PAID);
        OrderSummary cancelled = order(3, "Ana", OrderState.CANCELLED);
        OrderSummary expired = order(4, "Ana", OrderState.EXPIRED);

        assertThat(OrderFilter.initial().group()).isEqualTo(OrderFilter.Group.PENDING);
        for (OrderSummary each : new OrderSummary[] {pending, paid, cancelled, expired}) {
            assertThat(new OrderFilter(OrderFilter.Group.ALL, "").matches(each)).isTrue();
        }
        assertThat(OrderFilter.initial().matches(pending)).isTrue();
        assertThat(OrderFilter.initial().matches(paid)).isFalse();
        assertThat(OrderFilter.initial().withGroup(OrderFilter.Group.PAID).matches(paid)).isTrue();
        OrderFilter closed = OrderFilter.initial().withGroup(OrderFilter.Group.CLOSED);
        assertThat(closed.matches(cancelled)).isTrue();
        assertThat(closed.matches(expired)).isTrue();
        assertThat(closed.matches(pending)).isFalse();
    }

    @Test
    @DisplayName("the text is looked for in the customer, ignoring case and accents, and in the number of the order")
    void searchesByCustomerOrNumber() {
        OrderFilter all = new OrderFilter(OrderFilter.Group.ALL, "");
        OrderSummary garcia = order(12, "Ana García", OrderState.PAID);

        assertThat(all.withText("garcia").matches(garcia)).isTrue();
        assertThat(all.withText("ANA").matches(garcia)).isTrue();
        assertThat(all.withText("12").matches(garcia)).isTrue();
        assertThat(all.withText("laura").matches(garcia)).isFalse();
        assertThat(all.withText(null).text()).isEmpty();
        assertThat(all.withText("  ").matches(garcia)).isTrue();
    }

    @Test
    @DisplayName("only a pending order is open; only a paid one has a receipt")
    void stateRules() {
        for (OrderState state : OrderState.values()) {
            assertThat(state.isOpen()).isEqualTo(state == OrderState.PENDING_PAYMENT);
            assertThat(state.hasReceipt()).isEqualTo(state == OrderState.PAID);
        }
    }
}
