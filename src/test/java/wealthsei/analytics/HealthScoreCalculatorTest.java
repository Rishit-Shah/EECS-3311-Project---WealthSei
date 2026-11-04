package wealthsei.analytics;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import wealthsei.domain.BudgetLineStatus;
import wealthsei.domain.BudgetStatus;
import wealthsei.domain.Categories;
import wealthsei.domain.DailyBalance;
import wealthsei.domain.Forecast;
import wealthsei.domain.GoalStatus;
import wealthsei.domain.HealthScore;
import wealthsei.domain.Money;
import wealthsei.domain.SavingsGoal;
import wealthsei.domain.ScoreComponent;

class HealthScoreCalculatorTest {

    private final HealthScoreCalculator calculator = new HealthScoreCalculator();

    private static BudgetStatus budget(String... spentOfHundred) {
        List<BudgetLineStatus> lines = new ArrayList<>();
        var categories = List.of(Categories.DINING, Categories.SHOPPING, Categories.ENTERTAINMENT);
        for (int i = 0; i < spentOfHundred.length; i++) {
            lines.add(new BudgetLineStatus(categories.get(i), Money.of(spentOfHundred[i]), Money.of("100")));
        }
        return new BudgetStatus(YearMonth.of(2026, 11), lines);
    }

    /** 30 days: all income on day 1, all spending on day 2, starting from {@code balance}. */
    private static Forecast forecast(String balance, String income, String outgoing) {
        LocalDate start = LocalDate.of(2026, 11, 2);
        List<DailyBalance> days = new ArrayList<>();
        Money running = Money.of(balance);
        days.add(new DailyBalance(start, running, Money.ZERO, Money.ZERO));
        for (int i = 1; i <= 30; i++) {
            Money in = i == 1 ? Money.of(income) : Money.ZERO;
            Money out = i == 2 ? Money.of(outgoing) : Money.ZERO;
            running = running.add(in).subtract(out);
            days.add(new DailyBalance(start.plusDays(i), running, in, out));
        }
        return new Forecast(days);
    }

    private static SavingsGoal goal(GoalStatus status) {
        return new SavingsGoal("g" + status, "Goal", Money.of("1000"), LocalDate.of(2027, 6, 1), Money.ZERO,
                Money.of("50"), status);
    }

    private static ScoreComponent part(HealthScore s, String name) {
        return s.components().stream().filter(c -> c.name().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void everythingHealthyScoresOneHundred() {
        HealthScore s = calculator.calculate(budget("10", "20"), forecast("3000", "1000", "500"),
                List.of(goal(GoalStatus.ON_TRACK)));
        assertEquals(100, s.total());
        assertEquals(4, s.components().size());
        assertTrue(s.excluded().isEmpty());
        assertNull(HealthScoreCalculator.biggestGap(s));
    }

    @Test
    void budgetPointsFallWhenLinesAreOverOrWarning() {
        HealthScore s = calculator.calculate(budget("150", "10"), null, List.of());
        assertEquals(15, part(s, "Budget adherence").points());
        HealthScore warn = calculator.calculate(budget("85", "10"), null, List.of());
        assertEquals(23, part(warn, "Budget adherence").points());
    }

    @Test
    void savingsRateScalesUpToTwentyPercent() {
        HealthScore s = calculator.calculate(null, forecast("0", "1000", "900"), List.of());
        assertEquals(13, part(s, "Savings rate").points());
        HealthScore negative = calculator.calculate(null, forecast("0", "1000", "1500"), List.of());
        assertEquals(0, part(negative, "Savings rate").points());
    }

    @Test
    void bufferScalesUpToThreeMonths() {
        HealthScore s = calculator.calculate(null, forecast("500", "1000", "1000"), List.of());
        assertEquals(4, part(s, "Emergency buffer").points());
        HealthScore plenty = calculator.calculate(null, forecast("9000", "1000", "1000"), List.of());
        assertEquals(25, part(plenty, "Emergency buffer").points());
    }

    @Test
    void goalPointsFollowStatus() {
        HealthScore s = calculator.calculate(null, null,
                List.of(goal(GoalStatus.ON_TRACK), goal(GoalStatus.BEHIND)));
        assertEquals(10, part(s, "Goal progress").points());
        HealthScore risk = calculator.calculate(null, null, List.of(goal(GoalStatus.AT_RISK)));
        assertEquals(10, part(risk, "Goal progress").points());
        HealthScore done = calculator.calculate(null, null, List.of(goal(GoalStatus.ACHIEVED)));
        assertEquals(20, part(done, "Goal progress").points());
    }

    @Test
    void missingPartsAreLeftOutAndTheRestRescaled() {
        HealthScore s = calculator.calculate(null, forecast("9000", "1000", "500"), List.of());
        assertEquals(2, s.components().size());
        assertEquals(100, s.total());
        assertEquals(2, s.excluded().size());
        assertTrue(s.excluded().get(0).startsWith("Budget adherence"));
    }

    @Test
    void halfScoreWhenOnlyOnePartIsHalf() {
        HealthScore s = calculator.calculate(budget("150", "10"), null, List.of());
        assertEquals(50, s.total());
    }

    @Test
    void noDataGivesNotAvailable() {
        HealthScore s = calculator.calculate(null, null, List.of());
        assertFalse(s.isAvailable());
        assertEquals("N/A", s.explanation());
        assertEquals(4, s.excluded().size());
        assertTrue(HealthScoreSummaryTemplate.write(s).startsWith("N/A"));
    }

    @Test
    void noIncomeLeavesOutSavingsRate() {
        HealthScore s = calculator.calculate(null, forecast("1000", "0", "300"), List.of());
        assertTrue(s.excluded().stream().anyMatch(e -> e.startsWith("Savings rate")));
        assertEquals(1, s.components().size());
    }

    @Test
    void biggestGapIsTheWeakestPart() {
        HealthScore s = calculator.calculate(budget("10"), forecast("0", "1000", "1000"), List.of());
        ScoreComponent gap = HealthScoreCalculator.biggestGap(s);
        assertNotNull(gap);
        assertEquals(25, gap.missing());
        String text = HealthScoreSummaryTemplate.write(s);
        assertTrue(text.contains("out of 100"));
        assertTrue(text.contains("biggest room to improve"));
    }

    @Test
    void changeFromLastMonthIsReportedWhenKnown() {
        HealthScore s = calculator.calculate(budget("10"), null, List.of()).withPreviousTotal(80);
        assertEquals(20, s.changeFromLastMonth().getAsInt());
        assertTrue(HealthScoreSummaryTemplate.write(s).contains("20 up"));
    }

    @Test
    void domainRejectsBadPoints() {
        assertThrows(IllegalArgumentException.class, () -> new ScoreComponent("x", 31, 30, "d"));
        assertThrows(IllegalArgumentException.class, () -> new HealthScore(101, List.of(), List.of(), "e", null));
    }
}
