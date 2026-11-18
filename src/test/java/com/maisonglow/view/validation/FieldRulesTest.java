package com.maisonglow.view.validation;

import com.maisonglow.exception.ValidationException;
import com.maisonglow.util.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("Rules of the fields")
class FieldRulesTest {

    @Test
    @DisplayName("a full name needs letters, no numbers, a sensible length; accents, spaces and common marks are fine")
    void fullName() {
        assertThat(FieldRules.fullName("Ana María García-López")).isNull();
        assertThat(FieldRules.fullName("  Nicoll Gómez  ")).isNull();
        assertThat(FieldRules.fullName("O'Brien Peña")).isNull();
        assertThat(FieldRules.fullName("")).isEqualTo("Escribe tu nombre completo.");
        assertThat(FieldRules.fullName("   ")).isEqualTo("Escribe tu nombre completo.");
        assertThat(FieldRules.fullName(null)).isEqualTo("Escribe tu nombre completo.");
        assertThat(FieldRules.fullName("Al")).contains("muy corto");
        assertThat(FieldRules.fullName("Ana 2")).contains("números");
        assertThat(FieldRules.fullName("Ana #")).contains("solo puede tener letras");
        assertThat(FieldRules.fullName("A".repeat(FieldRules.NAME_MAX + 1))).contains("muy largo");
    }

    @Test
    @DisplayName("a document has letters and numbers only, with no spaces, between 4 and 20")
    void document() {
        assertThat(FieldRules.document("1001")).isNull();
        assertThat(FieldRules.document("1001234567")).isNull();
        assertThat(FieldRules.document("AB123456")).isNull();
        assertThat(FieldRules.document("")).isEqualTo("Escribe tu documento.");
        assertThat(FieldRules.document("123")).contains("entre 4 y 20");
        assertThat(FieldRules.document("1234 5678")).contains("sin espacios");
        assertThat(FieldRules.document("1".repeat(21))).contains("entre 4 y 20");
        assertThat(FieldRules.document("12-3456")).contains("letras o números");
    }

    @Test
    @DisplayName("the phone rule is the one of the service: 7 to 12 digits, with an optional +")
    void phone() throws ValidationException {
        assertThat(FieldRules.phone("3001112233")).isNull();
        assertThat(FieldRules.phone("+573001112233")).isNull();
        assertThat(FieldRules.phone("1234567890123")).contains("entre 7 y 12");
        assertThat(FieldRules.phone("+5730011122")).isNull();
        assertThat(FieldRules.phone("")).isEqualTo("Escribe tu teléfono.");
        assertThat(FieldRules.phone("12345")).contains("entre 7 y 12");
        assertThat(FieldRules.phone("300 111 2233")).contains("entre 7 y 12");
        assertThat(FieldRules.phone("abc1234567")).contains("entre 7 y 12");
        for (String phone : new String[] {"3001112233", "+5730011122", "12345", "300 111 2233", "1234567890123"}) {
            boolean accepted = FieldRules.phone(phone) == null;
            assertThat(accepted).as("same answer as the service for " + phone).isEqualTo(serviceAccepts(() -> Validator.requirePhone(phone)));
        }
    }

    @Test
    @DisplayName("the email rule is the one of the service, with an example in the message")
    void email() {
        assertThat(FieldRules.email("ana@correo.com")).isNull();
        assertThat(FieldRules.email("ana.garcia+tienda@mail.co.uk")).isNull();
        assertThat(FieldRules.email("")).isEqualTo("Escribe tu correo.");
        assertThat(FieldRules.email("sin-arroba")).contains("nombre@correo.com");
        assertThat(FieldRules.email("ana@correo")).contains("formato válido");
        assertThat(FieldRules.email("a@b.co" + "o".repeat(FieldRules.EMAIL_MAX))).contains("muy largo");
        for (String email : new String[] {"ana@correo.com", "sin-arroba", "ana@correo", "a b@c.co"}) {
            assertThat(FieldRules.email(email) == null).as(email).isEqualTo(serviceAccepts(() -> Validator.requireEmail(email)));
        }
    }

    private interface Check {
        void run() throws ValidationException;
    }

