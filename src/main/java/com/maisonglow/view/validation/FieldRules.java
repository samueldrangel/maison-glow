package com.maisonglow.view.validation;

import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * The rules of the fields that a person fills in, written once. Each rule gets what is in the field and answers {@code null}
 * if it is fine, or the sentence to show under the field, in Spanish and in "tú" (the interface tone). They check what can be
 * known by looking at the field alone, before the service is asked: a required field that is empty, a length, a format, a value
 * that makes no sense. They are never a replacement for the service, which keeps deciding what is allowed (a username already in
 * use, a wrong password, a taken hour); they only spare the person a round trip for a mistake that is visible in the field.
 * The limits (name, document, phone, email, username, password) match what the services accept, so no valid value is refused here.
 */
public final class FieldRules {

    /** Longest name accepted. */
    public static final int NAME_MAX = 100;
    /** Document: letters and numbers, from {@link #DOCUMENT_MIN} to {@link #DOCUMENT_MAX}. */
    public static final int DOCUMENT_MIN = 4;
    /** Longest document accepted. */
    public static final int DOCUMENT_MAX = 20;
    /** Longest email accepted. */
    public static final int EMAIL_MAX = 120;
    /** Username: from {@link #USERNAME_MIN} to {@link #USERNAME_MAX} characters. */
    public static final int USERNAME_MIN = 3;
    /** Longest username accepted. */
    public static final int USERNAME_MAX = 80;
    /** Password: from {@link #PASSWORD_MIN} to {@link #PASSWORD_MAX} characters. */
    public static final int PASSWORD_MIN = 12;
    /** Longest password accepted. */
    public static final int PASSWORD_MAX = 128;
    /** Longest notes accepted for a record of the history. */
    public static final int NOTES_MAX = 4000;
    /** Longest message sent to the assistant. */
    public static final int MESSAGE_MAX = 500;
    /** Longest text typed in a search box. */
    public static final int SEARCH_MAX = 80;

