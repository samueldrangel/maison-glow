package com.maisonglow.ai;

import java.nio.file.Path;

/** Customer, local photo and requested appearance change. */
public record TryOnRequest(int customerId, Path photo, String transformation, double intensity) {
    /** Keeps existing integrations at medium intensity. */
    public TryOnRequest(int customerId, Path photo, String transformation) {
        this(customerId, photo, transformation, 0.5);
    }
}
