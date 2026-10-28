package wealthsei.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The user's financial starting point and known commitments: balance, safety buffer,
 * recurring income, fixed bills and planned one-off expenses. Everything the forecast needs
 * that does not come from transactions.
 *
 * <p>Immutable. The {@code with...} methods return a changed copy.
 *
 * @param currentBalance  balance on {@code balanceAsOf} (may be negative if overdrawn)
 * @param balanceAsOf     the date {@code currentBalance} is correct for
 * @param safetyBuffer    money the user wants to always keep untouched (zero or more)
 * @param incomeSources   recurring income
 * @param fixedBills      recurring bills
 * @param plannedExpenses known one-off future expenses
 */
public record FinancialPlan(Money currentBalance, LocalDate balanceAsOf, Money safetyBuffer,
                            List<IncomeSource> incomeSources, List<FixedBill> fixedBills,
                            List<PlannedExpense> plannedExpenses) {

    public FinancialPlan {
        Objects.requireNonNull(currentBalance, "currentBalance");
        Objects.requireNonNull(balanceAsOf, "balanceAsOf");
        Objects.requireNonNull(safetyBuffer, "safetyBuffer");
        if (safetyBuffer.isNegative()) {
            throw new IllegalArgumentException("Safety buffer must not be negative, was " + safetyBuffer);
        }
        incomeSources = List.copyOf(Objects.requireNonNull(incomeSources, "incomeSources"));
        fixedBills = List.copyOf(Objects.requireNonNull(fixedBills, "fixedBills"));
        plannedExpenses = List.copyOf(Objects.requireNonNull(plannedExpenses, "plannedExpenses"));
    }

    /** A plan with just a balance: no buffer, income, bills or planned expenses yet. */
    public static FinancialPlan starting(Money balance, LocalDate asOf) {
        return new FinancialPlan(balance, asOf, Money.ZERO, List.of(), List.of(), List.of());
    }

    public FinancialPlan withBalance(Money balance, LocalDate asOf) {
        return new FinancialPlan(balance, asOf, safetyBuffer, incomeSources, fixedBills, plannedExpenses);
    }

    public FinancialPlan withSafetyBuffer(Money buffer) {
        return new FinancialPlan(currentBalance, balanceAsOf, buffer, incomeSources, fixedBills, plannedExpenses);
    }

    public FinancialPlan withIncomeSource(IncomeSource income) {
        List<IncomeSource> list = new ArrayList<>(incomeSources);
        list.add(Objects.requireNonNull(income, "income"));
        return new FinancialPlan(currentBalance, balanceAsOf, safetyBuffer, list, fixedBills, plannedExpenses);
    }

    public FinancialPlan withFixedBill(FixedBill bill) {
        List<FixedBill> list = new ArrayList<>(fixedBills);
        list.add(Objects.requireNonNull(bill, "bill"));
        return new FinancialPlan(currentBalance, balanceAsOf, safetyBuffer, incomeSources, list, plannedExpenses);
    }

    public FinancialPlan withPlannedExpense(PlannedExpense expense) {
        List<PlannedExpense> list = new ArrayList<>(plannedExpenses);
        list.add(Objects.requireNonNull(expense, "expense"));
        return new FinancialPlan(currentBalance, balanceAsOf, safetyBuffer, incomeSources, fixedBills, list);
    }

    /** Average monthly income across all income sources. */
    public Money monthlyIncome() {
        return incomeSources.stream().map(IncomeSource::monthlyEquivalent)
                .reduce(Money.ZERO, Money::add);
    }

    /** Average monthly cost of all fixed bills. */
    public Money monthlyFixedBills() {
        return fixedBills.stream().map(FixedBill::monthlyEquivalent)
                .reduce(Money.ZERO, Money::add);
    }
}
