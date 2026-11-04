package wealthsei.domain;

import java.util.List;
import java.util.Objects;

/**
 * Can the goal be reached by its deadline?
 *
 * @param requiredMonthly what must be put aside each month
 * @param monthlySurplus  what is left each month after bills and the other goals
 * @param feasible        true if the surplus covers the required amount
 * @param options         suggestions (a single "keep going" when feasible)
 */
public record FeasibilityReport(Money requiredMonthly, Money monthlySurplus, boolean feasible,
                                List<GoalOption> options) {

    public FeasibilityReport {
        Objects.requireNonNull(requiredMonthly, "requiredMonthly");
        Objects.requireNonNull(monthlySurplus, "monthlySurplus");
        options = List.copyOf(Objects.requireNonNull(options, "options"));
    }
}
