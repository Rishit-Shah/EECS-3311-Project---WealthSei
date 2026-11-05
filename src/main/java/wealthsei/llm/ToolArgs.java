package wealthsei.llm;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** The arguments of a tool call, as text; typed getters convert and report bad values clearly. */
public final class ToolArgs {

    private final Map<String, String> values;

    public ToolArgs(Map<String, String> values) {
        this.values = new LinkedHashMap<>(Objects.requireNonNull(values, "values"));
    }

    public static ToolArgs of(String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Expected name/value pairs");
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return new ToolArgs(map);
    }

    public Map<String, String> asMap() {
        return Map.copyOf(values);
    }

    public boolean has(String name) {
        String v = values.get(name);
        return v != null && !v.isBlank();
    }

    public Optional<String> text(String name) {
        return has(name) ? Optional.of(values.get(name).trim()) : Optional.empty();
    }

    public String requireText(String name) {
        return text(name).orElseThrow(() -> new IllegalArgumentException("Missing argument '" + name + "'"));
    }

    public Optional<BigDecimal> decimal(String name) {
        return text(name).map(t -> {
            try {
                return new BigDecimal(t);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Argument '" + name + "' must be a number, was '" + t + "'");
            }
        });
    }

    public Optional<Integer> integer(String name) {
        return decimal(name).map(d -> {
            try {
                return d.intValueExact();
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("Argument '" + name + "' must be a whole number, was '" + d + "'");
            }
        });
    }

    /** Converts the object a model sent ({@code {"month":"2026-10","limit":5}}) into arguments. */
    public static ToolArgs fromJson(Map<String, Object> json) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : json.entrySet()) {
            Object v = e.getValue();
            if (v == null) {
                continue;
            }
            map.put(e.getKey(), v instanceof BigDecimal d ? d.toPlainString()
                    : v instanceof Map || v instanceof java.util.List ? Json.write(v) : v.toString());
        }
        return new ToolArgs(map);
    }

    @Override
    public String toString() {
        return values.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ToolArgs other && values.equals(other.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }
}
