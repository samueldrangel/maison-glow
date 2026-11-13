package com.maisonglow.ai;

import java.nio.file.Path;

/** Customer, local photo and requested appearance change. */
public record TryOnRequest(int customerId, Path photo, String transformation) { }
