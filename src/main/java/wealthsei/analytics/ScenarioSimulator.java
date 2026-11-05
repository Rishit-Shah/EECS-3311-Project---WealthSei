package wealthsei.analytics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import wealthsei.domain.Forecast;
import wealthsei.domain.Money;
import wealthsei.domain.SavingsGoal;
import wealthsei.repository.GoalRepository;
import wealthsei.service.GoalPlanner;

/**
 * Runs a what-if: projects the baseline for six months, applies the changes to a deep copy of the
 * baseline inputs (Prototype pattern, so the real data can never be altered), projects again, and
 * compares end balance, lowest balance, monthly savings and goal completion dates. No AI.
 */
public class ScenarioSimulator {

    /** Six months. */
    public static final int HORIZON_DAYS = 180;
    public static final int MONTHS = 6;

    private final ForecastEngine engine;
    private final GoalRepository goals;
    private final GoalPlanner planner;
    private final Function<LocalDate, ForecastInputs> baselineProvider;

    public ScenarioSimulator(ForecastEngine engine, GoalRepository goals, GoalPlanner planner,
                             Function<LocalDate, ForecastInputs> baselineProvider) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.goals = Objects.requireNonNull(goals, "goals");
        this.planner = Objects.requireNonNull(planner, "planner");
        this.baselineProvider = Objects.requireNonNull(baselineProvider, "baselineProvider");
    }

    /** Simulates against the user's real baseline for {@code today}. */
    public ScenarioResult simulate(Scenario scenario, LocalDate today) {
        return simulate(scenario, baselineProvider.apply(today));
    }

    /**
     * @throws wealthsei.service.ValidationException if a change refers to something that does not exist
     */
    public ScenarioResult simulate(Scenario scenario, ForecastInputs baseline) {
        Objects.requireNonNull(scenario, "scenario");
        Objects.requireNonNull(baseline, "baseline");
        Forecast baseForecast = engine.project(baseline, HORIZON_DAYS);
        ForecastInputs changed = baseline.deepCopy();
        List<String> described = new ArrayList<>();
        for (ScenarioChange change : scenario.changes()) {
            changed = change.applyTo(changed);
            described.add(change.describe());
        }
        Forecast scenarioForecast = engine.project(changed, HORIZON_DAYS);
        Money baseSavings = engine.monthlySurplus(baseline);
        Money scenarioSavings = engine.monthlySurplus(changed);
        return new ScenarioResult(MONTHS, last(baseForecast), last(scenarioForecast), baseForecast.lowestBalance(),
                scenarioForecast.lowestBalance(), baseSavings, scenarioSavings, scenarioSavings.subtract(baseSavings),
                described, goalImpacts(baseline.today(), baseSavings, scenarioSavings));
    }

    private List<ScenarioResult.GoalImpact> goalImpacts(LocalDate today, Money baseSavings, Money scenarioSavings) {
        List<ScenarioResult.GoalImpact> impacts = new ArrayList<>();
        List<SavingsGoal> all = goals.findAll();
        for (SavingsGoal goal : all) {
            if (goal.isAchieved()) {
                continue;
            }
            Money others = Money.ZERO;
            for (SavingsGoal other : all) {
                if (!other.id().equals(goal.id()) && !other.isAchieved()) {
                    others = others.add(other.monthlyContribution());
                }
            }
            impacts.add(new ScenarioResult.GoalImpact(goal.name(),
                    planner.projectedCompletion(goal, baseSavings.subtract(others), today).orElse(null),
                    planner.projectedCompletion(goal, scenarioSavings.subtract(others), today).orElse(null)));
        }
        return impacts;
    }

    private static Money last(Forecast f) {
        return f.days().get(f.days().size() - 1).balance();
    }
}
