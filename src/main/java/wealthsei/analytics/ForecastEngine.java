package wealthsei.analytics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import wealthsei.domain.DailyBalance;
import wealthsei.domain.FixedBill;
import wealthsei.domain.Forecast;
import wealthsei.domain.IncomeSource;
import wealthsei.domain.Money;
import wealthsei.domain.PlannedExpense;
import wealthsei.domain.RecurringPayment;

/**
 * Projects the balance forward day by day. No AI: it adds the income, bills, recurring payments
 * and planned expenses that fall on each day, plus the user's typical everyday spending when the
 * inputs carry it. A recurring payment that matches a bill in the
 * plan (same name) is counted once, as the bill.
 */
public class ForecastEngine {

    /**
     * @param days how many days ahead; the result has {@code days + 1} entries, today first
     * @throws IllegalArgumentException if days is less than 1
     */
    public Forecast project(ForecastInputs inputs, int days) {
        Objects.requireNonNull(inputs, "inputs");
        if (days < 1) {
            throw new IllegalArgumentException("Days must be at least 1, was " + days);
        }
        LocalDate start = inputs.today();
        LocalDate first = start.plusDays(1);
        LocalDate last = start.plusDays(days);
        Map<LocalDate, Money> income = new HashMap<>();
        Map<LocalDate, Money> outgoing = new HashMap<>();
        for (IncomeSource s : inputs.incomeSources()) {
            for (LocalDate d : s.frequency().occurrences(s.nextPayDate(), first, last)) {
                income.merge(d, s.amount(), Money::add);
            }
        }
        for (FixedBill b : inputs.fixedBills()) {
            for (LocalDate d : b.frequency().occurrences(b.nextDueDate(), first, last)) {
                outgoing.merge(d, b.amount(), Money::add);
            }
        }
        for (RecurringPayment r : inputs.recurring()) {
            if (isCoveredByBill(r, inputs.fixedBills())) {
                continue;
            }
            for (LocalDate d : r.frequency().occurrences(r.nextDue(), first, last)) {
                outgoing.merge(d, r.amount(), Money::add);
            }
        }
        for (PlannedExpense e : inputs.plannedExpenses()) {
            if (!e.date().isBefore(first) && !e.date().isAfter(last)) {
                outgoing.merge(e.date(), e.amount(), Money::add);
            }
        }
        Money everyday = inputs.typicalMonthlySpending().multiply(12).divide(365);
        List<DailyBalance> result = new ArrayList<>();
        Money balance = inputs.openingBalance();
        result.add(new DailyBalance(start, balance, Money.ZERO, Money.ZERO));
        for (int i = 1; i <= days; i++) {
            LocalDate date = start.plusDays(i);
            Money in = income.getOrDefault(date, Money.ZERO);
            Money out = outgoing.getOrDefault(date, Money.ZERO).add(everyday);
            balance = balance.add(in).subtract(out);
            result.add(new DailyBalance(date, balance, in, out));
        }
        return new Forecast(result);
    }

    /** Income minus bills, recurring payments and (when counted) everyday spending, averaged per month. Negative when spending exceeds income. */
    public Money monthlySurplus(ForecastInputs inputs) {
        Objects.requireNonNull(inputs, "inputs");
        Money surplus = Money.ZERO;
        for (IncomeSource s : inputs.incomeSources()) {
            surplus = surplus.add(s.monthlyEquivalent());
        }
        for (FixedBill b : inputs.fixedBills()) {
            surplus = surplus.subtract(b.monthlyEquivalent());
        }
        for (RecurringPayment r : inputs.recurring()) {
            if (!isCoveredByBill(r, inputs.fixedBills())) {
                surplus = surplus.subtract(r.monthlyCost());
            }
        }
        return surplus.subtract(inputs.typicalMonthlySpending());
    }

    private static boolean isCoveredByBill(RecurringPayment payment, List<FixedBill> bills) {
        String merchant = payment.merchant().trim().toLowerCase(Locale.ROOT);
        for (FixedBill bill : bills) {
            String name = bill.name().trim().toLowerCase(Locale.ROOT);
            if (!name.isEmpty() && (name.contains(merchant) || merchant.contains(name))) {
                return true;
            }
        }
        return false;
    }
}
