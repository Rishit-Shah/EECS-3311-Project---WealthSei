package wealthsei.repository;

import java.util.Optional;
import wealthsei.domain.FinancialPlan;

/** Storage for the user's single financial plan. */
public interface PlanRepository {

    /** Saves the plan, replacing any earlier one. */
    void save(FinancialPlan plan);

    Optional<FinancialPlan> find();
}
