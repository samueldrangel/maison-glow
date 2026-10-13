package com.maisonglow.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BeautyService tests")
class BeautyServiceTest {

    private BeautyService haircut() {
        return new BeautyService(1, "Corte de cabello", "Corte y secado", 35000, 45);
    }

    @Test
    @DisplayName("a new service is active and exposes its data as Sellable")
    void newServiceIsActive() {
        Sellable service = haircut();

        assertThat(((BeautyService) service).isActive()).isTrue();
        assertThat(service.getName()).isEqualTo("Corte de cabello");
        assertThat(service.getPrice()).isEqualTo(35000);
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
        BeautyService service = haircut();

        service.deactivate();
        assertThat(service.isActive()).isFalse();

        service.activate();
        assertThat(service.isActive()).isTrue();
    }

    @Test
    @DisplayName("two services are equal when they share the id")
    void equalityById() {
        BeautyService renamed = new BeautyService(1, "Corte", "", 40000, 30);
        BeautyService other = new BeautyService(2, "Corte de cabello", "Corte y secado", 35000, 45);

        assertThat(haircut()).isEqualTo(renamed).hasSameHashCodeAs(renamed);
        assertThat(haircut()).isNotEqualTo(other);
    }
}
