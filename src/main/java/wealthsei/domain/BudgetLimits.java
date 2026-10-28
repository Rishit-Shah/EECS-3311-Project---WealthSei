package wealthsei.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The limits a user types in for a month, before they become a {@link Budget}.
 * Keeps the order the user entered them in.
 *
 * @param limits category to limit
 */
public record BudgetLimits(Map<Category, Money> limits) {

    public BudgetLimits {
        Objects.requireNonNull(limits, "limits");
        limits = Collections.unmodifiableMap(new LinkedHashMap<>(limits));
    }
}
