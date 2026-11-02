package wealthsei.repository;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.Budget;

/** Keeps budgets in memory. Used by tests now; a SQLite version replaces it for the app. */
public class InMemoryBudgetRepository implements BudgetRepository {

    private final Map<YearMonth, Budget> byMonth = new HashMap<>();

    @Override
    public void save(Budget budget) {
        Objects.requireNonNull(budget, "budget");
        byMonth.put(budget.month(), budget);
    }

    @Override
    public Optional<Budget> find(YearMonth month) {
        return Optional.ofNullable(byMonth.get(month));
    }
}
