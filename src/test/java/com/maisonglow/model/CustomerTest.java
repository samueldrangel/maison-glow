package com.maisonglow.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Customer tests")
class CustomerTest {

    private Customer ana() {
        return new Customer(1, "1065123456", "Ana Pérez", "3001234567", "ana@correo.com");
    }

    @Test
    @DisplayName("a new customer is registered today with no missed appointments")
    void newCustomerStartsClean() {
        Customer customer = ana();

        assertThat(customer.getRegisteredAt()).isEqualTo(LocalDate.now());
        assertThat(customer.getNoShowCount()).isZero();
        assertThat(customer.getRoleDescription()).isEqualTo("Cliente");
    }

    @Test
    @DisplayName("the full constructor keeps the stored history")
    void fullConstructorKeepsHistory() {
        LocalDate registered = LocalDate.of(2026, 3, 15);
        Customer customer = new Customer(7, "1065123456", "Ana Pérez", "3001234567",
            "ana@correo.com", registered, 2);

        assertThat(customer.getId()).isEqualTo(7);
        assertThat(customer.getRegisteredAt()).isEqualTo(registered);
        assertThat(customer.getNoShowCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("registerNoShow increments the counter")
    void registerNoShowIncrements() {
        Customer customer = ana();

        customer.registerNoShow();
        customer.registerNoShow();

        assertThat(customer.getNoShowCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("customers are equal when they share the document")
    void equalityByDocument() {
        Customer sameDocument = new Customer(9, "1065123456", "Ana María Pérez", "3109876543", "otra@correo.com");
        Customer otherDocument = new Customer(1, "1065999999", "Ana Pérez", "3001234567", "ana@correo.com");

        assertThat(ana()).isEqualTo(sameDocument).hasSameHashCodeAs(sameDocument);
        assertThat(ana()).isNotEqualTo(otherDocument);
    }

    @Test
    @DisplayName("toString shows role, name and document")
    void toStringShowsRole() {
        assertThat(ana()).hasToString("Cliente: Ana Pérez (1065123456)");
    }
}
