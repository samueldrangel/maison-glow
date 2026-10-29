package com.maisonglow.ui.console;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;

/**
 * One thing a menu option does. It may fail with the business exceptions,
 * which the {@link Menu} shows to the user.
 */
@FunctionalInterface
public interface MenuAction {

    /**
     * Runs the action.
     *
     * @throws ValidationException   if a value typed by the user is not valid
     * @throws BusinessRuleException if a business rule is broken
     * @throws DataAccessException   if the data cannot be read or stored
     */
    void run() throws ValidationException, BusinessRuleException, DataAccessException;
}
