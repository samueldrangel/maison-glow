package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.UserAccount;
import com.maisonglow.model.UserRole;
import java.util.Optional;

/** Account persistence; only the authentication service handles credentials. */
public interface AccountDao {
    /** Internal persistence snapshot. Never return it from a presentation-facing service. */
    record Credentials(UserAccount account, String passwordHash, int authVersion) {
        @Override public String toString() { return "Credentials[redacted]"; }
    }
    Optional<Credentials> findByUsername(String normalizedUsername) throws DataAccessException;
    Optional<Credentials> findById(int id) throws DataAccessException;
    UserAccount create(String username, String passwordHash, UserRole role, Integer customerId)
        throws DataAccessException;
    long countAdministrators(boolean activeOnly) throws DataAccessException;
    void changePassword(int id, String passwordHash) throws DataAccessException;
    void setActive(int id, boolean active) throws DataAccessException;
}
