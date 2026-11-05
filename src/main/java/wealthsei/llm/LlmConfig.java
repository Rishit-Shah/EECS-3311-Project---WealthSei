package wealthsei.llm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The model settings: the API key and the two model names. They come from environment
 * variables, or from a {@code .env} file kept out of version control; an environment variable wins.
 * The key is never part of {@link #toString()}.
 */
public final class LlmConfig {

    public static final String KEY_VAR = "ANTHROPIC_API_KEY";
    public static final String SONNET_VAR = "WEALTHSEI_SONNET_MODEL";
    public static final String HAIKU_VAR = "WEALTHSEI_HAIKU_MODEL";
    public static final String DEFAULT_SONNET = "claude-sonnet-5-5";
    public static final String DEFAULT_HAIKU = "claude-haiku-5-5";
    private static final String PLACEHOLDER_PREFIX = "sk-ant-REPLACE";

    private final String apiKey;
    private final String sonnetModel;
    private final String haikuModel;

    public LlmConfig(String apiKey, String sonnetModel, String haikuModel) {
        this.apiKey = apiKey == null || apiKey.isBlank() || apiKey.startsWith(PLACEHOLDER_PREFIX) ? null : apiKey.trim();
        this.sonnetModel = blankTo(sonnetModel, DEFAULT_SONNET);
        this.haikuModel = blankTo(haikuModel, DEFAULT_HAIKU);
    }

    /** A configuration with no key: AI features are off and templates are used instead. */
    public static LlmConfig none() {
        return new LlmConfig(null, null, null);
    }

    /**
     * Reads {@code envFile} if it exists, then lets {@code env} override it.
     *
     * @throws IllegalStateException if the file exists but cannot be read
     */
    public static LlmConfig load(Path envFile, Map<String, String> env) {
        Map<String, String> values = new HashMap<>();
        if (envFile != null && Files.isRegularFile(envFile)) {
            try {
                for (String line : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
                    parseLine(line, values);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + envFile + ": " + e.getMessage(), e);
            }
        }
        for (String name : new String[] {KEY_VAR, SONNET_VAR, HAIKU_VAR}) {
            String fromEnv = env.get(name);
            if (fromEnv != null && !fromEnv.isBlank()) {
                values.put(name, fromEnv);
            }
        }
        return new LlmConfig(values.get(KEY_VAR), values.get(SONNET_VAR), values.get(HAIKU_VAR));
    }

    private static void parseLine(String line, Map<String, String> values) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return;
        }
        if (trimmed.startsWith("export ")) {
            trimmed = trimmed.substring(7).trim();
        }
        int eq = trimmed.indexOf('=');
        if (eq <= 0) {
            return;
        }
        String name = trimmed.substring(0, eq).trim();
        String value = trimmed.substring(eq + 1).trim();
        if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                || value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1);
        }
        values.put(name, value);
    }

    public boolean hasKey() {
        return apiKey != null;
    }

    /** The key, for the transport to put in a request header. Never print it. */
    Optional<String> apiKey() {
        return Optional.ofNullable(apiKey);
    }

    public String sonnetModel() {
        return sonnetModel;
    }

    public String haikuModel() {
        return haikuModel;
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    @Override
    public String toString() {
        return "LlmConfig[key=" + (hasKey() ? "set" : "not set") + ", sonnet=" + sonnetModel + ", haiku=" + haikuModel + "]";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LlmConfig c && Objects.equals(apiKey, c.apiKey)
                && sonnetModel.equals(c.sonnetModel) && haikuModel.equals(c.haikuModel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiKey, sonnetModel, haikuModel);
    }
}
