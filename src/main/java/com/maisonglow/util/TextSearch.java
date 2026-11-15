package com.maisonglow.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

/**
 * Compares text the way a person searches: without caring for capital letters
 * or accents, so "esmalte", "ESMALTE" and "Esmalté" find the same product.
 */
public final class TextSearch {

    private TextSearch() {
    }

    /**
     * @param text any text
     * @return the text in lower case and without accents
     */
    public static String normalize(String text) {
        Objects.requireNonNull(text, "text");
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /**
     * @param text   the text to look into
     * @param search what the user typed; an empty search matches everything
     * @return true if the text contains the search, ignoring case and accents
     */
    public static boolean contains(String text, String search) {
        return normalize(text).contains(normalize(search).trim());
    }
}
