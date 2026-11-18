package com.maisonglow.model;

/** Public account data. Credentials never travel to controllers or session snapshots. */
public record UserAccount(int id, String username, UserRole role, Integer customerId, boolean active) { }
