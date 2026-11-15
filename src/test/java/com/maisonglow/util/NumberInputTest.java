package com.maisonglow.util;

import com.maisonglow.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NumberInput tests")
class NumberInputTest {

    @ParameterizedTest(name = "reads \"{0}\" as {1}")
    @CsvSource(delimiter = '|', value = {
        "28000        | 28000",
        "28.000       | 28000",
        "$ 28.000     | 28000",
        "1.250.000    | 1250000",
        "28.000,50    | 28000.50",
        "28,5         | 28.5",
        "28.5         | 28.5",
        "  12000  | 12000"})
    @DisplayName("accepts the ways a Colombian user writes an amount")
    void readsAmounts(String typed, String expected) throws Exception {
        assertThat(NumberInput.money(typed, "precio")).isEqualByComparingTo(new BigDecimal(expected));
    }

    @ParameterizedTest(name = "rejects \"{0}\"")
    @CsvSource(delimiter = '|', value = {"abc", "12.3.4", "28k", "--5"})
    @DisplayName("rejects what is not a number and names the field")
    void rejectsText(String typed) {
        assertThatThrownBy(() -> NumberInput.money(typed, "precio"))
            .isInstanceOf(ValidationException.class).hasMessageContaining("campo precio").hasMessageContaining("número");
    }

    @Test
    @DisplayName("asks for the amount when it is empty")
    void requiresTheAmount() {
        assertThatThrownBy(() -> NumberInput.money("  ", "precio"))
            .isInstanceOf(ValidationException.class).hasMessage("El campo precio es obligatorio.");
    }

    @Test
    @DisplayName("keeps a negative amount so the service can reject it with its own message")
    void keepsTheSign() throws Exception {
        assertThat(NumberInput.money("-500", "precio")).isEqualByComparingTo("-500");
    }

    @Test
    @DisplayName("reads whole numbers with their sign")
    void readsWholeNumbers() throws Exception {
        assertThat(NumberInput.wholeNumber("5", "cantidad")).isEqualTo(5);
        assertThat(NumberInput.wholeNumber(" -3 ", "cantidad")).isEqualTo(-3);
        assertThat(NumberInput.wholeNumber("+2", "cantidad")).isEqualTo(2);
    }

    @Test
    @DisplayName("rejects decimals, text and numbers that do not fit as whole numbers")
    void rejectsWholeNumberErrors() {
        assertThatThrownBy(() -> NumberInput.wholeNumber("2.5", "cantidad"))
            .isInstanceOf(ValidationException.class).hasMessage("El campo cantidad debe ser un número entero.");
        assertThatThrownBy(() -> NumberInput.wholeNumber("diez", "cantidad")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> NumberInput.wholeNumber("99999999999", "cantidad")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> NumberInput.wholeNumber("", "cantidad"))
            .isInstanceOf(ValidationException.class).hasMessage("El campo cantidad es obligatorio.");
    }
}
