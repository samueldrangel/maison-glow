package com.maisonglow.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@DisplayName("DateFormats tests")
class DateFormatsTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 13);

    @Test
    @DisplayName("writes the day as day/month/year")
    void writesTheDate() {
        assertThat(DateFormats.date(DAY)).isEqualTo("13/11/2026");
        assertThat(DateFormats.date(LocalDate.of(2026, 1, 5))).isEqualTo("05/01/2026");
    }

    @Test
    @DisplayName("writes the time with 24 hours")
    void writesTheTime() {
        assertThat(DateFormats.time(LocalTime.of(9, 30))).isEqualTo("09:30");
        assertThat(DateFormats.time(LocalTime.of(16, 5))).isEqualTo("16:05");
    }

    @Test
    @DisplayName("writes date and time together")
    void writesDateAndTime() {
        assertThat(DateFormats.dateTime(LocalDateTime.of(2026, 11, 13, 9, 30))).isEqualTo("13/11/2026 09:30");
    }

    @Test
    @DisplayName("writes the long date in Spanish")
    void writesTheLongDate() {
        assertThat(DateFormats.longDate(DAY)).isEqualTo("viernes 13 de noviembre de 2026");
    }

    @Test
    @DisplayName("rejects a missing value")
    void rejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> DateFormats.date(null));
        assertThatNullPointerException().isThrownBy(() -> DateFormats.time(null));
        assertThatNullPointerException().isThrownBy(() -> DateFormats.dateTime(null));
        assertThatNullPointerException().isThrownBy(() -> DateFormats.longDate(null));
    }
}
