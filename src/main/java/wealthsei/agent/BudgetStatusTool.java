package wealthsei.agent;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import wealthsei.domain.BudgetLineStatus;
import wealthsei.domain.BudgetStatus;
import wealthsei.llm.ParamSpec;
import wealthsei.llm.ParamType;
import wealthsei.llm.ToolArgs;
import wealthsei.llm.ToolSpec;
import wealthsei.service.BudgetService;

/** Spending against the monthly budget, per category. */
public class BudgetStatusTool implements Tool {

    private final BudgetService budgets;
    private final Supplier<LocalDate> today;

    public BudgetStatusTool(BudgetService budgets, Supplier<LocalDate> today) {
        this.budgets = budgets;
        this.today = today;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec("budget_status",
                "Budget limits and spending so far for one month, per category (spent, limit, remaining, percent used).",
                List.of(new ParamSpec("month", ParamType.STRING, "Month as YYYY-MM; defaults to the current month", false)));
    }

    @Override
    public ToolResult run(ToolArgs args) {
        YearMonth month;
        try {
            month = args.text("month").map(YearMonth::parse).orElse(YearMonth.from(today.get()));
        } catch (java.time.format.DateTimeParseException e) {
            return ToolResult.failure("The month must look like 2026-11");
        }
        BudgetStatus status;
        try {
            status = budgets.getStatus(month);
        } catch (wealthsei.service.ValidationException e) {
            // No budget for the month is a normal situation, not a tool failure.
            Map<String, Object> none = new LinkedHashMap<>();
            none.put("type", "budget_status");
            none.put("month", month.toString());
            none.put("budgetSet", false);
            none.put("note", "No budget has been set for " + month + ", so there are no limits to check.");
            return ToolResult.ok(none);
        }
        List<Object> lines = new ArrayList<>();
        for (BudgetLineStatus l : status.lines()) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("category", l.category().id());
            line.put("categoryName", l.category().name());
            line.put("spent", l.spent().amount());
            line.put("limit", l.limit().amount());
            line.put("remaining", l.remaining().amount());
            line.put("percentUsed", Math.round(l.ratio() * 100));
            line.put("level", l.level().name());
            lines.add(line);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "budget_status");
        data.put("month", month.toString());
        data.put("budgetSet", true);
        data.put("lines", lines);
        data.put("totalSpent", status.totalSpent().amount());
        data.put("totalLimit", status.totalLimit().amount());
        return ToolResult.ok(data);
    }
}
