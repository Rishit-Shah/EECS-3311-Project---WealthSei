package wealthsei.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ForecastDomainTest {

    private static DailyBalance day(String date, String balance) {
        return new DailyBalance(LocalDate.parse(date), Money.of(balance), Money.ZERO, Money.ZERO);
    }

    @Test
    void lowestPicksTheSmallestBalanceAndTheEarliestOnATie() {
        Forecast f = new Forecast(List.of(day("2026-11-04", "500"), day("2026-11-05", "100"),
                day("2026-11-06", "300"), day("2026-11-07", "100")));
        assertEquals(Money.of("100"), f.lowestBalance());
        assertEquals(LocalDate.of(2026, 11, 5), f.lowest().date());
    }

    @Test
    void aForecastNeedsAtLeastOneDay() {
        assertThrows(IllegalArgumentException.class, () -> new Forecast(List.of()));
    }

    @Test
    void dailyBalanceReportsActivityAndRejectsNegativeFlows() {
        assertFalse(day("2026-11-04", "5").hasActivity());
        assertTrue(new DailyBalance(LocalDate.of(2026, 11, 4), Money.of("5"), Money.of("1"), Money.ZERO).hasActivity());
        assertThrows(IllegalArgumentException.class, () ->
                new DailyBalance(LocalDate.of(2026, 11, 4), Money.of("5"), Money.of("-1"), Money.ZERO));
    }

    @Test
    void savingsGoalValidatesAndComputesRemaining() {
        SavingsGoal g = new SavingsGoal("g1", "Laptop", Money.of("1200"), LocalDate.of(2027, 1, 1), Money.of("200"),
                Money.of("100"), GoalStatus.ON_TRACK);
        assertEquals(Money.of("1000"), g.remaining());
        assertFalse(g.isAchieved());
        assertEquals(Money.ZERO, new SavingsGoal("g2", "Done", Money.of("50"), LocalDate.of(2027, 1, 1), Money.of("80"),
                Money.ZERO, GoalStatus.ACHIEVED).remaining());
        assertThrows(IllegalArgumentException.class, () -> new SavingsGoal("g", "x", Money.ZERO, LocalDate.of(2027, 1, 1),
                Money.ZERO, Money.ZERO, GoalStatus.ON_TRACK));
        assertThrows(IllegalArgumentException.class, () -> new SavingsGoal(" ", "x", Money.of("5"), LocalDate.of(2027, 1, 1),
                Money.ZERO, Money.ZERO, GoalStatus.ON_TRACK));
        assertThrows(IllegalArgumentException.class, () -> new SavingsGoal("g", "x", Money.of("5"), LocalDate.of(2027, 1, 1),
                Money.of("-1"), Money.ZERO, GoalStatus.ON_TRACK));
    }

    @Test
    void safeToSpendResultValidatesAndCanBeReworded() {
        SafeToSpendResult r = new SafeToSpendResult(Money.of("10"), Money.of("100"), Money.of("50"), Money.ZERO, "",
                Money.of("500"), Money.ZERO, Money.of("200"), 10, Money.ZERO);
        assertFalse(r.isOverCommitted());
        assertEquals("hello", r.withExplanation("hello").explanation());
        assertEquals(r.dailyAllowance(), r.withExplanation("x").dailyAllowance());
        assertThrows(IllegalArgumentException.class, () -> new SafeToSpendResult(Money.of("-1"), Money.ZERO, Money.ZERO,
                Money.ZERO, "", Money.ZERO, Money.ZERO, Money.ZERO, 1, Money.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new SafeToSpendResult(Money.ZERO, Money.ZERO, Money.ZERO,
                Money.ZERO, "", Money.ZERO, Money.ZERO, Money.ZERO, 0, Money.ZERO));
        assertTrue(new SafeToSpendResult(Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, "", Money.ZERO, Money.ZERO,
                Money.ZERO, 1, Money.of("5")).isOverCommitted());
    }
}
