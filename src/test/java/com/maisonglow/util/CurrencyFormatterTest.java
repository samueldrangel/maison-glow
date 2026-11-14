package com.maisonglow.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@DisplayName("CurrencyFormatter tests")
class CurrencyFormatterTest {

    @Test
    @DisplayName("writes pesos with dots as thousands separators and no decimals")
    void writesColombianPesos() {
        assertThat(CurrencyFormatter.format(new BigDecimal("28000"))).isEqualTo("$ 28.000");
        assertThat(CurrencyFormatter.format(1250000.0)).isEqualTo("$ 1.250.000");
    }

    @Test
    @DisplayName("writes zero")
    void writesZero() {
        assertThat(CurrencyFormatter.format(BigDecimal.ZERO)).isEqualTo("$ 0");
    }

    @Test
    @DisplayName("rounds half up to whole pesos")
    void roundsHalfUp() {
        assertThat(CurrencyFormatter.format(new BigDecimal("1999.50"))).isEqualTo("$ 2.000");
        assertThat(CurrencyFormatter.format(new BigDecimal("1999.49"))).isEqualTo("$ 1.999");
    }

    @Test
    @DisplayName("gives the same text for a BigDecimal and a double")
    void sameTextForBothTypes() {
        assertThat(CurrencyFormatter.format(new BigDecimal("45000"))).isEqualTo(CurrencyFormatter.format(45000.0));
    }

    @Test
    @DisplayName("rejects a missing amount")
    void rejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> CurrencyFormatter.format((BigDecimal) null));
    }
}
