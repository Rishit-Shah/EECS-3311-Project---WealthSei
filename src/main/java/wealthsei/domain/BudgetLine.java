package wealthsei.domain;

import java.util.Objects;

/**
 * One category's monthly spending limit inside a {@link Budget}.
 *
 * @param category the category being limited
 * @param limit    the most the user plans to spend in it this month (positive)
 */
public record BudgetLine(Category category, Money limit) {

    public BudgetLine {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(limit, "limit");
        if (!limit.isPositive()) {
            throw new IllegalArgumentException("Budget limit must be positive, was " + limit);
        }
    }
}
