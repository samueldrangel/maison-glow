package com.maisonglow.util;

import com.maisonglow.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Validator tests")
class ValidatorTest {

    @Test
    @DisplayName("requireText accepts a non-blank value")
    void requireTextAcceptsNonBlankValue() {
        assertThatCode(() -> Validator.requireText("Ana", "nombre")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("requireText rejects null and blank values")
    void requireTextRejectsNullAndBlank() {
        assertThatThrownBy(() -> Validator.requireText(null, "nombre"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("nombre");
        assertThatThrownBy(() -> Validator.requireText("   ", "nombre"))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("requireEmail accepts a well-formed address")
    void requireEmailAcceptsValidAddress() {
        assertThatCode(() -> Validator.requireEmail("ana.perez@correo.com")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("requireEmail rejects malformed addresses")
    void requireEmailRejectsMalformedAddress() {
        assertThatThrownBy(() -> Validator.requireEmail("ana@")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Validator.requireEmail("sin-arroba.com")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Validator.requireEmail("")).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("requirePhone accepts 7 to 12 digits with optional plus sign")
    void requirePhoneAcceptsValidNumbers() {
        assertThatCode(() -> Validator.requirePhone("3001234567")).doesNotThrowAnyException();
        assertThatCode(() -> Validator.requirePhone("+573001234567")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("requirePhone rejects short numbers and letters")
    void requirePhoneRejectsInvalidNumbers() {
        assertThatThrownBy(() -> Validator.requirePhone("12345")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Validator.requirePhone("30012abc67")).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("requirePositive rejects zero and negative numbers")
    void requirePositiveRejectsZeroAndNegative() {
        assertThatCode(() -> Validator.requirePositive(0.5, "precio")).doesNotThrowAnyException();
        assertThatThrownBy(() -> Validator.requirePositive(0, "precio")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Validator.requirePositive(-3, "precio")).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("requireNonNegative accepts zero and rejects negative numbers")
    void requireNonNegativeRejectsNegative() {
        assertThatCode(() -> Validator.requireNonNegative(0, "stock")).doesNotThrowAnyException();
        assertThatThrownBy(() -> Validator.requireNonNegative(-1, "stock"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("stock");
    }
}
