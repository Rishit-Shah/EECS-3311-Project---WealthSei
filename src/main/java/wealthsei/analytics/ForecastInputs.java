package wealthsei.analytics;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import wealthsei.domain.FixedBill;
import wealthsei.domain.IncomeSource;
import wealthsei.domain.Money;
import wealthsei.domain.PlannedExpense;
import wealthsei.domain.RecurringPayment;
import wealthsei.domain.Transaction;

/**
 * Everything the {@link ForecastEngine} needs. It can be copied ({@link #deepCopy}) so a
 * what-if scenario can change its own copy without touching the real data (Prototype pattern).
 *
 * @param today          the day the forecast starts from
 * @param openingBalance the balance today
 * @param transactions   the stored history
 * @param recurring      payments found in the history
 * @param incomeSources  income from the user's plan
 * @param fixedBills     bills from the user's plan
 * @param plannedExpenses one-off planned expenses from the user's plan
 * @param typicalMonthlySpending everyday spending not covered by bills or recurring payments (zero when not counted)
 */
public record ForecastInputs(LocalDate today, Money openingBalance, List<Transaction> transactions,
                             List<RecurringPayment> recurring, List<IncomeSource> incomeSources,
                             List<FixedBill> fixedBills, List<PlannedExpense> plannedExpenses,
                             Money typicalMonthlySpending) {

    /** Without everyday spending (the original form). */
    public ForecastInputs(LocalDate today, Money openingBalance, List<Transaction> transactions,
                          List<RecurringPayment> recurring, List<IncomeSource> incomeSources,
                          List<FixedBill> fixedBills, List<PlannedExpense> plannedExpenses) {
        this(today, openingBalance, transactions, recurring, incomeSources, fixedBills, plannedExpenses, Money.ZERO);
    }

    public ForecastInputs {
        Objects.requireNonNull(today, "today");
        Objects.requireNonNull(openingBalance, "openingBalance");
        transactions = List.copyOf(Objects.requireNonNull(transactions, "transactions"));
        recurring = List.copyOf(Objects.requireNonNull(recurring, "recurring"));
        incomeSources = List.copyOf(Objects.requireNonNull(incomeSources, "incomeSources"));
        fixedBills = List.copyOf(Objects.requireNonNull(fixedBills, "fixedBills"));
        plannedExpenses = List.copyOf(Objects.requireNonNull(plannedExpenses, "plannedExpenses"));
        Objects.requireNonNull(typicalMonthlySpending, "typicalMonthlySpending");
    }

    /** A separate copy that can be changed without affecting this one. */
    public ForecastInputs deepCopy() {
        return new ForecastInputs(today, openingBalance, transactions, recurring, incomeSources, fixedBills,
                plannedExpenses, typicalMonthlySpending);
    }

    public ForecastInputs withOpeningBalance(Money balance) {
        return new ForecastInputs(today, balance, transactions, recurring, incomeSources, fixedBills,
                plannedExpenses, typicalMonthlySpending);
    }

    public ForecastInputs withPlannedExpenses(List<PlannedExpense> expenses) {
        return new ForecastInputs(today, openingBalance, transactions, recurring, incomeSources, fixedBills,
                expenses, typicalMonthlySpending);
    }

    public ForecastInputs withTypicalMonthlySpending(Money spending) {
        return new ForecastInputs(today, openingBalance, transactions, recurring, incomeSources, fixedBills,
                plannedExpenses, spending);
    }
}
