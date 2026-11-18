package com.maisonglow.view.access;

import java.util.Objects;

/**
 * Who is using the application: the account name, how to greet them and their role.
 *
 * @param username    the identifier of the account
 * @param displayName the name shown on screen
 * @param role        what the account may do
 */
public record AccessSession(String username, String displayName, AccessRole role) {

    public AccessSession {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(role, "role");
    }

    /** @return true if this is the administrator of the business */
    public boolean isAdministrator() {
        return role == AccessRole.ADMIN;
    }
}
