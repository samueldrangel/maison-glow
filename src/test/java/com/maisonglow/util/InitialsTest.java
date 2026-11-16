package com.maisonglow.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Initials tests")
class InitialsTest {

    @Test
    @DisplayName("takes the first letters of the first two words, in capitals")
    void takesTwoLetters() {
        assertThat(Initials.of("ana maría García")).isEqualTo("AM");
        assertThat(Initials.of("  Laura  ")).isEqualTo("L");
    }

    @Test
    @DisplayName("gives an empty text for a blank or missing name")
    void emptyWhenBlank() {
        assertThat(Initials.of("   ")).isEmpty();
        assertThat(Initials.of(null)).isEmpty();
    }
}
