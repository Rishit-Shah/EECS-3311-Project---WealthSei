package wealthsei.agent;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import wealthsei.analytics.Scenario;
import wealthsei.analytics.ScenarioParser;
import wealthsei.analytics.ScenarioResult;
import wealthsei.analytics.ScenarioSimulator;
import wealthsei.llm.ParamSpec;
import wealthsei.llm.ParamType;
import wealthsei.llm.ToolArgs;
import wealthsei.llm.ToolSpec;

/** Runs a what-if over the next six months and returns baseline and scenario figures side by side. */
public class ScenarioTool implements Tool {

    public static final String NAME = "scenario";

    private final ScenarioSimulator simulator;
    private final Supplier<LocalDate> today;

    public ScenarioTool(ScenarioSimulator simulator, Supplier<LocalDate> today) {
        this.simulator = simulator;
        this.today = today;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(NAME,
                "Simulates a what-if over the next 6 months and compares it with the current plan (end balance, lowest balance, "
                        + "monthly savings, goal completion dates). Give at least one change, at most 5 in total.",
                List.of(new ParamSpec("cancel", ParamType.STRING,
                                "Recurring payments or bills to cancel, comma separated, e.g. Spotify,Netflix", false),
                        new ParamSpec("caps", ParamType.STRING,
                                "Monthly spending caps as category=amount, comma separated, e.g. dining=150,shopping=100", false),
                        new ParamSpec("income_increase", ParamType.NUMBER, "Extra income per month, e.g. 200", false),
                        new ParamSpec("one_time_expenses", ParamType.STRING,
                                "Single expenses as amount@YYYY-MM-DD, comma separated, e.g. 600@2026-12-01", false)));
    }

    @Override
    public ToolResult run(ToolArgs args) {
        Scenario scenario = new Scenario(UUID.randomUUID().toString().substring(0, 8), "What-if", ScenarioParser.parse(args));
        return ToolResult.ok(toData(simulator.simulate(scenario, today.get())));
    }

    static Map<String, Object> toData(ScenarioResult r) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "scenario");
        data.put("months", r.months());
        data.put("changes", new ArrayList<Object>(r.changes()));
        data.put("baselineEndBalance", r.baselineEndBalance().amount());
        data.put("scenarioEndBalance", r.scenarioEndBalance().amount());
        data.put("endBalanceChange", r.endBalanceDelta().amount());
        data.put("baselineLowestBalance", r.baselineLowest().amount());
        data.put("scenarioLowestBalance", r.scenarioLowest().amount());
        data.put("everydaySpendingPerMonthCounted", r.typicalMonthlySpending().amount());
        data.put("baselineMonthlySavings", r.baselineMonthlySavings().amount());
        data.put("scenarioMonthlySavings", r.scenarioMonthlySavings().amount());
        data.put("monthlySavingsChange", r.monthlySavingsDelta().amount());
        List<Object> goals = new ArrayList<>();
        for (ScenarioResult.GoalImpact g : r.goalImpacts()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("goal", g.goalName());
            item.put("baselineCompletion", g.baselineCompletion() == null ? "not reached" : g.baselineCompletion().toString());
            item.put("scenarioCompletion", g.scenarioCompletion() == null ? "not reached" : g.scenarioCompletion().toString());
            goals.add(item);
        }
        data.put("goals", goals);
        return data;
    }
}
