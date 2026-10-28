package wealthsei.domain;

import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The spending limits for one month: one {@link BudgetLine} per category, at least one line,
 * no category twice.
 *
 * @param month the month the budget is for
 * @param lines the per-category limits
 */
public record Budget(YearMonth month, List<BudgetLine> lines) {

    public Budget {
        Objects.requireNonNull(month, "month");
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("A budget needs at least one category limit");
        }
        Set<Category> seen = new HashSet<>();
        for (BudgetLine line : lines) {
            if (!seen.add(line.category())) {
                throw new IllegalArgumentException("Category appears twice in budget: " + line.category().name());
            }
        }
    }

    /** Builds a budget from the limits a user entered. Each limit must be positive. */
    public static Budget from(YearMonth month, BudgetLimits limits) {
        Objects.requireNonNull(limits, "limits");
        List<BudgetLine> lines = limits.limits().entrySet().stream()
                .map(e -> new BudgetLine(e.getKey(), e.getValue()))
                .toList();
        return new Budget(month, lines);
    }

    public Optional<Money> limitFor(Category category) {
        return lines.stream().filter(l -> l.category().equals(category))
                .map(BudgetLine::limit).findFirst();
    }

    public Money totalLimit() {
        return lines.stream().map(BudgetLine::limit).reduce(Money.ZERO, Money::add);
    }
}
