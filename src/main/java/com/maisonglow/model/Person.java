package com.maisonglow.model;

import java.util.Objects;

/**
 * Holds the contact data shared by every person the business deals with.
 * Two people are considered the same when they have the same identity
 * document, which never changes once registered.
 */
public abstract class Person {

    private int id;
    private final String document;
    private String name;
    private String phone;
    private String email;

    /**
     * Creates a person with its contact data.
     *
     * @param id       the identifier; 0 if the person is not stored yet
     * @param document the identity document, unique per person
     * @param name     the full name
     * @param phone    the contact phone number
     * @param email    the contact email address
     */
    protected Person(int id, String document, String name, String phone, String email) {
        this.id = id;
        this.document = document;
        this.name = name;
        this.phone = phone;
        this.email = email;
    }

    /**
     * Describes the role this person plays in the business.
     *
     * @return the role name shown to the user
     */
    public abstract String getRoleDescription();

    /**
     * Returns the identifier assigned by the data store.
     *
     * @return the identifier; 0 if the person is not stored yet
     */
    public int getId() {
        return id;
    }

    /**
     * Assigns the identifier given by the data store.
     *
     * @param id the new identifier
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Returns the identity document.
     *
     * @return the document
     */
    public String getDocument() {
        return document;
    }

    /**
     * Returns the full name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Changes the full name.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the contact phone number.
     *
     * @return the phone number
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Changes the contact phone number.
     *
     * @param phone the new phone number
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Returns the contact email address.
     *
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Changes the contact email address.
     *
     * @param email the new email address
     */
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return Objects.equals(document, ((Person) other).document);
    }

    @Override
    public int hashCode() {
        return Objects.hash(document);
    }

    @Override
    public String toString() {
        return getRoleDescription() + ": " + name + " (" + document + ")";
    }
}
