package com.maisonglow.view.orders;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Commerce.Order;
import com.maisonglow.model.Commerce.OrderStatus;
import com.maisonglow.service.CustomerService;
import com.maisonglow.service.OrderService;
import com.maisonglow.service.SettingsService;
import com.maisonglow.util.InvoiceFormatter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The order service (S-R04) seen by the orders screen of the administrator. It turns the {@link Order} snapshots of the
 * service into the rows and details the screen shows, adds the name of the customer, and formats the receipt with the
 * same formatter as the sales screen. It decides nothing: who may see, pay or cancel an order is checked by the service
 * on every call, and the stock reserved, sold or released is the service's job too.
 *
 * <p>The service does not say how an order was paid, so the detail of a paid order has no payment method.
 */
public final class OrderServiceGateway implements OrdersGateway {

    private final OrderService orders;
    private final CustomerService customers;
    private final SettingsService settings;

    /**
     * Creates the adapter.
     *
     * @param orders    the order service that holds the rules
     * @param customers gives the name of the customer of each order
     * @param settings  gives the name of the business for the receipt
     */
    public OrderServiceGateway(OrderService orders, CustomerService customers, SettingsService settings) {
        this.orders = Objects.requireNonNull(orders, "orders");
        this.customers = Objects.requireNonNull(customers, "customers");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    @Override
    public List<OrderSummary> list() throws BusinessRuleException, DataAccessException {
        try {
            List<OrderSummary> rows = new java.util.ArrayList<>();
            for (Order order : orders.all()) {
                rows.add(summary(order));
            }
            return rows;
        } catch (ValidationException e) {
            throw new BusinessRuleException(e.getMessage(), e);
        }
    }

    @Override
    public OrderDetail detail(int orderId) throws BusinessRuleException, DataAccessException {
        try {
            return detailOf(orders.find(orderId));
        } catch (ValidationException e) {
            throw new BusinessRuleException(e.getMessage(), e);
        }
    }

    @Override
    public int confirmPayment(int orderId, PaymentMethod method) throws BusinessRuleException, DataAccessException {
        try {
            Order paid = orders.confirmPayment(orderId, method.name());
            return paid.saleId() == null ? 0 : paid.saleId();
        } catch (ValidationException e) {
            throw new BusinessRuleException(e.getMessage(), e);
        }
    }

    @Override
    public void cancel(int orderId) throws BusinessRuleException, DataAccessException {
        try {
            orders.cancel(orderId);
        } catch (ValidationException e) {
            throw new BusinessRuleException(e.getMessage(), e);
        }
    }

    @Override
    public String receipt(int orderId) throws BusinessRuleException, DataAccessException {
        try {
            return new InvoiceFormatter(settings.businessName()).format(orders.receipt(orderId));
        } catch (ValidationException e) {
            throw new BusinessRuleException(e.getMessage(), e);
        }
    }

    private OrderSummary summary(Order order) {
        LocalDateTime reservedUntil = order.status() == OrderStatus.PENDING_PAYMENT ? order.expiresAt() : null;
        return new OrderSummary(order.id(), nameOf(order.customerId()), order.createdAt(), order.total(), stateOf(order.status()),
            reservedUntil);
    }

    private OrderDetail detailOf(Order order) {
        List<OrderDetail.Line> lines = order.lines().stream()
            .map(line -> new OrderDetail.Line(line.name(), line.quantity(), line.unitPrice())).toList();
        return new OrderDetail(summary(order), lines, Optional.empty(), Optional.ofNullable(order.saleId()));
    }

    private String nameOf(int customerId) {
        try {
            return customers.findById(customerId).getName();
        } catch (BusinessRuleException | DataAccessException e) {
            return "Cliente n.º " + customerId;
        }
    }

    private static OrderState stateOf(OrderStatus status) {
        return switch (status) {
            case PENDING_PAYMENT -> OrderState.PENDING_PAYMENT;
            case PAID -> OrderState.PAID;
            case CANCELLED -> OrderState.CANCELLED;
            case EXPIRED -> OrderState.EXPIRED;
        };
    }
}
