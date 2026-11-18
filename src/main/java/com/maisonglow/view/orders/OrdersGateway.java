package com.maisonglow.view.orders;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;

import java.util.List;

/**
 * What the orders screen of the administrator needs from the order service (contract of S-R04). The screen depends
 * only on this interface; {@link OrderServiceGateway} connects it to the real service, and the tests use their own.
 * Paying and cancelling are administrator operations: the service checks the permission again, the screen only hides what cannot be done.
 *
 * <p>Errors come as {@link BusinessRuleException} (the order is not open, the reservation ended, there is no session)
 * or {@link DataAccessException}; their messages are in Spanish and meant to be shown.
 */
public interface OrdersGateway {

    /**
     * @return every order, newest first
     * @throws BusinessRuleException if the session may not see them
     * @throws DataAccessException   if they cannot be read
     */
    List<OrderSummary> list() throws BusinessRuleException, DataAccessException;

    /**
     * @param orderId the order to open
     * @return the order with its lines
     * @throws BusinessRuleException if it does not exist or the session may not see it
     * @throws DataAccessException   if it cannot be read
     */
    OrderDetail detail(int orderId) throws BusinessRuleException, DataAccessException;

    /**
     * Confirms the payment: the sale is generated and the reservation is consumed in one step, so the stock is
     * discounted once.
     *
     * @param orderId the order paid
     * @param method  how it was paid
     * @return the number of the sale generated
     * @throws BusinessRuleException if the order is not open, the reservation ended or the session is not an administrator
     * @throws DataAccessException   if it cannot be stored
     */
    int confirmPayment(int orderId, PaymentMethod method) throws BusinessRuleException, DataAccessException;

    /**
     * Cancels an open order and releases its reservation, once.
     *
     * @param orderId the order to cancel
     * @throws BusinessRuleException if the order is not open or the session is not an administrator
     * @throws DataAccessException   if it cannot be stored
     */
    void cancel(int orderId) throws BusinessRuleException, DataAccessException;

    /**
     * @param orderId a paid order
     * @return the text of the receipt of the sale it generated
     * @throws BusinessRuleException if the order is not paid
     * @throws DataAccessException   if it cannot be read
     */
    String receipt(int orderId) throws BusinessRuleException, DataAccessException;
}
