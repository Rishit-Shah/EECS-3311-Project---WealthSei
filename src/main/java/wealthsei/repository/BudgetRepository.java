package wealthsei.repository;

import java.time.YearMonth;
import java.util.Optional;
import wealthsei.domain.Budget;

/** Storage for monthly budgets. One budget per month; saving again replaces it. */
public interface BudgetRepository {

    void save(Budget budget);

    Optional<Budget> find(YearMonth month);
}
