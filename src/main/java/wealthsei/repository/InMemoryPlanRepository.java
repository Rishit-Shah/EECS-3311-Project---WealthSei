package wealthsei.repository;

import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.FinancialPlan;

/** Keeps the plan in memory. Used by tests now; a SQLite version replaces it for the app. */
public class InMemoryPlanRepository implements PlanRepository {

    private FinancialPlan plan;

    @Override
    public void save(FinancialPlan plan) {
        this.plan = Objects.requireNonNull(plan, "plan");
    }

    @Override
    public Optional<FinancialPlan> find() {
        return Optional.ofNullable(plan);
    }
}
