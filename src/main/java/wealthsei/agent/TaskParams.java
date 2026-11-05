package wealthsei.agent;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Extra inputs for a task, such as the item and price of an affordability question. */
public record TaskParams(Map<String, String> values) {

    public TaskParams {
        values = Map.copyOf(Objects.requireNonNull(values, "values"));
    }

    public static TaskParams none() {
        return new TaskParams(Map.of());
    }

    public static TaskParams of(String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Expected name/value pairs");
        }
        Map<String, String> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return new TaskParams(map);
    }

    public Optional<String> get(String name) {
        return Optional.ofNullable(values.get(name));
    }
}
