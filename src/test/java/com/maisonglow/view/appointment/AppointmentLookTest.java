package com.maisonglow.view.appointment;

import com.maisonglow.model.AppointmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AppointmentLook tests")
class AppointmentLookTest {

    @Test
    @DisplayName("every state of an appointment has a look, so none can be left without a chip")
    void coversEveryState() {
        for (AppointmentStatus status : AppointmentStatus.values()) {
            AppointmentLook look = AppointmentLook.of(status);

            assertThat(look.label()).isNotBlank();
            assertThat(look.chipStyle()).startsWith("chip-");
        }
    }

    @Test
    @DisplayName("pending is gold, confirmed is green and a no-show warns")
    void colours() {
        assertThat(AppointmentLook.of(AppointmentStatus.PENDING).chipStyle()).isEqualTo("chip-gold");
        assertThat(AppointmentLook.of(AppointmentStatus.CONFIRMED).chipStyle()).isEqualTo("chip-success");
        assertThat(AppointmentLook.of(AppointmentStatus.NO_SHOW).chipStyle()).isEqualTo("chip-warning");
        assertThat(AppointmentLook.of(AppointmentStatus.NO_SHOW).label()).isEqualTo("Inasistencia");
    }
}
