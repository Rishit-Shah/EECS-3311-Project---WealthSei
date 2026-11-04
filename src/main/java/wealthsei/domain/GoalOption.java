package wealthsei.domain;

import java.util.Objects;

/**
 * One suggestion for a goal.
 *
 * @param kind        which kind of change
 * @param description what to do, in plain words
 * @param impact      what it achieves
 */
public record GoalOption(GoalOptionKind kind, String description, String impact) {

    public GoalOption {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(impact, "impact");
    }
}
