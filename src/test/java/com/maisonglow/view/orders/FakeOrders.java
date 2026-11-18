package com.maisonglow.view.orders;

import com.maisonglow.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * An order service in memory for the tests of the orders screen: it applies the rules the real one (S-R04) will have —
 * only an open order can be paid or cancelled, paying generates one sale, cancelling releases the reservation — and
 * records what it was asked, so a test can check that nothing was called when it should not have been.
 */
public final class FakeOrders implements OrdersGateway {

    private final List<OrderDetail> orders = new ArrayList<>();
    /** What the screen asked, in order, for example {@code pay:3:CASH}. */
    public final List<String> calls = new ArrayList<>();

    /**
     * Adds an order.
     *
     * @param id            number of the order
     * @param customer      who placed it
     * @param state         where it is
     * @param createdAt     when it was placed
     * @param reservedUntil when the reservation ends, null if closed
     * @param lines         what was ordered
     * @return this, to chain
     */
    public FakeOrders order(int id, String customer, OrderState state, LocalDateTime createdAt, LocalDateTime reservedUntil,
                            OrderDetail.Line... lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderDetail.Line line : lines) {
            total = total.add(line.subtotal());
        }
        OrderSummary summary = new OrderSummary(id, customer, createdAt, total, state, reservedUntil);
        orders.add(new OrderDetail(summary, List.of(lines), state == OrderState.PAID ? Optional.of(PaymentMethod.CASH) : Optional.empty(),
            state == OrderState.PAID ? Optional.of(100 + id) : Optional.empty()));
        return this;
    }

    @Override
    public List<OrderSummary> list() {
        calls.add("list");
        return orders.stream().map(OrderDetail::summary).sorted(Comparator.comparing(OrderSummary::createdAt).reversed()).toList();
    }

    @Override
    public OrderDetail detail(int orderId) throws BusinessRuleException {
        calls.add("detail:" + orderId);
        return find(orderId);
    }

    @Override
    public int confirmPayment(int orderId, PaymentMethod method) throws BusinessRuleException {
        calls.add("pay:" + orderId + ":" + method);
        OrderDetail order = find(orderId);
        if (!order.summary().state().isOpen()) {
            throw new BusinessRuleException("El pedido " + orderId + " ya no está pendiente de pago.");
        }
        replace(order, OrderState.PAID, Optional.of(method), Optional.of(100 + orderId));
        return 100 + orderId;
    }

    @Override
    public void cancel(int orderId) throws BusinessRuleException {
        calls.add("cancel:" + orderId);
        OrderDetail order = find(orderId);
        if (!order.summary().state().isOpen()) {
            throw new BusinessRuleException("El pedido " + orderId + " ya no está pendiente de pago.");
        }
        replace(order, OrderState.CANCELLED, Optional.empty(), Optional.empty());
    }

    @Override
    public String receipt(int orderId) throws BusinessRuleException {
        calls.add("receipt:" + orderId);
        OrderDetail order = find(orderId);
        if (!order.summary().state().hasReceipt()) {
            throw new BusinessRuleException("El pedido " + orderId + " no está pagado.");
        }
        return "COMPROBANTE DE VENTA " + order.saleId().orElseThrow();
    }

    private OrderDetail find(int orderId) throws BusinessRuleException {
        return orders.stream().filter(order -> order.summary().id() == orderId).findFirst()
            .orElseThrow(() -> new BusinessRuleException("El pedido " + orderId + " no existe."));
    }

    private void replace(OrderDetail order, OrderState state, Optional<PaymentMethod> method, Optional<Integer> saleId) {
        OrderSummary old = order.summary();
        OrderSummary updated = new OrderSummary(old.id(), old.customerName(), old.createdAt(), old.total(), state, null);
        orders.set(orders.indexOf(order), new OrderDetail(updated, order.lines(), method, saleId));
    }
}
