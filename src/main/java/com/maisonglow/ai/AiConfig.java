package com.maisonglow.ai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/** Reads provider selection without storing an API key in the repository. */
public final class AiConfig {
    /** Available provider modes. */
    public enum Provider { SIMULATED, API }

    private static final String DEFAULT_KEY_ENV = "MAISON_GLOW_AI_API_KEY";
    private final Provider provider;
    private final String apiKey;

    private AiConfig(Provider provider, String apiKey) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.apiKey = apiKey == null ? "" : apiKey;
    }

    /** Reads config.properties from the current directory and the process environment. */
    public static AiConfig load() {
        return load(Path.of("config.properties"), System.getenv());
    }

    /** Reads a chosen file and environment; absent/unreadable configuration selects simulation. */
    public static AiConfig load(Path file, Map<String, String> environment) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(environment, "environment");
        if (!Files.isRegularFile(file)) return simulated();
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        } catch (IOException | SecurityException e) {
            return simulated();
        }
        String provider = properties.getProperty("ai.provider", "simulated").trim().toLowerCase(Locale.ROOT);
        if (!provider.equals("api")) return simulated();
        String keyName = properties.getProperty("ai.api_key_env", DEFAULT_KEY_ENV).trim();
        if (keyName.isBlank()) return simulated();
        String key = environment.get(keyName);
        return key == null || key.isBlank() ? simulated() : new AiConfig(Provider.API, key.trim());
    }

    /** Returns a safe configuration without an external provider. */
    public static AiConfig simulated() {
        return new AiConfig(Provider.SIMULATED, "");
    }

    /** Returns the requested provider, after the key availability check. */
    public Provider provider() { return provider; }

    /** Returns the in-memory key for a future API adapter; never log this value. */
    public String apiKey() { return apiKey; }

    @Override public String toString() { return "AiConfig[provider=" + provider + ", apiKey=REDACTED]"; }
}
