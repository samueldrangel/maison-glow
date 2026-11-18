package com.maisonglow.view.access;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;

import java.util.Optional;

/**
 * What the access screens need from the account service (contract S-R01/S-R02): sign up, sign in, sign out and change
 * the password. The screens only depend on this interface; until the real service exists, {@link InMemoryAccessGateway}
 * answers in its place and nothing is written to disk.
 *
 * <p>Errors come as {@link ValidationException} (a field is missing or malformed) or {@link BusinessRuleException}
 * (a rule is broken: wrong credentials, username in use). Their messages are in Spanish and name the field they are
 * about, so the screens can show them under it.
 */
public interface AccessGateway {

    /**
     * Opens a session.
     *
     * @param username the identifier the person signs in with
     * @param password the password, in clear text, only for this call
     * @return the open session
     * @throws ValidationException   if the username or the password is empty
     * @throws BusinessRuleException if the credentials do not match or the account is off
     * @throws DataAccessException   if the accounts cannot be read
     */
    AccessSession login(String username, String password) throws ValidationException, BusinessRuleException, DataAccessException;

    /**
     * Creates a customer account and opens its session. The role is never chosen by the person who signs up.
     *
     * @param data what the person typed
     * @return the open session of the new account
     * @throws ValidationException   if a field is empty or malformed, or the confirmation does not match
     * @throws BusinessRuleException if the username or the document is already in use
     */
    AccessSession register(RegistrationData data) throws ValidationException, BusinessRuleException, DataAccessException;

    /**
     * Changes the password of the open session.
     *
     * @param current      the password in use
     * @param replacement  the new password
     * @param confirmation the new password again
     * @throws ValidationException   if a field is empty, too short or the confirmation does not match
     * @throws BusinessRuleException if there is no open session or the current password is wrong
     */
    void changePassword(String current, String replacement, String confirmation)
        throws ValidationException, BusinessRuleException, DataAccessException;

    /** Closes the session; calling it with no session open does nothing. */
    void logout();

    /** @return the open session, empty if nobody signed in */
    Optional<AccessSession> current();
}
