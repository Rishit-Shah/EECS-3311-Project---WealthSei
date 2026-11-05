package wealthsei.agent;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import wealthsei.domain.FeasibilityReport;
import wealthsei.domain.SavingsGoal;
import wealthsei.llm.ToolArgs;
import wealthsei.llm.ToolSpec;
import wealthsei.service.GoalService;

/** The user's savings goals: progress, status and what each needs per month. */
public class GoalTool implements Tool {

    private final GoalService goals;
    private final Supplier<LocalDate> today;

    public GoalTool(GoalService goals, Supplier<LocalDate> today) {
        this.goals = goals;
        this.today = today;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec("goals", "All savings goals with target, saved, deadline, status and the monthly amount each needs.",
                List.of());
    }

    @Override
    public ToolResult run(ToolArgs args) {
        List<Object> list = new ArrayList<>();
        for (SavingsGoal g : goals.goals()) {
            FeasibilityReport r = goals.feasibility(g.id(), today.get());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", g.name());
            item.put("target", g.target().amount());
            item.put("saved", g.saved().amount());
            item.put("deadline", g.deadline().toString());
            item.put("monthlyContribution", g.monthlyContribution().amount());
            item.put("status", g.status().name());
            item.put("requiredMonthly", r.requiredMonthly().amount());
            item.put("reachableOnTime", r.feasible());
            list.add(item);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "goals");
        data.put("count", list.size());
        data.put("goals", list);
        return ToolResult.ok(data);
    }
}
