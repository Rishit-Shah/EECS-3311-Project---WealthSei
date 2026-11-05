package wealthsei.agent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import wealthsei.service.ValidationException;

/** Named text values that describe a proposed change, e.g. month, category and limit. */
public record ParamSet(Map<String, String> values) {

    public ParamSet {
        Objects.requireNonNull(values, "values");
        values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static ParamSet empty() {
        return new ParamSet(Map.of());
    }

    /** @param keyValues alternating names and values */
    public static ParamSet of(String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Parameters must come in name/value pairs");
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return new ParamSet(map);
    }

    public Optional<String> get(String name) {
        return Optional.ofNullable(values.get(name)).map(String::trim).filter(v -> !v.isEmpty());
    }

    /** @throws ValidationException if the value is missing */
    public String require(String name) {
        return get(name).orElseThrow(() -> new ValidationException("The proposal is missing '" + name + "'"));
    }
}
