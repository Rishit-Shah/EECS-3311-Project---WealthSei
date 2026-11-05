package wealthsei.analytics;

import java.util.List;
import java.util.Objects;
import wealthsei.service.ValidationException;

/**
 * A what-if: a name and one to five changes to the baseline.
 *
 * @param id      identifier
 * @param name    short label
 * @param changes what is different in this scenario
 */
public record Scenario(String id, String name, List<ScenarioChange> changes) {

    public static final int MAX_CHANGES = 5;

    public Scenario {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        changes = List.copyOf(Objects.requireNonNull(changes, "changes"));
        if (changes.isEmpty()) {
            throw new ValidationException("A scenario needs at least one change");
        }
        if (changes.size() > MAX_CHANGES) {
            throw new ValidationException("A scenario can have at most " + MAX_CHANGES
                    + " changes; split it into smaller ones");
        }
    }
}
