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

    private Treatment haircut() {
        return new Treatment(1, "Corte de cabello", "Corte y secado", 35000, 45);
    }

    @Test
    @DisplayName("a new professional is active, has no treatments and a role")
    void newProfessionalDefaults() {
        Professional professional = laura();

        assertThat(professional.isActive()).isTrue();
        assertThat(professional.getTreatments()).isEmpty();
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
    @DisplayName("addTreatment avoids duplicates and offers reflects the list")
    void addTreatmentWithoutDuplicates() {
        Professional professional = laura();

        professional.addTreatment(haircut());
        professional.addTreatment(haircut());

        assertThat(professional.getTreatments()).hasSize(1);
        assertThat(professional.offers(haircut())).isTrue();
    }

    @Test
    @DisplayName("removeTreatment stops offering the treatment")
    void removeTreatment() {
        Professional professional = laura();
        professional.addTreatment(haircut());

        professional.removeTreatment(haircut());

        assertThat(professional.offers(haircut())).isFalse();
    }

    @Test
    @DisplayName("the treatment list cannot be modified from outside")
    void treatmentsAreReadOnly() {
        assertThatThrownBy(() -> laura().getTreatments().add(haircut()))
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
