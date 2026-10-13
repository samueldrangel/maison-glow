package com.maisonglow.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Professional tests")
class ProfessionalTest {

    private static final LocalDateTime DAY = LocalDateTime.of(2026, 10, 20, 0, 0);

    private Professional laura() {
        return new Professional(1, "49777888", "Laura Gómez", "3157778888", "laura@correo.com",
            "Peluquería", LocalTime.of(8, 0), LocalTime.of(17, 0));
    }

    private BeautyService haircut() {
        return new BeautyService(1, "Corte de cabello", "Corte y secado", 35000, 45);
    }

    @Test
    @DisplayName("a new professional is active, has no services and a role")
    void newProfessionalDefaults() {
        Professional professional = laura();

        assertThat(professional.isActive()).isTrue();
        assertThat(professional.getServices()).isEmpty();
        assertThat(professional.getRoleDescription()).isEqualTo("Profesional");
    }

    @Test
    @DisplayName("worksDuring accepts a slot inside the working hours")
    void worksDuringInside() {
        assertThat(laura().worksDuring(DAY.withHour(10), DAY.withHour(11))).isTrue();
    }

    @Test
    @DisplayName("worksDuring accepts a slot exactly on the limits")
    void worksDuringOnTheEdges() {
        assertThat(laura().worksDuring(DAY.withHour(8), DAY.withHour(9))).isTrue();
        assertThat(laura().worksDuring(DAY.withHour(16), DAY.withHour(17))).isTrue();
    }

    @Test
    @DisplayName("worksDuring rejects slots outside or crossing the limits")
    void worksDuringOutside() {
        Professional professional = laura();

        assertThat(professional.worksDuring(DAY.withHour(7), DAY.withHour(8).plusMinutes(30))).isFalse();
        assertThat(professional.worksDuring(DAY.withHour(16).plusMinutes(30), DAY.withHour(17).plusMinutes(15)))
            .isFalse();
        assertThat(professional.worksDuring(DAY.withHour(18), DAY.withHour(19))).isFalse();
    }

    @Test
    @DisplayName("worksDuring rejects slots spanning two days or reversed")
    void worksDuringRejectsInvalidSlots() {
        Professional professional = laura();

        assertThat(professional.worksDuring(DAY.withHour(10), DAY.plusDays(1).withHour(10))).isFalse();
        assertThat(professional.worksDuring(DAY.withHour(11), DAY.withHour(10))).isFalse();
    }

    @Test
    @DisplayName("addService avoids duplicates and offers reflects the list")
    void addServiceWithoutDuplicates() {
        Professional professional = laura();

        professional.addService(haircut());
        professional.addService(haircut());

        assertThat(professional.getServices()).hasSize(1);
        assertThat(professional.offers(haircut())).isTrue();
    }

    @Test
    @DisplayName("removeService stops offering the service")
    void removeService() {
        Professional professional = laura();
        professional.addService(haircut());

        professional.removeService(haircut());

        assertThat(professional.offers(haircut())).isFalse();
    }

    @Test
    @DisplayName("the service list cannot be modified from outside")
    void servicesAreReadOnly() {
        assertThatThrownBy(() -> laura().getServices().add(haircut()))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("deactivate and activate toggle availability")
    void deactivateAndActivate() {
        Professional professional = laura();

        professional.deactivate();
        assertThat(professional.isActive()).isFalse();

        professional.activate();
        assertThat(professional.isActive()).isTrue();
    }

    @Test
    @DisplayName("a customer and a professional with the same document are not equal")
    void differentRolesAreNotEqual() {
        Customer customer = new Customer(1, "49777888", "Laura Gómez", "3157778888", "laura@correo.com");

        assertThat(laura()).isNotEqualTo(customer);
    }
}
