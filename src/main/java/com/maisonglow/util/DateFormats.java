package com.maisonglow.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * The dates and times of the interface, written one way everywhere:
 * day/month/year and 24-hour time, in Spanish. Views call this instead of
 * building their own {@link DateTimeFormatter}.
 */
public final class DateFormats {

    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", COLOMBIA);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", COLOMBIA);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", COLOMBIA);
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", COLOMBIA);

    private DateFormats() {
    }

    /**
     * @param date the day to write
     * @return for example {@code 13/11/2026}
     */
    public static String date(LocalDate date) {
        return DATE.format(Objects.requireNonNull(date, "date"));
    }

    /**
     * @param time the time to write
     * @return for example {@code 09:30}
     */
    public static String time(LocalTime time) {
        return TIME.format(Objects.requireNonNull(time, "time"));
    }

    /**
     * @param dateTime the moment to write
     * @return for example {@code 13/11/2026 09:30}
     */
    public static String dateTime(LocalDateTime dateTime) {
        return DATE_TIME.format(Objects.requireNonNull(dateTime, "dateTime"));
    }

    /**
     * @param date the day to write in full, for headings
     * @return for example {@code viernes 13 de noviembre de 2026}
     */
    public static String longDate(LocalDate date) {
        return LONG_DATE.format(Objects.requireNonNull(date, "date"));
    }
}
