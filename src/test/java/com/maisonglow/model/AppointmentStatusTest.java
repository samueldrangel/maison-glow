package com.maisonglow.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.maisonglow.model.AppointmentStatus.CANCELLED;
import static com.maisonglow.model.AppointmentStatus.COMPLETED;
import static com.maisonglow.model.AppointmentStatus.CONFIRMED;
import static com.maisonglow.model.AppointmentStatus.NO_SHOW;
import static com.maisonglow.model.AppointmentStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AppointmentStatus tests")
class AppointmentStatusTest {

    @Test
    @DisplayName("only pending and confirmed appointments block the schedule")
    void blocksSchedule() {
        assertThat(PENDING.blocksSchedule()).isTrue();
        assertThat(CONFIRMED.blocksSchedule()).isTrue();
        assertThat(COMPLETED.blocksSchedule()).isFalse();
        assertThat(CANCELLED.blocksSchedule()).isFalse();
        assertThat(NO_SHOW.blocksSchedule()).isFalse();
    }

    @Test
    @DisplayName("only a pending appointment can be confirmed")
    void onlyPendingCanBeConfirmed() {
        assertThat(PENDING.canChangeTo(CONFIRMED)).isTrue();
        assertThat(CONFIRMED.canChangeTo(CONFIRMED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"COMPLETED", "CANCELLED", "NO_SHOW"})
    @DisplayName("an active appointment can be closed in any way")
    void activeCanBeClosed(AppointmentStatus closing) {
        assertThat(PENDING.canChangeTo(closing)).isTrue();
        assertThat(CONFIRMED.canChangeTo(closing)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"COMPLETED", "CANCELLED", "NO_SHOW"})
    @DisplayName("a final status cannot change to any status")
    void finalStatusesNeverChange(AppointmentStatus finalStatus) {
        for (AppointmentStatus next : AppointmentStatus.values()) {
            assertThat(finalStatus.canChangeTo(next)).isFalse();
        }
    }

    @Test
    @DisplayName("no status can go back to pending")
    void nothingReturnsToPending() {
        for (AppointmentStatus status : AppointmentStatus.values()) {
            assertThat(status.canChangeTo(PENDING)).isFalse();
        }
    }
}
