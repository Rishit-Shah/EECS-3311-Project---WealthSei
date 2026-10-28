package wealthsei.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class BudgetStatusTest {

    private static final Category FOOD = new Category("groceries", "Groceries", true);
    private static final Category FUN = new Category("fun", "Fun", false);
    private static final Category RENT = new Category("rent", "Rent", true);

    private BudgetLineStatus line(String spent, String limit) {
        return new BudgetLineStatus(FOOD, Money.of(spent), Money.of(limit));
    }

    @Test
    void levelThresholds() {
        assertEquals(AlertSeverity.INFO, line("79.99", "100").level());
        assertEquals(AlertSeverity.WARNING, line("80", "100").level());
        assertEquals(AlertSeverity.WARNING, line("99.99", "100").level());
        assertEquals(AlertSeverity.WARNING, line("100", "100").level());
        assertEquals(AlertSeverity.OVER, line("100.01", "100").level());
    }

    @Test
    void zeroSpentIsInfoEvenWithZeroLimit() {
        assertEquals(AlertSeverity.INFO, line("0", "100").level());
        assertEquals(AlertSeverity.INFO, line("0", "0").level());
        assertEquals(0.0, line("0", "0").ratio());
    }

    @Test
    void spendingAgainstZeroLimitIsOver() {
        assertEquals(AlertSeverity.OVER, line("1", "0").level());
        assertEquals(Double.POSITIVE_INFINITY, line("1", "0").ratio());
    }

    @Test
    void ratioAndRemaining() {
        assertEquals(0.5, line("50", "100").ratio());
        assertEquals(1.2, line("120", "100").ratio(), 0.0000001);
        assertEquals(Money.of("50"), line("50", "100").remaining());
        assertEquals(Money.of("-20"), line("120", "100").remaining());
    }

    @Test
    void rejectsNegativeAmounts() {
        assertThrows(IllegalArgumentException.class, () -> line("-1", "100"));
        assertThrows(IllegalArgumentException.class, () -> line("1", "-100"));
    }

    @Test
    void statusTotalsAndLookup() {
        BudgetStatus s = new BudgetStatus(YearMonth.of(2026, 10), List.of(
                new BudgetLineStatus(FOOD, Money.of("90"), Money.of("100")),
                new BudgetLineStatus(FUN, Money.of("120"), Money.of("100")),
                new BudgetLineStatus(RENT, Money.of("10"), Money.of("1000"))));
        assertEquals(Money.of("220"), s.totalSpent());
        assertEquals(Money.of("1200"), s.totalLimit());
        assertEquals(Money.of("120"), s.lineFor(FUN).orElseThrow().spent());
        assertTrue(s.lineFor(new Category("x", "X", false)).isEmpty());
    }

    @Test
    void linesAtLeastFiltersBySeverity() {
        BudgetStatus s = new BudgetStatus(YearMonth.of(2026, 10), List.of(
                new BudgetLineStatus(FOOD, Money.of("90"), Money.of("100")),
                new BudgetLineStatus(FUN, Money.of("120"), Money.of("100")),
                new BudgetLineStatus(RENT, Money.of("10"), Money.of("1000"))));
        assertEquals(2, s.linesAtLeast(AlertSeverity.WARNING).size());
        assertEquals(1, s.linesAtLeast(AlertSeverity.OVER).size());
        assertEquals(3, s.linesAtLeast(AlertSeverity.INFO).size());
    }

    @Test
    void emptyStatusIsAllowedAndTotalsZero() {
        BudgetStatus s = new BudgetStatus(YearMonth.of(2026, 10), List.of());
        assertEquals(Money.ZERO, s.totalSpent());
        assertEquals(Money.ZERO, s.totalLimit());
    }
}
