package com.maisonglow.service;

import com.maisonglow.dao.CustomerDao;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Customer;
import com.maisonglow.util.Validator;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Manages the customers of the business (RF-01, RF-02): registration with a
 * unique document, lookup, update and removal.
 */
public class CustomerService {

    private final CustomerDao customerDao;

    /**
     * Creates the service.
     *
     * @param customerDao the customers storage
     */
    public CustomerService(CustomerDao customerDao) {
        this.customerDao = customerDao;
    }

    /**
     * Registers a new customer.
     *
     * @param data the document, name, phone and email; the document must be unique among customers
     * @return the stored customer, with its id
     * @throws ValidationException   if a field is missing or malformed
     * @throws BusinessRuleException if another customer has the same document
     * @throws DataAccessException   if the customer cannot be stored
     */
    public Customer register(ContactData data)
        throws ValidationException, BusinessRuleException, DataAccessException {
        data.validate();
        String document = data.document().trim();
        if (customerDao.findByDocument(document).isPresent()) {
            throw new BusinessRuleException("Ya existe un cliente con el documento " + document + ".");
        }
        return customerDao.create(new Customer(0, document, data.name().trim(), data.phone().trim(),
            data.email().trim()));
    }

    /**
     * Returns the customer with the given id.
     *
     * @param id the customer id
     * @return the customer
     * @throws BusinessRuleException if no customer has that id
     * @throws DataAccessException   if the lookup fails
     */
    public Customer findById(int id) throws BusinessRuleException, DataAccessException {
        return customerDao.findById(id)
            .orElseThrow(() -> new BusinessRuleException("No existe el cliente con id " + id + "."));
    }

    /**
     * Looks up customers whose name contains the given text, ignoring case.
     *
     * @param text the text to search for
     * @return the matching customers
     * @throws ValidationException if the text is blank
     * @throws DataAccessException if the customers cannot be read
     */
    public List<Customer> searchByName(String text) throws ValidationException, DataAccessException {
        Validator.requireText(text, "nombre a buscar");
        String wanted = text.trim().toLowerCase(Locale.ROOT);
        return customerDao.findAll().stream()
            .filter(customer -> customer.getName().toLowerCase(Locale.ROOT).contains(wanted))
            .toList();
    }

    /**
     * Returns every customer.
     *
     * @return the customers
     * @throws DataAccessException if the customers cannot be read
     */
    public List<Customer> listAll() throws DataAccessException {
        return customerDao.findAll();
    }

    /**
     * Changes the contact data of an existing customer. The document cannot change.
     *
     * @param id   the customer id
     * @param data the new contact data; its document must be the customer's current one
     * @return the updated customer
     * @throws ValidationException   if a field is missing or malformed
     * @throws BusinessRuleException if the customer does not exist or the document is different
     * @throws DataAccessException   if the customer cannot be stored
     */
    public Customer update(int id, ContactData data)
        throws ValidationException, BusinessRuleException, DataAccessException {
        data.validate();
        Customer customer = findById(id);
        if (!customer.getDocument().equals(data.document().trim())) {
            throw new BusinessRuleException("El documento de un cliente no se puede cambiar.");
        }
        customer.setName(data.name().trim());
        customer.setPhone(data.phone().trim());
        customer.setEmail(data.email().trim());
        customerDao.update(customer);
        return customer;
    }

    /**
     * Records that a customer missed an appointment.
     *
     * @param id the customer id
     * @throws BusinessRuleException if the customer does not exist
     * @throws DataAccessException   if the customer cannot be stored
     */
    public void registerNoShow(int id) throws BusinessRuleException, DataAccessException {
        Customer customer = findById(id);
        customer.registerNoShow();
        customerDao.update(customer);
    }

    /**
     * Removes a customer.
     *
     * @param id the customer id
     * @throws BusinessRuleException if the customer does not exist
     * @throws DataAccessException   if the customer cannot be removed
     */
    public void delete(int id) throws BusinessRuleException, DataAccessException {
        findById(id);
        customerDao.delete(id);
    }

    /**
     * Looks up a customer by document.
     *
     * @param document the identity document
     * @return the customer, or an empty {@link Optional}
     * @throws DataAccessException if the lookup fails
     */
    public Optional<Customer> findByDocument(String document) throws DataAccessException {
        return customerDao.findByDocument(document);
    }
}
