package wealthsei.analytics;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import wealthsei.domain.DailyBalance;
import wealthsei.domain.Forecast;
import wealthsei.domain.GoalStatus;
import wealthsei.domain.Money;
import wealthsei.domain.SafeToSpendResult;
import wealthsei.domain.SavingsGoal;

class SafeToSpendCalculatorTest {

    private final SafeToSpendCalculator calculator = new SafeToSpendCalculator();

    /** A forecast starting on {@code start} with the given balance and one inflow/outflow per listed day. */
    private static Forecast forecast(String start, String balance, int days, String[][] flows) {
        LocalDate first = LocalDate.parse(start);
        List<DailyBalance> list = new ArrayList<>();
        Money running = Money.of(balance);
        list.add(new DailyBalance(first, running, Money.ZERO, Money.ZERO));
        for (int i = 1; i <= days; i++) {
            LocalDate d = first.plusDays(i);
            Money in = Money.ZERO;
            Money out = Money.ZERO;
            for (String[] f : flows) {
                if (LocalDate.parse(f[0]).equals(d)) {
                    in = in.add(Money.of(f[1]));
                    out = out.add(Money.of(f[2]));
                }
            }
            running = running.add(in).subtract(out);
            list.add(new DailyBalance(d, running, in, out));
        }
        return new Forecast(list);
    }

    private static SavingsGoal goal(String monthly, GoalStatus status) {
        return new SavingsGoal("g", "Goal", Money.of("1000"), LocalDate.of(2027, 6, 1), Money.ZERO, Money.of(monthly), status);
    }

    @Test
    void appliesTheStage1Formula() {
        // Nov 4 -> Nov 30 is 27 days including today
        Forecast f = forecast("2026-11-04", "1500", 30, new String[][]{
                {"2026-11-15", "2000", "0"}, {"2026-11-20", "0", "900"}, {"2026-12-01", "5000", "0"}});
        SafeToSpendResult r = calculator.calculate(f, List.of(goal("100", GoalStatus.ON_TRACK)), Money.of("200"));
        assertEquals(27, r.daysLeft());
        assertEquals(Money.of("2000"), r.expectedIncome());   // Dec 1 income is next month, ignored
        assertEquals(Money.of("900"), r.upcomingBills());
        assertEquals(Money.of("100"), r.goalReserve());
        // 1500 + 2000 - 900 - 100 - 200 = 2300
        assertEquals(Money.of("2300"), r.remainingThisMonth());
        assertEquals(Money.of("2300").divide(27), r.dailyAllowance());
        assertEquals(Money.ZERO, r.overCommittedBy());
        assertFalse(r.isOverCommitted());
    }

    @Test
    void aNegativeResultShowsZeroAndTheShortfall() {
        Forecast f = forecast("2026-11-04", "300", 30, new String[][]{{"2026-11-10", "0", "800"}});
        SafeToSpendResult r = calculator.calculate(f, List.of(), Money.of("200"));
        assertEquals(Money.ZERO, r.dailyAllowance());
        assertEquals(Money.ZERO, r.remainingThisMonth());
        assertEquals(Money.of("700"), r.overCommittedBy());   // 300 - 800 - 200 = -700
        assertTrue(r.isOverCommitted());
    }

    @Test
    void achievedGoalsReserveNothing() {
        Forecast f = forecast("2026-11-04", "1000", 30, new String[][]{});
        SafeToSpendResult r = calculator.calculate(f, List.of(goal("100", GoalStatus.ACHIEVED),
                goal("40", GoalStatus.BEHIND), goal("10", GoalStatus.AT_RISK)), Money.ZERO);
        assertEquals(Money.of("50"), r.goalReserve());
        assertEquals(Money.of("950"), r.remainingThisMonth());
    }

    @Test
    void onTheLastDayOfTheMonthTheWholeRemainderIsTheAllowance() {
        Forecast f = forecast("2026-11-30", "500", 30, new String[][]{});
        SafeToSpendResult r = calculator.calculate(f, List.of(), Money.of("200"));
        assertEquals(1, r.daysLeft());
        assertEquals(Money.of("300"), r.dailyAllowance());
    }

    @Test
    void theFirstOfALongMonthStillFitsInA30DayForecast() {
        Forecast f = forecast("2026-12-01", "3200", 30, new String[][]{{"2026-12-31", "0", "100"}});
        SafeToSpendResult r = calculator.calculate(f, List.of(), Money.ZERO);
        assertEquals(31, r.daysLeft());
        assertEquals(Money.of("100"), r.dailyAllowance());   // (3200 - 100) / 31
    }
}
