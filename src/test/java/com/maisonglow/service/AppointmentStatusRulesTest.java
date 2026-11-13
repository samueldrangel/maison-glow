package com.maisonglow.service;

import com.maisonglow.model.AppointmentStatus;

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
class AppointmentStatusRulesTest {

    @Test
    @DisplayName("only pending and confirmed appointments block the schedule")
    void blocksSchedule() {
        assertThat(AppointmentService.blocksSchedule(PENDING)).isTrue();
        assertThat(AppointmentService.blocksSchedule(CONFIRMED)).isTrue();
        assertThat(AppointmentService.blocksSchedule(COMPLETED)).isFalse();
        assertThat(AppointmentService.blocksSchedule(CANCELLED)).isFalse();
        assertThat(AppointmentService.blocksSchedule(NO_SHOW)).isFalse();
    }

    @Test
    @DisplayName("only a pending appointment can be confirmed")
    void onlyPendingCanBeConfirmed() {
        assertThat(AppointmentService.canChangeTo(PENDING, CONFIRMED)).isTrue();
        assertThat(AppointmentService.canChangeTo(CONFIRMED, CONFIRMED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"COMPLETED", "CANCELLED", "NO_SHOW"})
    @DisplayName("an active appointment can be closed in any way")
    void activeCanBeClosed(AppointmentStatus closing) {
        assertThat(AppointmentService.canChangeTo(PENDING, closing)).isTrue();
        assertThat(AppointmentService.canChangeTo(CONFIRMED, closing)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"COMPLETED", "CANCELLED", "NO_SHOW"})
    @DisplayName("a final status cannot change to any status")
    void finalStatusesNeverChange(AppointmentStatus finalStatus) {
        for (AppointmentStatus next : AppointmentStatus.values()) {
            assertThat(AppointmentService.canChangeTo(finalStatus, next)).isFalse();
        }
    }

    @Test
    @DisplayName("no status can go back to pending")
    void nothingReturnsToPending() {
        for (AppointmentStatus status : AppointmentStatus.values()) {
            assertThat(AppointmentService.canChangeTo(status, PENDING)).isFalse();
        }
    }
}
