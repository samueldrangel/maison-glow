package com.maisonglow.dao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TextLineCodec tests")
class TextLineCodecTest {

    @Test
    @DisplayName("encode joins plain fields with the separator")
    void encodeJoinsFields() {
        assertThat(TextLineCodec.encode("1", "Corte", "35000")).isEqualTo("1|Corte|35000");
    }

    @Test
    @DisplayName("encode escapes the separator, the backslash and line breaks")
    void encodeEscapesSpecialCharacters() {
        assertThat(TextLineCodec.encode("a|b", "c\\d", "e\nf\r"))
            .isEqualTo("a\\|b|c\\\\d|e\\nf\\r");
    }

    @Test
    @DisplayName("decode restores the original fields")
    void decodeRoundTrip() {
        String[] fields = {"Tinte | mechas", "C:\\ruta", "línea 1\nlínea 2", ""};

        assertThat(TextLineCodec.decode(TextLineCodec.encode(fields))).containsExactly(fields);
    }

    @Test
    @DisplayName("decode keeps empty fields, including trailing ones")
    void decodeKeepsEmptyFields() {
        assertThat(TextLineCodec.decode("1||")).containsExactly("1", "", "");
    }

    @Test
    @DisplayName("encode writes null as an empty field")
    void encodeWritesNullAsEmpty() {
        assertThat(TextLineCodec.encode("1", null)).isEqualTo("1|");
    }
}
