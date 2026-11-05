package wealthsei.agent;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import wealthsei.analytics.AnalyticsService;
import wealthsei.domain.BudgetStatus;
import wealthsei.domain.HealthScore;
import wealthsei.domain.Insight;
import wealthsei.domain.ScoreComponent;
import wealthsei.llm.ParamSpec;
import wealthsei.llm.ParamType;
import wealthsei.llm.ToolArgs;
import wealthsei.llm.ToolSpec;
import wealthsei.service.BudgetService;

/** Spending habits found in a month and the financial health score with its parts. */
public class InsightTool implements Tool {

    public static final String NAME = "insights";

    private final AnalyticsService analytics;
    private final BudgetService budgets;
    private final Supplier<LocalDate> today;

    public InsightTool(AnalyticsService analytics, BudgetService budgets, Supplier<LocalDate> today) {
        this.analytics = analytics;
        this.budgets = budgets;
        this.today = today;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(NAME,
                "Spending habits found in one month (weekend spikes, small purchases that add up, unusual expenses) "
                        + "and the financial health score out of 100 with what it is made of.",
                List.of(new ParamSpec("month", ParamType.STRING, "Month as YYYY-MM; defaults to the current month", false)));
    }

    @Override
    public ToolResult run(ToolArgs args) {
        YearMonth month;
        try {
            month = args.text("month").map(YearMonth::parse).orElse(YearMonth.from(today.get()));
        } catch (java.time.format.DateTimeParseException e) {
            return ToolResult.failure("The month must look like 2026-10");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "insights");
        data.put("month", month.toString());
        List<Object> habits = new ArrayList<>();
        try {
            for (Insight i : analytics.habits(month)) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("habit", i.title());
                item.put("severity", i.severity().name());
                item.put("evidence", i.evidence());
                habits.add(item);
            }
            data.put("habits", habits);
        } catch (wealthsei.service.ValidationException e) {
            data.put("habitsNote", e.getMessage());
        }
        BudgetStatus status = budgets.hasBudget(month) ? budgets.getStatus(month) : null;
        HealthScore score = analytics.healthScore(status, today.get());
        if (score.isAvailable()) {
            data.put("healthScore", score.total());
            List<Object> parts = new ArrayList<>();
            for (ScoreComponent c : score.components()) {
                Map<String, Object> part = new LinkedHashMap<>();
                part.put("name", c.name());
                part.put("points", c.points());
                part.put("outOf", c.maxPoints());
                part.put("detail", c.detail());
                parts.add(part);
            }
            data.put("scoreParts", parts);
        } else {
            data.put("healthScoreNote", "Not enough information for a score yet (no budget, plan or goals).");
        }
        return ToolResult.ok(data);
    }
}
