package wealthsei.domain;

import java.util.Objects;

/**
 * How one category is doing against its limit.
 *
 * <p>Alert level: more than 100% of the limit is {@link AlertSeverity#OVER}; 80% or more is
 * {@link AlertSeverity#WARNING}; anything lower is {@link AlertSeverity#INFO}. Exactly 100%
 * is still a warning, because the limit has been reached but not exceeded. A zero limit with
 * any spending counts as OVER.
 *
 * @param category the category
 * @param spent    amount spent so far this month
 * @param limit    the category's limit (zero or more)
 */
public record BudgetLineStatus(Category category, Money spent, Money limit) {

    public BudgetLineStatus {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(spent, "spent");
        Objects.requireNonNull(limit, "limit");
        if (spent.isNegative()) {
            throw new IllegalArgumentException("Spent must not be negative, was " + spent);
        }
        if (limit.isNegative()) {
            throw new IllegalArgumentException("Limit must not be negative, was " + limit);
        }
    }

    /** Spent divided by limit: 0.5 means half used, 1.2 means 20% over. */
    public double ratio() {
        if (limit.isZero()) {
            return spent.isZero() ? 0.0 : Double.POSITIVE_INFINITY;
        }
        return spent.amount().doubleValue() / limit.amount().doubleValue();
    }

    public AlertSeverity level() {
        if (limit.isZero()) {
            return spent.isZero() ? AlertSeverity.INFO : AlertSeverity.OVER;
        }
        if (spent.compareTo(limit) > 0) {
            return AlertSeverity.OVER;
        }
        // 80% or more, compared in exact money terms: spent x 5 >= limit x 4
        if (spent.multiply(5).compareTo(limit.multiply(4)) >= 0) {
            return AlertSeverity.WARNING;
        }
        return AlertSeverity.INFO;
    }

    public Money remaining() {
        return limit.subtract(spent);
    }
}
