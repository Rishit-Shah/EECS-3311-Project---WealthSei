package wealthsei.analytics;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import wealthsei.domain.AlertSeverity;
import wealthsei.domain.Categories;
import wealthsei.domain.Category;
import wealthsei.domain.Insight;
import wealthsei.domain.InsightType;
import wealthsei.domain.Money;
import wealthsei.domain.Transaction;

class HabitAnalyzerTest {

    private static final YearMonth OCT = YearMonth.of(2026, 10);
    private final HabitAnalyzer analyzer = new HabitAnalyzer();
    private int next = 0;

    private Transaction spend(LocalDate date, String amount, Category category) {
        return new Transaction("t" + (++next), date, Money.of(amount), "Shop", "SHOP", false, category);
    }

    private Transaction earn(LocalDate date, String amount) {
        return new Transaction("t" + (++next), date, Money.of(amount), "Employer", "PAY", true, Categories.INCOME);
    }

    private static boolean weekend(LocalDate d) {
        return d.getDayOfWeek().getValue() >= 6;
    }

    private static boolean has(List<Insight> list, InsightType type) {
        return list.stream().anyMatch(i -> i.type() == type);
    }

    /** One purchase per day of October 2026. */
    private List<Transaction> everyDay(String weekdayAmount, String weekendAmount, Category category) {
        List<Transaction> txs = new ArrayList<>();
        for (int day = 1; day <= 31; day++) {
            LocalDate d = OCT.atDay(day);
            txs.add(spend(d, weekend(d) ? weekendAmount : weekdayAmount, category));
        }
        return txs;
    }

    @Test
    void emptyHistoryGivesNoInsights() {
        assertTrue(analyzer.analyze(List.of()).isEmpty());
        assertTrue(analyzer.analyze(List.of(), OCT).isEmpty());
    }

    @Test
    void steadySpendingFindsNothing() {
        assertTrue(analyzer.analyze(everyDay("20", "20", Categories.DINING), OCT).isEmpty());
    }

    @Test
    void weekendSpikeIsFoundWithEvidence() {
        List<Insight> found = analyzer.analyze(everyDay("20", "70", Categories.DINING), OCT);
        Insight spike = found.stream().filter(i -> i.type() == InsightType.WEEKEND_SPIKE).findFirst().orElseThrow();
        assertTrue(spike.evidence().contains("70.00"));
        assertTrue(spike.evidence().contains("20.00"));
        assertEquals(AlertSeverity.WARNING, spike.severity());
    }

    @Test
    void essentialSpendingDoesNotCountAsAHabit() {
        assertTrue(analyzer.analyze(everyDay("20", "200", Categories.GROCERIES), OCT).isEmpty());
    }

    @Test
    void paydaySplurgeIsFound() {
        List<Transaction> txs = new ArrayList<>();
        txs.add(earn(OCT.atDay(15), "2000"));
        for (int day = 1; day <= 31; day++) {
            boolean payWindow = day >= 15 && day <= 17;
            txs.add(spend(OCT.atDay(day), payWindow ? "100" : "5", Categories.SHOPPING));
        }
        Insight found = analyzer.analyze(txs, OCT).stream()
                .filter(i -> i.type() == InsightType.PAYDAY_SPLURGE).findFirst().orElseThrow();
        assertTrue(found.evidence().contains("300.00"));
    }

    @Test
    void monthEndCrunchIsFound() {
        List<Transaction> txs = new ArrayList<>();
        for (int day = 1; day <= 31; day++) {
            txs.add(spend(OCT.atDay(day), day >= 25 ? "60" : "15", Categories.ENTERTAINMENT));
        }
        assertTrue(has(analyzer.analyze(txs, OCT), InsightType.MONTH_END_CRUNCH));
    }

    @Test
    void smallPurchasesThatAddUpAreFound() {
        List<Transaction> txs = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            txs.add(spend(OCT.atDay(i * 2), "9", Categories.DINING));
        }
        Insight leak = analyzer.analyze(txs, OCT).stream()
                .filter(i -> i.type() == InsightType.SMALL_PURCHASE_LEAK).findFirst().orElseThrow();
        assertTrue(leak.evidence().contains("10 purchases"));
        assertTrue(leak.evidence().contains("90.00"));
    }

    @Test
    void fewSmallPurchasesAreNotALeak() {
        List<Transaction> txs = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            txs.add(spend(OCT.atDay(i * 3), "9", Categories.DINING));
        }
        assertFalse(has(analyzer.analyze(txs, OCT), InsightType.SMALL_PURCHASE_LEAK));
    }

    @Test
    void unusualExpenseIsAboveTheCategoryNorm() {
        List<Transaction> txs = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            txs.add(spend(LocalDate.of(2026, 9, i * 4), "20", Categories.SHOPPING));
        }
        txs.add(spend(LocalDate.of(2026, 10, 12), "400", Categories.SHOPPING));
        List<Insight> found = analyzer.analyze(txs, OCT);
        Insight unusual = found.stream().filter(i -> i.type() == InsightType.UNUSUAL_EXPENSE).findFirst().orElseThrow();
        assertTrue(unusual.evidence().contains("400.00"));
        assertEquals(AlertSeverity.WARNING, unusual.severity());
    }

    @Test
    void anExpenseInLineWithTheNormIsNotUnusual() {
        List<Transaction> txs = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            txs.add(spend(LocalDate.of(2026, 9, i * 4), "20", Categories.SHOPPING));
        }
        txs.add(spend(LocalDate.of(2026, 10, 12), "25", Categories.SHOPPING));
        assertFalse(has(analyzer.analyze(txs, OCT), InsightType.UNUSUAL_EXPENSE));
    }

    @Test
    void unusualNeedsEnoughHistory() {
        List<Transaction> txs = List.of(
                spend(LocalDate.of(2026, 9, 5), "20", Categories.SHOPPING),
                spend(LocalDate.of(2026, 10, 12), "400", Categories.SHOPPING));
        assertFalse(has(analyzer.analyze(txs, OCT), InsightType.UNUSUAL_EXPENSE));
    }

    @Test
    void analyzeWithoutAMonthUsesTheLatestOne() {
        List<Transaction> txs = everyDay("20", "70", Categories.DINING);
        txs.add(spend(LocalDate.of(2026, 9, 1), "1", Categories.DINING));
        assertTrue(has(analyzer.analyze(txs), InsightType.WEEKEND_SPIKE));
    }

    @Test
    void worseInsightsComeFirst() {
        List<Insight> found = analyzer.analyze(everyDay("5", "90", Categories.DINING), OCT);
        assertFalse(found.isEmpty());
        for (int i = 1; i < found.size(); i++) {
            assertTrue(found.get(i - 1).severity().compareTo(found.get(i).severity()) >= 0);
        }
    }

    @Test
    void insightRejectsBlankTitle() {
        assertThrows(IllegalArgumentException.class,
                () -> new Insight(InsightType.WEEKEND_SPIKE, " ", "e", AlertSeverity.INFO, "x"));
    }
}
