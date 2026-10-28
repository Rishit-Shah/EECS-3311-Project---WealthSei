package wealthsei.domain;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Spending against the budget for one month: a {@link BudgetLineStatus} per limited category.
 *
 * @param month the month
 * @param lines each category's spent / limit / level
 */
public record BudgetStatus(YearMonth month, List<BudgetLineStatus> lines) {

    public BudgetStatus {
        Objects.requireNonNull(month, "month");
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
    }

    public Money totalSpent() {
        return lines.stream().map(BudgetLineStatus::spent).reduce(Money.ZERO, Money::add);
    }

    public Money totalLimit() {
        return lines.stream().map(BudgetLineStatus::limit).reduce(Money.ZERO, Money::add);
    }

    public Optional<BudgetLineStatus> lineFor(Category category) {
        return lines.stream().filter(l -> l.category().equals(category)).findFirst();
    }

    /** Categories at the given level or worse (WARNING returns WARNING and OVER lines). */
    public List<BudgetLineStatus> linesAtLeast(AlertSeverity level) {
        return lines.stream().filter(l -> l.level().compareTo(level) >= 0).toList();
    }
}
