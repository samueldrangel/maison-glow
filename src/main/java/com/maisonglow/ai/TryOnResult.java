package com.maisonglow.ai;

import java.nio.file.Path;
import java.util.Objects;

/** Generated local image and warning to display beside it. */
public record TryOnResult(Path image, String disclaimer, int contentWidth, int contentHeight) {
    public TryOnResult(Path image, String disclaimer) { this(image, disclaimer, 0, 0); }
    /** Requires a usable image location and a non-empty warning. */
    public TryOnResult {
        Objects.requireNonNull(image, "image");
        Objects.requireNonNull(disclaimer, "disclaimer");
        if (disclaimer.isBlank()) throw new IllegalArgumentException("El aviso es obligatorio.");
    }
}
