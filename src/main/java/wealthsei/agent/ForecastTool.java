package wealthsei.agent;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import wealthsei.analytics.AnalyticsService;
import wealthsei.domain.DailyBalance;
import wealthsei.domain.Forecast;
import wealthsei.domain.Money;
import wealthsei.domain.PurchaseImpact;
import wealthsei.domain.SafeToSpendResult;
import wealthsei.llm.ParamSpec;
import wealthsei.llm.ParamType;
import wealthsei.llm.ToolArgs;
import wealthsei.llm.ToolSpec;

/**
 * The balance forecast. With a price it works out what a purchase would do (lowest balance and
 * daily allowance with and without it, and the verdict the fixed rules give); without one it
 * summarises the next days.
 */
public class ForecastTool implements Tool {

    public static final String NAME = "forecast";

    private final AnalyticsService analytics;
    private final Supplier<LocalDate> today;

    public ForecastTool(AnalyticsService analytics, Supplier<LocalDate> today) {
        this.analytics = analytics;
        this.today = today;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(NAME,
                "Projected balance and safe-to-spend. Give 'price' (and optionally 'item' and 'date') to see what a purchase would do; "
                        + "otherwise it summarises the next 'days' days.",
                List.of(new ParamSpec("days", ParamType.INTEGER, "How many days ahead (default 30)", false),
                        new ParamSpec("item", ParamType.STRING, "What would be bought", false),
                        new ParamSpec("price", ParamType.NUMBER, "Purchase price", false),
                        new ParamSpec("date", ParamType.STRING, "Purchase date as YYYY-MM-DD (default today)", false)));
    }

    @Override
    public ToolResult run(ToolArgs args) {
        LocalDate now = today.get();
        if (args.has("price")) {
            LocalDate date = parseDate(args.text("date").orElse(now.toString()));
            PurchaseImpact i = analytics.purchaseImpact(args.text("item").orElse("purchase"),
                    Money.of(args.decimal("price").orElseThrow()), date, now);
            return ToolResult.ok(impactData(i));
        }
        int days = args.integer("days").orElse(AnalyticsService.FORECAST_DAYS);
        if (days < 1 || days > 365) {
            return ToolResult.failure("days must be between 1 and 365");
        }
        Forecast forecast = analytics.forecast(days, now);
        SafeToSpendResult safe = analytics.safeToSpend(now);
        DailyBalance low = forecast.lowest();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "forecast");
        data.put("days", days);
        data.put("startBalance", forecast.days().get(0).balance().amount());
        data.put("lowestBalance", low.balance().amount());
        data.put("lowestBalanceDate", low.date().toString());
        data.put("endBalance", forecast.days().get(forecast.days().size() - 1).balance().amount());
        data.put("safeToSpendPerDay", safe.dailyAllowance().amount());
        data.put("remainingThisMonth", safe.remainingThisMonth().amount());
        data.put("upcomingBills", safe.upcomingBills().amount());
        data.put("safetyBuffer", safe.safetyBuffer().amount());
        return ToolResult.ok(data);
    }

    static Map<String, Object> impactData(PurchaseImpact i) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "purchase_impact");
        data.put("item", i.description());
        data.put("price", i.price().amount());
        data.put("date", i.date().toString());
        data.put("lowestBalanceWithout", i.lowestBefore().balance().amount());
        data.put("lowestBalanceWith", i.lowestAfter().balance().amount());
        data.put("lowestBalanceWithDate", i.lowestAfter().date().toString());
        data.put("safeToSpendPerDayWithout", i.allowanceBefore().amount());
        data.put("safeToSpendPerDayWith", i.allowanceAfter().amount());
        data.put("safetyBuffer", i.safetyBuffer().amount());
        data.put("goalsNeedingHelp", i.goalsNeedingHelp());
        data.put("suggestedVerdict", i.suggested().label());
        data.put("reason", i.reason());
        return data;
    }

    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text);
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("The date must look like 2026-11-20");
        }
    }
}
