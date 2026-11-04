package wealthsei.analytics;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Categories;
import wealthsei.domain.DailyBalance;
import wealthsei.domain.FixedBill;
import wealthsei.domain.Forecast;
import wealthsei.domain.Frequency;
import wealthsei.domain.IncomeSource;
import wealthsei.domain.Money;
import wealthsei.domain.PlannedExpense;
import wealthsei.domain.RecurringPayment;

class ForecastEngineTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);
    private final ForecastEngine engine = new ForecastEngine();

    private static ForecastInputs inputs(String balance, List<IncomeSource> income, List<FixedBill> bills,
                                         List<RecurringPayment> recurring, List<PlannedExpense> planned) {
        return new ForecastInputs(TODAY, Money.of(balance), List.of(), recurring, income, bills, planned);
    }

    private static RecurringPayment recurring(String merchant, String amount, String nextDue) {
        return new RecurringPayment(merchant, Money.of(amount), Frequency.MONTHLY, LocalDate.parse(nextDue), 1.0, false,
                Money.ZERO, 4);
    }

    private static DailyBalance on(Forecast f, String date) {
        return f.days().stream().filter(d -> d.date().equals(LocalDate.parse(date))).findFirst().orElseThrow();
    }

    @Test
    void startsWithTodayAtTheOpeningBalanceAndHasDaysPlusOneEntries() {
        Forecast f = engine.project(inputs("500", List.of(), List.of(), List.of(), List.of()), 30);
        assertEquals(31, f.days().size());
        assertEquals(TODAY, f.days().get(0).date());
        assertEquals(Money.of("500"), f.days().get(0).balance());
        assertEquals(TODAY.plusDays(30), f.days().get(30).date());
        assertEquals(Money.of("500"), f.lowestBalance());
    }

    @Test
    void incomeBillsRecurringAndPlannedExpensesMoveTheBalanceOnTheirDays() {
        Forecast f = engine.project(inputs("1000",
                List.of(new IncomeSource("Job", Money.of("2000"), Frequency.MONTHLY, LocalDate.of(2026, 11, 15))),
                List.of(new FixedBill("Rent", Money.of("900"), Frequency.MONTHLY, LocalDate.of(2026, 11, 1), Categories.RENT)),
                List.of(recurring("Netflix", "16.49", "2026-11-05")),
                List.of(new PlannedExpense("Laptop", Money.of("300"), LocalDate.of(2026, 11, 10), null))), 30);
        assertEquals(Money.of("983.51"), on(f, "2026-11-05").balance());
        assertEquals(Money.of("683.51"), on(f, "2026-11-10").balance());
        assertEquals(Money.of("2683.51"), on(f, "2026-11-15").balance());
        assertEquals(Money.of("16.49"), on(f, "2026-11-05").outgoing());
        assertEquals(Money.of("2000"), on(f, "2026-11-15").income());
        // rent was due Nov 1 (past), next on Dec 1, which is day 27; Netflix again Dec 5 is outside 30 days
        assertEquals(Money.of("900"), on(f, "2026-12-01").outgoing());
        assertEquals(Money.of("683.51"), f.lowestBalance());
    }

    @Test
    void aPastAnchorStillProducesFutureOccurrences() {
        Forecast f = engine.project(inputs("0",
                List.of(new IncomeSource("Pay", Money.of("100"), Frequency.WEEKLY, LocalDate.of(2026, 10, 1))),
                List.of(), List.of(), List.of()), 14);
        // weekly from Oct 1 (a Thursday): Nov 5 and Nov 12 fall in Nov 5..Nov 18
        assertEquals(Money.of("100"), on(f, "2026-11-05").income());
        assertEquals(Money.of("100"), on(f, "2026-11-12").income());
        assertEquals(Money.of("200"), f.days().get(14).balance());
    }

    @Test
    void aRecurringPaymentMatchingAPlanBillIsCountedOnce() {
        Forecast f = engine.project(inputs("1000", List.of(),
                List.of(new FixedBill("Netflix", Money.of("16.49"), Frequency.MONTHLY, LocalDate.of(2026, 11, 5), null)),
                List.of(recurring("Netflix.com", "16.49", "2026-11-05")), List.of()), 10);
        assertEquals(Money.of("16.49"), on(f, "2026-11-05").outgoing());
    }

    @Test
    void plannedExpensesOutsideTheWindowAreIgnored() {
        Forecast f = engine.project(inputs("100", List.of(), List.of(), List.of(), List.of(
                new PlannedExpense("Past", Money.of("50"), LocalDate.of(2026, 11, 3), null),
                new PlannedExpense("Today", Money.of("50"), TODAY, null),
                new PlannedExpense("Far", Money.of("50"), LocalDate.of(2027, 3, 1), null))), 30);
        assertEquals(Money.of("100"), f.lowestBalance());
    }

    @Test
    void theBalanceCanGoNegative() {
        Forecast f = engine.project(inputs("100", List.of(), List.of(), List.of(), List.of(
                new PlannedExpense("Big", Money.of("250"), TODAY.plusDays(2), null))), 5);
        assertEquals(Money.of("-150"), f.lowestBalance());
        assertEquals(TODAY.plusDays(2), f.lowest().date());
    }

    @Test
    void fewerThanOneDayIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> engine.project(inputs("1", List.of(), List.of(), List.of(), List.of()), 0));
    }

    @Test
    void monthlySurplusIsIncomeMinusBillsAndUncoveredRecurring() {
        Money surplus = engine.monthlySurplus(inputs("0",
                List.of(new IncomeSource("Job", Money.of("1000"), Frequency.BIWEEKLY, TODAY),
                        new IncomeSource("Side", Money.of("120"), Frequency.YEARLY, TODAY)),
                List.of(new FixedBill("Rent", Money.of("900"), Frequency.MONTHLY, TODAY, null)),
                List.of(recurring("Spotify", "10", "2026-11-12"), recurring("Rent Co", "900", "2026-11-01")), List.of()));
        // 1000 biweekly = 2166.67 per month (1000*26/12), 120 yearly = 10, rent 900, Spotify 10, "Rent Co" covered by the Rent bill
        assertEquals(Money.of("2166.67").add(Money.of("10")).subtract(Money.of("900")).subtract(Money.of("10")), surplus);
    }

    @Test
    void deepCopyIsEqualButIndependentOfLaterChanges() {
        ForecastInputs original = inputs("500", List.of(), List.of(), List.of(), List.of());
        ForecastInputs copy = original.deepCopy();
        assertEquals(original, copy);
        ForecastInputs changed = copy.withOpeningBalance(Money.of("1"));
        assertEquals(Money.of("500"), original.openingBalance());
        assertEquals(Money.of("500"), copy.openingBalance());
        assertEquals(Money.of("1"), changed.openingBalance());
        ForecastInputs withExpense = original.withPlannedExpenses(List.of(new PlannedExpense("x", Money.of("5"), TODAY, null)));
        assertTrue(original.plannedExpenses().isEmpty());
        assertEquals(1, withExpense.plannedExpenses().size());
    }
}
