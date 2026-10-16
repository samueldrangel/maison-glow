package com.maisonglow.service;

import com.maisonglow.model.Customer;
import com.maisonglow.model.Sale;

/**
 * Sends the invoice of a sale to the customer by email (RF-27). The sale
 * logic depends on this contract, not on a mail provider, so a real
 * implementation can replace the simulated one in delivery 2.
 */
public interface EmailNotifier {

    /**
     * Sends the invoice of a sale.
     *
     * @param customer the customer who receives the invoice
     * @param sale     the sale to invoice
     */
    void sendInvoice(Customer customer, Sale sale);
}