    private static boolean serviceAccepts(Check check) {
        try {
            check.run();
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }

    @Test
    @DisplayName("a new username: 3 to 80 characters, letters without accents, numbers and . _ @ + -, starting with a letter or number")
    void newUsername() {
        assertThat(FieldRules.newUsername("ana")).isNull();
        assertThat(FieldRules.newUsername("Ana.Garcia_1")).isNull();
        assertThat(FieldRules.newUsername("ana@correo.com")).isNull();
        assertThat(FieldRules.newUsername("")).isEqualTo("Elige un usuario.");
        assertThat(FieldRules.newUsername("an")).contains("entre 3 y 80");
        assertThat(FieldRules.newUsername("a".repeat(81))).contains("entre 3 y 80");
        assertThat(FieldRules.newUsername("ñandú")).contains("sin tildes");
        assertThat(FieldRules.newUsername("ana lopez")).contains("sin tildes");
        assertThat(FieldRules.newUsername(".ana")).contains("sin tildes");
    }

    @Test
    @DisplayName("signing in only asks that something was written; a password is never trimmed")
    void login() {
        assertThat(FieldRules.loginUsername("ana")).isNull();
        assertThat(FieldRules.loginUsername("  ")).isEqualTo("Escribe tu usuario.");
        assertThat(FieldRules.loginPassword("x")).isNull();
        assertThat(FieldRules.loginPassword(" ")).isNull();
        assertThat(FieldRules.loginPassword("")).isEqualTo("Escribe tu contraseña.");
        assertThat(FieldRules.loginPassword(null)).isEqualTo("Escribe tu contraseña.");
    }

    @Test
    @DisplayName("a new password has 12 to 128 characters and the message says how many are missing")
    void newPassword() {
        assertThat(FieldRules.newPassword("clave-segura-1")).isNull();
        assertThat(FieldRules.newPassword("a".repeat(12))).isNull();
        assertThat(FieldRules.newPassword("con espacios al medio 1")).isNull();
        assertThat(FieldRules.newPassword("")).isEqualTo("Escribe una contraseña.");
        assertThat(FieldRules.newPassword("corta")).isEqualTo("La contraseña debe tener al menos 12 caracteres (te faltan 7).");
        assertThat(FieldRules.newPassword("a".repeat(11))).contains("te falta 1)");
        assertThat(FieldRules.newPassword("a".repeat(129))).contains("máximo 128");
    }

    @Test
    @DisplayName("the confirmation must be there and the same; the new password must differ from the current one")
    void confirmationAndChange() {
        assertThat(FieldRules.confirmation("clave-segura-1", "clave-segura-1")).isNull();
        assertThat(FieldRules.confirmation("clave-segura-1", "")).contains("Repite");
        assertThat(FieldRules.confirmation("clave-segura-1", "otra")).contains("confirmación no coincide");
        assertThat(FieldRules.currentPassword("")).isEqualTo("Escribe tu contraseña actual.");
        assertThat(FieldRules.currentPassword("x")).isNull();
        assertThat(FieldRules.differentFromCurrent("clave-vieja-1", "clave-nueva-1")).isNull();
        assertThat(FieldRules.differentFromCurrent("clave-vieja-1", "clave-vieja-1")).contains("distinta de la actual");
        assertThat(FieldRules.differentFromCurrent("", "")).isNull();
    }

    @Test
    @DisplayName("the notes of a change are required and at most 4.000 characters, saying how many were typed")
    void notes() {
        assertThat(FieldRules.notes("Me pinté el cabello")).isNull();
        assertThat(FieldRules.notes("x".repeat(FieldRules.NOTES_MAX))).isNull();
        assertThat(FieldRules.notes("")).isEqualTo("Cuéntanos qué cambiaste.");
        assertThat(FieldRules.notes("   ")).isEqualTo("Cuéntanos qué cambiaste.");
        assertThat(FieldRules.notes("x".repeat(FieldRules.NOTES_MAX + 5))).contains("máximo 4000").contains("4005");
    }

    @Test
    @DisplayName("the days: a record cannot be of a future day and an appointment cannot be of a past one")
    void days() {
        LocalDate today = LocalDate.of(2026, 11, 18);

        assertThat(FieldRules.dayNotInTheFuture(today, today)).isNull();
        assertThat(FieldRules.dayNotInTheFuture(today.minusDays(3), today)).isNull();
        assertThat(FieldRules.dayNotInTheFuture(today.plusDays(1), today)).isEqualTo("La fecha no puede ser futura.");
        assertThat(FieldRules.dayNotInTheFuture(null, today)).isEqualTo("Elige la fecha.");
        assertThat(FieldRules.dayNotInThePast(today, today)).isNull();
        assertThat(FieldRules.dayNotInThePast(today.plusDays(9), today)).isNull();
        assertThat(FieldRules.dayNotInThePast(today.minusDays(1), today)).contains("anterior a hoy");
        assertThat(FieldRules.dayNotInThePast(null, today)).isEqualTo("Elige la fecha.");
    }

    @Test
    @DisplayName("a list needs a choice, and the message names what to choose")
    void choice() {
        assertThat(FieldRules.choice("algo", "un tratamiento")).isNull();
        assertThat(FieldRules.choice(null, "un tratamiento")).isEqualTo("Elige un tratamiento.");
        assertThat(FieldRules.choice(null, "un horario")).isEqualTo("Elige un horario.");
    }

    @Test
    @DisplayName("a message to the assistant has a limit and the message says how many characters were typed")
    void assistantMessage() {
        assertThat(FieldRules.assistantMessage("hola")).isNull();
        assertThat(FieldRules.assistantMessage(null)).isNull();
        assertThat(FieldRules.assistantMessage("x".repeat(FieldRules.MESSAGE_MAX))).isNull();
        assertThat(FieldRules.assistantMessage("x".repeat(FieldRules.MESSAGE_MAX + 1))).contains("máximo 500").contains("501");
        assertThatCode(() -> FieldRules.assistantMessage("x")).doesNotThrowAnyException();
    }
}
