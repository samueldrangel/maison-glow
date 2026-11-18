package com.maisonglow.view.access;

/**
 * What a person types to create an account. There is no role here: a public sign-up only creates customers.
 *
 * @param name         full name
 * @param document     identity document
 * @param phone        contact phone
 * @param email        contact email
 * @param username     identifier to sign in with
 * @param password     chosen password
 * @param confirmation the password again
 */
public record RegistrationData(String name, String document, String phone, String email, String username,
                               String password, String confirmation) {

    @Override
    public String toString() {
        return "RegistrationData[username=" + username + "]";
    }
}
