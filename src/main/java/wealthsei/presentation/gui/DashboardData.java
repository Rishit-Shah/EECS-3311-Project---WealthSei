package wealthsei.presentation.gui;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import wealthsei.command.Proposal;
import wealthsei.domain.BudgetStatus;
import wealthsei.domain.Category;
import wealthsei.domain.Forecast;
import wealthsei.domain.HealthScore;
import wealthsei.domain.Insight;
import wealthsei.domain.Money;
import wealthsei.domain.SafeToSpendResult;
import wealthsei.domain.SavingsGoal;
import wealthsei.domain.Transaction;
import wealthsei.domain.TxFilter;
import wealthsei.facade.WealthSeiFacade;
import wealthsei.service.ValidationException;

/**
 * Everything the dashboard shows, gathered once from the facade. Parts that cannot be worked out yet
 * (no plan, no budget) are null or empty, and the screen shows a hint instead.
 */
public record DashboardData(LocalDate today, YearMonth month, Money spentThisMonth, Money receivedThisMonth,
                            Map<Category, Money> spendingByCategory, BudgetStatus budget, SafeToSpendResult safe,
                            Forecast forecast, HealthScore score, List<SavingsGoal> goals, List<Proposal> pending,
                            List<Insight> habits, List<Transaction> recent, int transactionCount,
                            wealthsei.analytics.RecurringReport recurring) {

    public static DashboardData load(WealthSeiFacade facade, LocalDate today) {
        return load(facade, today, null);
    }

    /** @param chosen the month the person picked, or null to use this month (or the latest month with data) */
    public static DashboardData load(WealthSeiFacade facade, LocalDate today, YearMonth chosen) {
        YearMonth month = chosen != null ? chosen : YearMonth.from(today);
        List<Transaction> all = facade.listTransactions(TxFilter.all());
        // Show the latest month that has data when the current one is still empty.
        YearMonth shown = month;
        if (chosen == null && all.stream().noneMatch(t -> YearMonth.from(t.date()).equals(month))) {
            shown = all.stream().map(t -> YearMonth.from(t.date())).max(Comparator.naturalOrder()).orElse(month);
        }
        final YearMonth showMonth = shown;
        List<Transaction> inMonth = all.stream().filter(t -> YearMonth.from(t.date()).equals(showMonth)).toList();
        Money spent = Money.ZERO;
        Money received = Money.ZERO;
        Map<Category, Money> byCat = new LinkedHashMap<>();
        for (Transaction t : inMonth) {
            if (t.isExpense()) {
                spent = spent.add(t.amount());
                byCat.merge(t.category(), t.amount(), Money::add);
            } else {
                received = received.add(t.amount());
            }
        }
        Map<Category, Money> sorted = new LinkedHashMap<>();
        byCat.entrySet().stream().sorted(Map.Entry.<Category, Money>comparingByValue().reversed())
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));

        BudgetStatus budget = null;
        try {
            budget = facade.getBudgetStatus(showMonth);
        } catch (ValidationException e) {
            // no budget for this month
        }
        SafeToSpendResult safe = null;
        Forecast forecast = null;
        try {
            safe = facade.getSafeToSpend(today);
            forecast = facade.getForecast(30, today);
        } catch (ValidationException e) {
            // no plan yet
        }
        HealthScore score = null;
        try {
            HealthScore s = facade.getHealthScore(showMonth, today);
            score = s.isAvailable() ? s : null;
        } catch (ValidationException e) {
            // not enough information
        }
        List<Insight> habits = List.of();
        try {
            habits = facade.getHabits(showMonth);
        } catch (ValidationException e) {
            // not enough history
        }
        wealthsei.analytics.RecurringReport recurring = null;
        try {
            recurring = facade.recurringPayments(today);
        } catch (ValidationException e) {
            // not enough history to find repeating payments
        }
        List<Transaction> recent = new ArrayList<>(all);
        recent.sort(Comparator.comparing(Transaction::date).reversed());
        return new DashboardData(today, showMonth, spent, received, sorted, budget, safe, forecast, score,
                facade.getGoals(), facade.listPendingProposals(today), habits,
                recent.stream().limit(6).toList(), all.size(), recurring);
    }

    public boolean hasData() {
        return transactionCount > 0;
    }
}
