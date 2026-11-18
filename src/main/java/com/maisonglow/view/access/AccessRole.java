package com.maisonglow.view.access;

/** The two roles of the application (table {@code app_user}); there is no third one. */
public enum AccessRole {
    /** Sees and manages the whole business. */
    ADMIN,
    /** Sees only what is theirs. */
    CUSTOMER
}
