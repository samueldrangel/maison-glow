package com.maisonglow.dao;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts between the fields of a record and one line of a data file.
 * Fields are separated by {@code |}. Inside a field, {@code |} and the
 * backslash are written as {@code \|} and {@code \\}, and line breaks as
 * {@code \n} and {@code \r}, so every record always fits on a single line.
 */
public final class TextLineCodec {

    private static final char SEPARATOR = '|';
    private static final char ESCAPE = '\\';
    private static final char NEW_LINE = '\n';
    private static final char CARRIAGE_RETURN = '\r';
    private static final char NEW_LINE_CODE = 'n';
    private static final char CARRIAGE_RETURN_CODE = 'r';

    private TextLineCodec() {
    }

    /**
     * Joins the fields into one line, escaping the special characters.
     *
     * @param fields the field values; {@code null} is written as an empty field
     * @return the encoded line, without a line terminator
     */
    public static String encode(String... fields) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                line.append(SEPARATOR);
            }
            appendEscaped(line, fields[i]);
        }
        return line.toString();
    }

    /**
     * Splits a line into its fields, undoing the escapes made by
     * {@link #encode(String...)}. Empty fields are kept.
     *
     * @param line the encoded line
     * @return the decoded field values
     */
    public static String[] decode(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            boolean escapesNext = current == ESCAPE && i + 1 < line.length();
            if (escapesNext) {
                i++;
                field.append(unescape(line.charAt(i)));
            } else if (current == SEPARATOR) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(current);
            }
        }
        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }

    private static void appendEscaped(StringBuilder line, String value) {
        if (value == null) {
            return;
        }
        for (char current : value.toCharArray()) {
            switch (current) {
                case SEPARATOR, ESCAPE -> line.append(ESCAPE).append(current);
                case NEW_LINE -> line.append(ESCAPE).append(NEW_LINE_CODE);
                case CARRIAGE_RETURN -> line.append(ESCAPE).append(CARRIAGE_RETURN_CODE);
                default -> line.append(current);
            }
        }
    }

    private static char unescape(char code) {
        return switch (code) {
            case NEW_LINE_CODE -> NEW_LINE;
            case CARRIAGE_RETURN_CODE -> CARRIAGE_RETURN;
            default -> code;
        };
    }
}
