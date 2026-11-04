package wealthsei.analytics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import wealthsei.domain.AlertSeverity;
import wealthsei.domain.BudgetLineStatus;
import wealthsei.domain.BudgetStatus;
import wealthsei.domain.DailyBalance;
import wealthsei.domain.Forecast;
import wealthsei.domain.GoalStatus;
import wealthsei.domain.HealthScore;
import wealthsei.domain.SavingsGoal;
import wealthsei.domain.ScoreComponent;

/**
 * Builds the 0 to 100 financial health score from four transparent parts (no AI):
 * budget adherence 30, savings rate 25, emergency buffer 25, goal progress 20.
 * A part that cannot be measured is left out and the rest re-scaled to 100.
 *
 * <p>Savings rate and buffer are measured over the forecast window (income and spending
 * the plan and history project for the period): savings rate 20% or more earns full points;
 * a buffer of 3 or more months of spending earns full points.
 */
public class HealthScoreCalculator {

    public static final int BUDGET_MAX = 30;
    public static final int SAVINGS_MAX = 25;
    public static final int BUFFER_MAX = 25;
    public static final int GOALS_MAX = 20;
    static final double FULL_SAVINGS_RATE = 0.20;
    static final double FULL_BUFFER_MONTHS = 3.0;

    /**
     * @param status   budget status for the month, or null when no budget is set
     * @param forecast projected balances, or null when there is no plan
     * @param goals    savings goals (may be empty)
     */
    public HealthScore calculate(BudgetStatus status, Forecast forecast, List<SavingsGoal> goals) {
        List<ScoreComponent> parts = new ArrayList<>();
        List<String> excluded = new ArrayList<>();

        if (status == null || status.lines().isEmpty()) {
            excluded.add("Budget adherence: no budget limits set for this month");
        } else {
            parts.add(budget(status));
        }
        addForecastParts(forecast, parts, excluded);
        if (goals == null || goals.isEmpty()) {
            excluded.add("Goal progress: no savings goals yet");
        } else {
            parts.add(goalPart(goals));
        }

        if (parts.isEmpty()) {
            return new HealthScore(0, List.of(), excluded, "N/A", null);
        }
        int earned = parts.stream().mapToInt(ScoreComponent::points).sum();
        int available = parts.stream().mapToInt(ScoreComponent::maxPoints).sum();
        int total = (int) Math.round(100.0 * earned / available);
        return new HealthScore(total, parts, excluded, "", null);
    }

    /** The component with the most points still to gain, or null if every part is full. */
    public static ScoreComponent biggestGap(HealthScore score) {
        return score.components().stream().filter(c -> c.missing() > 0)
                .max(Comparator.comparingInt(ScoreComponent::missing)).orElse(null);
    }

    private ScoreComponent budget(BudgetStatus status) {
        int n = status.lines().size();
        double within = 0;
        int ok = 0;
        for (BudgetLineStatus line : status.lines()) {
            AlertSeverity level = line.level();
            if (level == AlertSeverity.INFO) {
                within += 1;
                ok++;
            } else if (level == AlertSeverity.WARNING) {
                within += 0.5;
            }
        }
        int points = (int) Math.round(BUDGET_MAX * within / n);
        return new ScoreComponent("Budget adherence", points, BUDGET_MAX,
                ok + " of " + n + " budget lines comfortably within limit");
    }

    private void addForecastParts(Forecast forecast, List<ScoreComponent> parts, List<String> excluded) {
        if (forecast == null) {
            excluded.add("Savings rate: no plan yet (set your balance first)");
            excluded.add("Emergency buffer: no plan yet (set your balance first)");
            return;
        }
        double income = 0;
        double outgoing = 0;
        for (DailyBalance d : forecast.days()) {
            income += d.income().amount().doubleValue();
            outgoing += d.outgoing().amount().doubleValue();
        }
        if (income <= 0) {
            excluded.add("Savings rate: no income in the period");
        } else {
            double rate = (income - outgoing) / income;
            int points = (int) Math.round(SAVINGS_MAX * clamp(rate / FULL_SAVINGS_RATE));
            parts.add(new ScoreComponent("Savings rate", points, SAVINGS_MAX,
                    String.format("saving %.0f%% of income (full points at %.0f%%)", rate * 100,
                            FULL_SAVINGS_RATE * 100)));
        }
        if (outgoing <= 0) {
            excluded.add("Emergency buffer: no spending to measure against");
        } else {
            double balance = forecast.days().get(0).balance().amount().doubleValue();
            double days = forecast.days().size() - 1;
            double monthlySpend = outgoing / Math.max(days, 1) * 30.0;
            double months = Math.max(balance, 0) / monthlySpend;
            int points = (int) Math.round(BUFFER_MAX * clamp(months / FULL_BUFFER_MONTHS));
            parts.add(new ScoreComponent("Emergency buffer", points, BUFFER_MAX,
                    String.format("balance covers %.1f months of spending (full points at %.0f)", months,
                            FULL_BUFFER_MONTHS)));
        }
    }

    private ScoreComponent goalPart(List<SavingsGoal> goals) {
        double sum = 0;
        int onTrack = 0;
        for (SavingsGoal g : goals) {
            GoalStatus s = g.status();
            if (s == GoalStatus.ON_TRACK || s == GoalStatus.ACHIEVED) {
                sum += 1;
                onTrack++;
            } else if (s == GoalStatus.AT_RISK) {
                sum += 0.5;
            }
        }
        int points = (int) Math.round(GOALS_MAX * sum / goals.size());
        return new ScoreComponent("Goal progress", points, GOALS_MAX,
                onTrack + " of " + goals.size() + " goals on track or achieved");
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
