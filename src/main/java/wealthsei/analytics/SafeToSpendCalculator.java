package wealthsei.analytics;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import wealthsei.domain.DailyBalance;
import wealthsei.domain.Forecast;
import wealthsei.domain.Money;
import wealthsei.domain.SafeToSpendResult;
import wealthsei.domain.SavingsGoal;

/**
 * Applies the Safe-to-Spend formula:
 * (balance + expected income - upcoming bills - goal reserve - safety buffer) / days left,
 * where "expected income" and "upcoming bills" are those due after today and up to the end of
 * the month. A negative result is shown as zero with the shortfall reported separately.
 */
public class SafeToSpendCalculator {

    /**
     * @param forecast the projection; its first day is today
     * @param goals    savings goals; each goal not yet achieved reserves its monthly contribution
     * @param buffer   money to keep untouched
     */
    public SafeToSpendResult calculate(Forecast forecast, List<SavingsGoal> goals, Money buffer) {
        Objects.requireNonNull(forecast, "forecast");
        Objects.requireNonNull(goals, "goals");
        Objects.requireNonNull(buffer, "buffer");
        DailyBalance today = forecast.days().get(0);
        LocalDate monthEnd = YearMonth.from(today.date()).atEndOfMonth();
        Money income = Money.ZERO;
        Money bills = Money.ZERO;
        for (DailyBalance day : forecast.days().subList(1, forecast.days().size())) {
            if (day.date().isAfter(monthEnd)) {
                break;
            }
            income = income.add(day.income());
            bills = bills.add(day.outgoing());
        }
        Money reserve = Money.ZERO;
        for (SavingsGoal goal : goals) {
            if (!goal.isAchieved()) {
                reserve = reserve.add(goal.monthlyContribution());
            }
        }
        Money available = today.balance().add(income).subtract(bills).subtract(reserve).subtract(buffer);
        Money remaining = Money.max(Money.ZERO, available);
        Money shortfall = available.isNegative() ? available.negate() : Money.ZERO;
        int daysLeft = (int) ChronoUnit.DAYS.between(today.date(), monthEnd) + 1;
        return new SafeToSpendResult(remaining.divide(daysLeft), remaining, bills, reserve, "",
                today.balance(), income, buffer, daysLeft, shortfall);
    }
}
