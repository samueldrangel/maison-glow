package com.maisonglow.util;

import java.util.Arrays;
import java.util.Locale;

/** The initials of a person's name, for the round medallions that stand in for a photo. */
public final class Initials {

    private static final int MAX = 2;

    private Initials() {
    }

    /**
     * @param name a full name
     * @return up to two capital letters taken from the first two words, or an empty text if the name is blank
     */
    public static String of(String name) {
        if (name == null) {
            return "";
        }
        return Arrays.stream(name.trim().split("\\s+"))
            .filter(word -> !word.isEmpty())
            .limit(MAX)
            .map(word -> word.substring(0, 1).toUpperCase(Locale.ROOT))
            .reduce("", String::concat);
    }
}
