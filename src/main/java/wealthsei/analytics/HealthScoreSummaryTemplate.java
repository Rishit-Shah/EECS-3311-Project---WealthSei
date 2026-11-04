package wealthsei.analytics;

import wealthsei.domain.HealthScore;
import wealthsei.domain.ScoreComponent;

/** Plain-language explanation of a health score; the fallback when no language model is available. */
public final class HealthScoreSummaryTemplate {

    private HealthScoreSummaryTemplate() {
    }

    public static String write(HealthScore score) {
        if (!score.isAvailable()) {
            return "N/A: there is not enough data to score yet. Set a budget, set your balance with "
                    + "'plan balance <amount>', or add a savings goal.";
        }
        StringBuilder sb = new StringBuilder("Your score is " + score.total() + " out of 100.");
        score.changeFromLastMonth().ifPresent(c ->
                sb.append(" That is ").append(c == 0 ? "unchanged" : Math.abs(c) + (c > 0 ? " up" : " down"))
                        .append(" from last month."));
        ScoreComponent gap = HealthScoreCalculator.biggestGap(score);
        if (gap == null) {
            sb.append(" Every measured part is at full points.");
        } else {
            sb.append(" The biggest room to improve is ").append(gap.name().toLowerCase()).append(" (")
                    .append(gap.points()).append(" of ").append(gap.maxPoints()).append(": ")
                    .append(gap.detail()).append(").");
        }
        if (!score.excluded().isEmpty()) {
            sb.append(" Parts left out (and the rest re-scaled): ").append(score.excluded().size()).append(".");
        }
        return sb.toString();
    }
}
