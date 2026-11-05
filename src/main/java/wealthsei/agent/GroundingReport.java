package wealthsei.agent;

import java.util.List;
import java.util.Objects;

/**
 * The result of checking an answer's figures against what the tools returned.
 *
 * @param grounded          true if every checked figure was found in a tool result
 * @param ungroundedFigures the figures, as written, that no tool produced
 */
public record GroundingReport(boolean grounded, List<String> ungroundedFigures) {

    public GroundingReport {
        ungroundedFigures = List.copyOf(Objects.requireNonNull(ungroundedFigures, "ungroundedFigures"));
    }

    public static GroundingReport ok() {
        return new GroundingReport(true, List.of());
    }
}
