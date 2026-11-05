package wealthsei.analytics;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import wealthsei.domain.Money;

/**
 * Baseline against scenario over the simulated period.
 *
 * @param months               how many months were simulated
 * @param baselineEndBalance   balance at the end without the changes
 * @param scenarioEndBalance   balance at the end with the changes
 * @param baselineLowest       lowest balance along the way without the changes
 * @param scenarioLowest       lowest balance along the way with the changes
 * @param baselineMonthlySavings money left each month without the changes
 * @param scenarioMonthlySavings money left each month with the changes
 * @param monthlySavingsDelta  scenario minus baseline, per month
 * @param changes              what was changed, in plain words
 * @param goalImpacts          when each goal would be reached, with and without the changes
 * @param typicalMonthlySpending everyday spending counted in both columns (from your past full months)
 */
public record ScenarioResult(int months, Money baselineEndBalance, Money scenarioEndBalance, Money baselineLowest,
                             Money scenarioLowest, Money baselineMonthlySavings, Money scenarioMonthlySavings,
                             Money monthlySavingsDelta, List<String> changes, List<GoalImpact> goalImpacts,
                             Money typicalMonthlySpending) {

    public ScenarioResult(int months, Money baselineEndBalance, Money scenarioEndBalance, Money baselineLowest,
                          Money scenarioLowest, Money baselineMonthlySavings, Money scenarioMonthlySavings,
                          Money monthlySavingsDelta, List<String> changes, List<GoalImpact> goalImpacts) {
        this(months, baselineEndBalance, scenarioEndBalance, baselineLowest, scenarioLowest, baselineMonthlySavings,
                scenarioMonthlySavings, monthlySavingsDelta, changes, goalImpacts, Money.ZERO);
    }

    /**
     * When a goal would be reached. A null date means it would not be reached at that saving rate.
     */
    public record GoalImpact(String goalName, LocalDate baselineCompletion, LocalDate scenarioCompletion) { }

    public ScenarioResult {
        Objects.requireNonNull(baselineEndBalance, "baselineEndBalance");
        Objects.requireNonNull(scenarioEndBalance, "scenarioEndBalance");
        Objects.requireNonNull(baselineLowest, "baselineLowest");
        Objects.requireNonNull(scenarioLowest, "scenarioLowest");
        Objects.requireNonNull(baselineMonthlySavings, "baselineMonthlySavings");
        Objects.requireNonNull(scenarioMonthlySavings, "scenarioMonthlySavings");
        Objects.requireNonNull(monthlySavingsDelta, "monthlySavingsDelta");
        changes = List.copyOf(changes);
        goalImpacts = List.copyOf(goalImpacts);
        Objects.requireNonNull(typicalMonthlySpending, "typicalMonthlySpending");
    }

    public Money endBalanceDelta() {
        return scenarioEndBalance.subtract(baselineEndBalance);
    }
}
