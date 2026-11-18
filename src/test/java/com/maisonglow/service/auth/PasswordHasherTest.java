package com.maisonglow.service.auth;

import com.maisonglow.exception.ValidationException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PasswordHasherTest {
    private final PasswordHasher hasher = new PasswordHasher();

    @Test void saltsAreRandomAndPasswordsAreExact() throws Exception {
        char[] password = " fixture-password ".toCharArray();
        String first = hasher.hash(password);
        String second = hasher.hash(password);
        assertThat(first).isNotEqualTo(second);
        assertThat(hasher.matches(password, first)).isTrue();
        assertThat(hasher.matches("fixture-password".toCharArray(), first)).isFalse();
        assertThat(password).isEqualTo(" fixture-password ".toCharArray());
    }

    @Test void invalidInputAndMalformedStoredHashesFailClosed() {
        assertThatThrownBy(() -> hasher.hash(null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> hasher.hash("x".repeat(129).toCharArray())).isInstanceOf(ValidationException.class);
        for (String hash : new String[]{"plaintext", "pbkdf2-sha256$x$a$b", "pbkdf2-sha256$1$a$b",
            "pbkdf2-sha256$999999999$a$b", "pbkdf2-sha256$600000$!$!", "pbkdf2-sha256$600000$YQ==$Yg=="})
            assertThat(hasher.matches("fixture-password".toCharArray(), hash)).isFalse();
        assertThat(hasher.matches(null, "unsupported")).isFalse();
        assertThat(hasher.matches(new char[0], "unsupported")).isFalse();
        assertThat(hasher.matches("fixture-password".toCharArray(), null)).isFalse();
    }
}
