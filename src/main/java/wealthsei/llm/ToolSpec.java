package wealthsei.llm;

import java.util.List;
import java.util.Objects;

/** What a tool is called, what it does and what it accepts; sent to the model so it can choose. */
public record ToolSpec(String name, String description, List<ParamSpec> params) {

    public ToolSpec {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        params = List.copyOf(Objects.requireNonNull(params, "params"));
        if (name.isBlank()) {
            throw new IllegalArgumentException("Tool name must not be blank");
        }
    }

    /**
     * Checks a call before the tool runs: required arguments present, numbers really numbers,
     * no arguments the tool does not know.
     *
     * @throws IllegalArgumentException describing the first problem
     */
    public void validate(ToolArgs args) {
        for (ParamSpec p : params) {
            if (p.required() && !args.has(p.name())) {
                throw new IllegalArgumentException("Missing required argument '" + p.name() + "'");
            }
            if (args.has(p.name())) {
                switch (p.type()) {
                    case NUMBER -> args.decimal(p.name());
                    case INTEGER -> args.integer(p.name());
                    default -> { }
                }
            }
        }
        for (String given : args.asMap().keySet()) {
            if (params.stream().noneMatch(p -> p.name().equals(given))) {
                throw new IllegalArgumentException("Unknown argument '" + given + "'");
            }
        }
    }
}
