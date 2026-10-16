package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Customer;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Stores customers in a text file with the columns of the {@code customer}
 * table: {@code id|document|name|phone|email|no_show_count|registered_at}.
 */
public class CustomerDaoText extends TextFileDao<Customer> implements CustomerDao {

    private static final int ID = 0;
    private static final int DOCUMENT = 1;
    private static final int NAME = 2;
    private static final int PHONE = 3;
    private static final int EMAIL = 4;
    private static final int NO_SHOW_COUNT = 5;
    private static final int REGISTERED_AT = 6;
    private static final int FIELD_COUNT = 7;

    /**
     * Creates the DAO over the given file.
     *
     * @param file the customers file, usually {@code data/customers.txt}
     */
    public CustomerDaoText(Path file) {
        super(file);
    }

    @Override
    public Optional<Customer> findByDocument(String document) throws DataAccessException {
        return findAll().stream()
            .filter(customer -> customer.getDocument().equals(document))
            .findFirst();
    }

    @Override
    protected String[] toFields(Customer customer) {
        String[] fields = new String[FIELD_COUNT];
        fields[ID] = String.valueOf(customer.getId());
        fields[DOCUMENT] = customer.getDocument();
        fields[NAME] = customer.getName();
        fields[PHONE] = customer.getPhone();
        fields[EMAIL] = customer.getEmail();
        fields[NO_SHOW_COUNT] = String.valueOf(customer.getNoShowCount());
        fields[REGISTERED_AT] = customer.getRegisteredAt().toString();
        return fields;
    }

    @Override
    protected Customer fromFields(String[] fields) throws DataAccessException {
        if (fields.length != FIELD_COUNT) {
            throw new DataAccessException("Se esperaban " + FIELD_COUNT + " campos y hay " + fields.length + ".");
        }
        return new Customer(Integer.parseInt(fields[ID]), fields[DOCUMENT], fields[NAME], fields[PHONE],
            fields[EMAIL], LocalDate.parse(fields[REGISTERED_AT]), Integer.parseInt(fields[NO_SHOW_COUNT]));
    }

    @Override
    protected int getId(Customer customer) {
        return customer.getId();
    }

    @Override
    protected void setId(Customer customer, int id) {
        customer.setId(id);
    }
}
