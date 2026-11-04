package wealthsei.domain;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * A 0 to 100 score built from transparent parts.
 *
 * @param total         the score; parts that could not be measured are left out and the rest re-scaled to 100
 * @param components    the parts that were measured
 * @param excluded      parts that were left out, each with the reason
 * @param explanation   plain-language explanation
 * @param previousTotal last month's score, or null if it could not be worked out
 */
public record HealthScore(int total, List<ScoreComponent> components, List<String> excluded, String explanation,
                          Integer previousTotal) {

    public HealthScore {
        components = List.copyOf(Objects.requireNonNull(components, "components"));
        excluded = List.copyOf(Objects.requireNonNull(excluded, "excluded"));
        Objects.requireNonNull(explanation, "explanation");
        if (total < 0 || total > 100) {
            throw new IllegalArgumentException("Score must be between 0 and 100, was " + total);
        }
    }

    /** False when nothing could be measured, so there is no score to show. */
    public boolean isAvailable() {
        return !components.isEmpty();
    }

    public OptionalInt changeFromLastMonth() {
        return previousTotal == null ? OptionalInt.empty() : OptionalInt.of(total - previousTotal);
    }

    public HealthScore withExplanation(String text) {
        return new HealthScore(total, components, excluded, text, previousTotal);
    }

    public HealthScore withPreviousTotal(Integer previous) {
        return new HealthScore(total, components, excluded, explanation, previous);
    }
}
