package wealthsei.analytics;

import java.time.YearMonth;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import wealthsei.domain.Category;
import wealthsei.domain.Money;
import wealthsei.domain.Transaction;
import wealthsei.service.ValidationException;

/**
 * Limits what is spent in a category each month. The saving is what the category costs on average
 * (over up to the last 3 full months of history, or this month so far if there is no earlier month)
 * minus the cap, and counts as extra money each month from the first of next month.
 */
public class CapCategoryChange implements ScenarioChange {

    private static final int MONTHS_LOOKED_AT = 3;

    private final Category category;
    private final Money monthlyCap;

    public CapCategoryChange(Category category, Money monthlyCap) {
        this.category = Objects.requireNonNull(category, "category");
        this.monthlyCap = Objects.requireNonNull(monthlyCap, "monthlyCap");
        if (monthlyCap.isNegative()) {
            throw new ValidationException("The monthly cap cannot be negative");
        }
    }

    @Override
    public ForecastInputs applyTo(ForecastInputs inputs) {
        Money saving = Money.max(Money.ZERO, averageMonthlySpend(inputs).subtract(monthlyCap));
        if (saving.isZero()) {
            return inputs.deepCopy();
        }
        return IncomeChange.addMonthlyIncome(inputs, "What-if saving on " + category.name(), saving);
    }

    /** The category's average monthly spending, from history (zero if there is none). */
    Money averageMonthlySpend(ForecastInputs inputs) {
        YearMonth current = YearMonth.from(inputs.today());
        YearMonth first = inputs.transactions().stream().map(t -> YearMonth.from(t.date())).min(YearMonth::compareTo)
                .orElse(current);
        YearMonth from = first.isBefore(current.minusMonths(MONTHS_LOOKED_AT)) ? current.minusMonths(MONTHS_LOOKED_AT) : first;
        YearMonth to = current.minusMonths(1);
        if (from.isAfter(to)) {
            from = current;
            to = current;
        }
        Set<YearMonth> months = new HashSet<>();
        for (YearMonth m = from; !m.isAfter(to); m = m.plusMonths(1)) {
            months.add(m);
        }
        Money total = Money.ZERO;
        for (Transaction t : inputs.transactions()) {
            if (t.isExpense() && t.category().equals(category) && months.contains(YearMonth.from(t.date()))
                    && !t.date().isAfter(inputs.today())) {
                total = total.add(t.amount());
            }
        }
        return total.divide(months.size());
    }

    @Override
    public String describe() {
        return "Cap " + category.name() + " at " + monthlyCap + " a month";
    }

}
