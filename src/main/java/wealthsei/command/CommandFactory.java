package wealthsei.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;
import wealthsei.agent.ParamSet;
import wealthsei.agent.ProposalDraft;
import wealthsei.analytics.AnalyticsService;
import wealthsei.domain.Categories;
import wealthsei.domain.Category;
import wealthsei.domain.Money;
import wealthsei.domain.RecurringPayment;
import wealthsei.service.BudgetService;
import wealthsei.service.GoalService;
import wealthsei.service.ValidationException;

/**
 * Builds the command that goes with a proposal draft (Factory pattern). It checks the details when
 * the proposal is created, so a draft that could never be applied is refused up front.
 *
 * <p>Parameters by type: BUDGET_ADJUSTMENT {@code month, category, limit}; GOAL_ADJUSTMENT
 * {@code goalId, monthly}; FLAG_SUBSCRIPTION {@code merchant}; RECATEGORIZE {@code txId, category}.
 */
public class CommandFactory {

    private final BudgetService budgets;
    private final GoalService goals;
    private final AnalyticsService analytics;
    private final CategoryCorrector corrector;
    private final Supplier<LocalDate> today;

    public CommandFactory(BudgetService budgets, GoalService goals, AnalyticsService analytics,
                          CategoryCorrector corrector, Supplier<LocalDate> today) {
        this.budgets = Objects.requireNonNull(budgets, "budgets");
        this.goals = Objects.requireNonNull(goals, "goals");
        this.analytics = Objects.requireNonNull(analytics, "analytics");
        this.corrector = Objects.requireNonNull(corrector, "corrector");
        this.today = Objects.requireNonNull(today, "today");
    }

    /** @throws ValidationException if the type is unknown or the details are missing or wrong */
    public Command create(ProposalDraft draft) {
        if (draft.type() == null) {
            throw new ValidationException("'" + draft.title() + "' is only a suggestion, not a change that can be applied");
        }
        ParamSet p = draft.params();
        return switch (draft.type()) {
            case BUDGET_ADJUSTMENT -> budgetAdjustment(p);
            case GOAL_ADJUSTMENT -> goalAdjustment(p);
            case FLAG_SUBSCRIPTION -> new FlagSubscriptionCommand(analytics, today, resolveMerchant(p.require("merchant")));
            case RECATEGORIZE -> corrector.newCommand(p.require("txId"), category(p.require("category")));
        };
    }

    private Command budgetAdjustment(ParamSet p) {
        YearMonth month;
        try {
            month = YearMonth.parse(p.require("month"));
        } catch (DateTimeParseException e) {
            throw new ValidationException("The month must look like 2026-11");
        }
        Category category = category(p.require("category"));
        Money limit = money(p.require("limit"));
        if (!limit.isPositive()) {
            throw new ValidationException("A limit must be more than zero");
        }
        if (budgets.limitOf(month, category).isEmpty()) {
            throw new ValidationException("There is no " + category.name() + " limit for " + month
                    + " to change; set one with 'budget set' first");
        }
        return new SetBudgetLimitCommand(budgets, month, category, limit);
    }

    private Command goalAdjustment(ParamSet p) {
        String goalId = p.require("goalId");
        goals.find(goalId);
        Money monthly = money(p.require("monthly"));
        if (monthly.isNegative()) {
            throw new ValidationException("The monthly amount cannot be negative");
        }
        return new UpdateGoalContributionCommand(goals, today, goalId, monthly);
    }

    /** Exact name (ignoring case), or a part of the name when it picks exactly one recurring payment. */
    private String resolveMerchant(String typed) {
        List<RecurringPayment> found = analytics.detectRecurring(today.get());
        for (RecurringPayment r : found) {
            if (r.merchant().equalsIgnoreCase(typed)) {
                return r.merchant();
            }
        }
        List<String> partial = found.stream().map(RecurringPayment::merchant)
                .filter(m -> m.toLowerCase(Locale.ROOT).contains(typed.toLowerCase(Locale.ROOT))).distinct().toList();
        if (partial.size() == 1) {
            return partial.get(0);
        }
        throw new ValidationException(partial.isEmpty()
                ? "No recurring payment found for '" + typed + "'"
                : "'" + typed + "' matches several recurring payments: " + String.join(", ", partial));
    }

    private static Category category(String text) {
        return Categories.find(text).orElseThrow(() -> new ValidationException("Unknown category '" + text + "'"));
    }

    private static Money money(String text) {
        try {
            return Money.of(new BigDecimal(text.trim().replace("$", "")));
        } catch (NumberFormatException e) {
            throw new ValidationException("'" + text + "' is not an amount");
        }
    }
}
