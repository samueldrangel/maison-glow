package com.maisonglow.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@DisplayName("TextSearch tests")
class TextSearchTest {

    @Test
    @DisplayName("ignores capital letters and accents")
    void ignoresCaseAndAccents() {
        assertThat(TextSearch.contains("Champú Nutritivo", "champu")).isTrue();
        assertThat(TextSearch.contains("Esmalte Rosa Nude", "ESMALTÉ")).isTrue();
        assertThat(TextSearch.contains("Uñas", "unas")).isTrue();
    }

    @Test
    @DisplayName("finds a piece in the middle of the text")
    void findsAPiece() {
        assertThat(TextSearch.contains("Base Líquida Natural", "quida nat")).isTrue();
    }

    @Test
    @DisplayName("an empty search matches everything and spaces around it do not matter")
    void emptySearchMatchesAll() {
        assertThat(TextSearch.contains("Cualquiera", "")).isTrue();
        assertThat(TextSearch.contains("Cualquiera", "   ")).isTrue();
        assertThat(TextSearch.contains("Cualquiera", "  cual ")).isTrue();
    }

    @Test
    @DisplayName("does not match what is not there")
    void noMatch() {
        assertThat(TextSearch.contains("Shampoo", "laca")).isFalse();
    }

    @Test
    @DisplayName("rejects a missing text")
    void rejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> TextSearch.normalize(null));
    }
}
