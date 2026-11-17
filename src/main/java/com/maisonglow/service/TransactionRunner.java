package com.maisonglow.service;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;

/** Storage-independent transaction boundary for operations that update multiple DAOs. */
public interface TransactionRunner {
    @FunctionalInterface interface Action<T> {
        T run() throws ValidationException, BusinessRuleException, DataAccessException;
    }
    <T> T execute(Action<T> action) throws ValidationException, BusinessRuleException, DataAccessException;
    TransactionRunner DIRECT = new TransactionRunner() {
        public <T> T execute(Action<T> action) throws ValidationException, BusinessRuleException, DataAccessException { return action.run(); }
    };
}
