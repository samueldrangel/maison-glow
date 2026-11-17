package com.maisonglow.dao;

import com.maisonglow.service.TransactionRunner;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;

/** Shares the connection across all DAO calls made by a business operation. */
public final class SqliteTransactions implements TransactionRunner {
    private final SqliteDatabase database;
    public SqliteTransactions(SqliteDatabase database) { this.database = database; }
    @Override public <T> T execute(Action<T> action) throws ValidationException, BusinessRuleException, DataAccessException {
        try {
            return database.transaction(c -> {
                try { return action.run(); }
                catch (ValidationException | BusinessRuleException e) { throw new DataAccessException("Operación rechazada.",e); }
            });
        } catch (DataAccessException e) {
            if (e.getCause() instanceof ValidationException validation) throw validation;
            if (e.getCause() instanceof BusinessRuleException rule) throw rule;
            throw e;
        }
    }
}
