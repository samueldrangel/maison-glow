package com.maisonglow.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Treatment tests")
class TreatmentTest {

    private Treatment haircut() {
        return new Treatment(1, "Corte de cabello", "Corte y secado", 35000, 45);
    }

    @Test
    @DisplayName("a new treatment is active and exposes its data as Sellable")
    void newTreatmentIsActive() {
        Sellable treatment = haircut();

        assertThat(((Treatment) treatment).isActive()).isTrue();
        assertThat(treatment.getName()).isEqualTo("Corte de cabello");
        assertThat(treatment.getPrice()).isEqualTo(35000);
    }

    @Test
    @DisplayName("calculateEnd adds the duration to the start")
    void calculateEndAddsDuration() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 20, 9, 30);

        assertThat(haircut().calculateEnd(start)).isEqualTo(LocalDateTime.of(2026, 10, 20, 10, 15));
    }

    @Test
    @DisplayName("deactivate and activate toggle availability")
    void deactivateAndActivate() {
        Treatment treatment = haircut();

        treatment.deactivate();
        assertThat(treatment.isActive()).isFalse();

        treatment.activate();
        assertThat(treatment.isActive()).isTrue();
    }

    @Test
    @DisplayName("two treatments are equal when they share the id")
    void equalityById() {
        Treatment renamed = new Treatment(1, "Corte", "", 40000, 30);
        Treatment other = new Treatment(2, "Corte de cabello", "Corte y secado", 35000, 45);

        assertThat(haircut()).isEqualTo(renamed).hasSameHashCodeAs(renamed);
        assertThat(haircut()).isNotEqualTo(other);
    }
}
