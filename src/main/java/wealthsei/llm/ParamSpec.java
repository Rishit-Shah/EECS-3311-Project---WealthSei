package wealthsei.llm;

import java.util.Objects;

/** One parameter a tool accepts. */
public record ParamSpec(String name, ParamType type, String description, boolean required) {

    public ParamSpec {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(description, "description");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Parameter name must not be blank");
        }
    }
}