    private static final Pattern NAME = Pattern.compile("^\\p{L}[\\p{L} .'-]*$");
    private static final Pattern DOCUMENT = Pattern.compile("^[0-9A-Za-z]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?\\d{7,12}$");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._@+-]*$");

    private FieldRules() {
    }

    /**
     * @param value what is in the field
     * @return null if it is a plausible full name, or what to tell the person
     */
    public static String fullName(String value) {
        String text = trimmed(value);
        if (text.isEmpty()) {
            return "Escribe tu nombre completo.";
        }
        if (text.length() < 3) {
            return "El nombre es muy corto: escribe tu nombre completo.";
        }
        if (text.length() > NAME_MAX) {
            return "El nombre es muy largo: usa máximo " + NAME_MAX + " caracteres.";
        }
        if (text.chars().anyMatch(Character::isDigit)) {
            return "El nombre no puede tener números.";
        }
        return NAME.matcher(text).matches() ? null : "El nombre solo puede tener letras, espacios y . ' -";
    }

    /**
     * @param value what is in the field
     * @return null if it is a plausible identity document, or what to tell the person
     */
    public static String document(String value) {
        String text = trimmed(value);
        if (text.isEmpty()) {
            return "Escribe tu documento.";
        }
        if (text.length() < DOCUMENT_MIN || text.length() > DOCUMENT_MAX || !DOCUMENT.matcher(text).matches()) {
            return "El documento debe tener entre " + DOCUMENT_MIN + " y " + DOCUMENT_MAX + " letras o números, sin espacios.";
        }
        return null;
    }

    /**
     * @param value what is in the field
     * @return null if it is a phone number of 7 to 12 digits (optionally starting with +), or what to tell the person
     */
    public static String phone(String value) {
        String text = trimmed(value);
        if (text.isEmpty()) {
            return "Escribe tu teléfono.";
        }
        return PHONE.matcher(text).matches() ? null : "El teléfono debe tener entre 7 y 12 dígitos (puede empezar con +).";
    }

    /**
     * @param value what is in the field
     * @return null if it looks like an email address, or what to tell the person
     */
    public static String email(String value) {
        String text = trimmed(value);
        if (text.isEmpty()) {
            return "Escribe tu correo.";
        }
        if (text.length() > EMAIL_MAX) {
            return "El correo es muy largo: usa máximo " + EMAIL_MAX + " caracteres.";
        }
        return EMAIL.matcher(text).matches() ? null : "El correo no tiene un formato válido, por ejemplo nombre@correo.com.";
    }

    /**
     * @param value what is in the field, to choose a new username
     * @return null if the service would accept it, or what to tell the person
     */
    public static String newUsername(String value) {
        String text = trimmed(value);
        if (text.isEmpty()) {
            return "Elige un usuario.";
        }
        if (text.length() < USERNAME_MIN || text.length() > USERNAME_MAX) {
            return "El usuario debe tener entre " + USERNAME_MIN + " y " + USERNAME_MAX + " caracteres.";
        }
        return USERNAME.matcher(text).matches() ? null : "El usuario solo puede tener letras sin tildes, números y . _ @ + -";
    }

    /**
     * @param value what is in the field, to sign in
     * @return null if something was written, or what to tell the person
     */
    public static String loginUsername(String value) {
        return trimmed(value).isEmpty() ? "Escribe tu usuario." : null;
    }

    /**
     * @param value what is in the field, to sign in (it is never trimmed or limited: it is what the person has)
     * @return null if something was written, or what to tell the person
     */
    public static String loginPassword(String value) {
        return value == null || value.isEmpty() ? "Escribe tu contraseña." : null;
    }

    /**
     * @param value what is in the field, to choose a new password
     * @return null if it has an acceptable length, or what to tell the person
     */
    public static String newPassword(String value) {
        if (value == null || value.isBlank()) {
            return "Escribe una contraseña.";
        }
        if (value.length() < PASSWORD_MIN) {
            int missing = PASSWORD_MIN - value.length();
            return "La contraseña debe tener al menos " + PASSWORD_MIN + " caracteres (te " + (missing == 1 ? "falta 1)." : "faltan " + missing + ").");
        }
        return value.length() > PASSWORD_MAX ? "La contraseña puede tener máximo " + PASSWORD_MAX + " caracteres." : null;
    }

    /**
     * @param password     the password typed
     * @param confirmation the password typed again
     * @return null if both are the same, or what to tell the person
     */
    public static String confirmation(String password, String confirmation) {
        if (confirmation == null || confirmation.isEmpty()) {
            return "Repite la contraseña para confirmarla.";
        }
        return confirmation.equals(password) ? null : "La confirmación no coincide con la contraseña.";
    }

    /**
     * @param value what is in the field of the current password
     * @return null if something was written, or what to tell the person
     */
    public static String currentPassword(String value) {
        return value == null || value.isEmpty() ? "Escribe tu contraseña actual." : null;
    }

    /**
     * @param current the current password
     * @param replacement the new one
     * @return null if the new one is different from the current one, or what to tell the person
     */
    public static String differentFromCurrent(String current, String replacement) {
        return current != null && !current.isEmpty() && current.equals(replacement)
            ? "La contraseña nueva debe ser distinta de la actual." : null;
    }

    /**
     * @param value what was written about a change of image
     * @return null if there is something to keep and it fits, or what to tell the person
     */
    public static String notes(String value) {
        String text = trimmed(value);
        if (text.isEmpty()) {
            return "Cuéntanos qué cambiaste.";
        }
        if (value.length() > NOTES_MAX) {
            return "Las notas pueden tener máximo " + NOTES_MAX + " caracteres (llevas " + value.length() + ").";
        }
        return null;
    }

    /**
     * @param date  the day chosen, null if none
     * @param today the current day
     * @return null if there is a day and it is not in the future, or what to tell the person
     */
    public static String dayNotInTheFuture(LocalDate date, LocalDate today) {
        if (date == null) {
            return "Elige la fecha.";
        }
        return date.isAfter(today) ? "La fecha no puede ser futura." : null;
    }

    /**
     * @param date  the day chosen, null if none
     * @param today the current day
     * @return null if there is a day and it is not in the past, or what to tell the person
     */
    public static String dayNotInThePast(LocalDate date, LocalDate today) {
        if (date == null) {
            return "Elige la fecha.";
        }
        return date.isBefore(today) ? "La fecha no puede ser anterior a hoy." : null;
    }

    /**
     * @param chosen the value chosen in a list, null if none
     * @param what   what is being chosen, with its article, for the sentence: for example {@code "un tratamiento"}
     * @return null if something was chosen, or what to tell the person
     */
    public static String choice(Object chosen, String what) {
        return chosen == null ? "Elige " + what + "." : null;
    }

    /**
     * @param value what is in the message field of the assistant
     * @return null if it fits, or what to tell the person
     */
    public static String assistantMessage(String value) {
        return value != null && value.length() > MESSAGE_MAX
            ? "El mensaje es muy largo: usa máximo " + MESSAGE_MAX + " caracteres (llevas " + value.length() + ")." : null;
    }

    private static String trimmed(String value) {
        return value == null ? "" : value.trim();
    }
}
