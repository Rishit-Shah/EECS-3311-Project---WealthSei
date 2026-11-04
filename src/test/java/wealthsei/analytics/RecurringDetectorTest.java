package wealthsei.analytics;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Categories;
import wealthsei.domain.Frequency;
import wealthsei.domain.Money;
import wealthsei.domain.RecurringPayment;
import wealthsei.domain.Transaction;

class RecurringDetectorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);
    private final RecurringDetector detector = new RecurringDetector();
    private int counter;

    private Transaction tx(String date, String merchant, String amount) {
        return new Transaction("t" + (++counter), LocalDate.parse(date), Money.of(amount), merchant, "", false,
                Categories.SUBSCRIPTIONS);
    }

    private List<Transaction> monthly(String merchant, String amount, String... dates) {
        List<Transaction> list = new ArrayList<>();
        for (String d : dates) {
            list.add(tx(d, merchant, amount));
        }
        return list;
    }

    @Test
    void aSteadyMonthlySubscriptionIsFound() {
        List<RecurringPayment> found = detector.detect(
                monthly("Netflix", "16.49", "2026-07-05", "2026-08-05", "2026-09-05", "2026-10-05"), TODAY);
        assertEquals(1, found.size());
        RecurringPayment p = found.get(0);
        assertEquals("Netflix", p.merchant());
        assertEquals(Frequency.MONTHLY, p.frequency());
        assertEquals(Money.of("16.49"), p.amount());
        assertEquals(LocalDate.of(2026, 11, 5), p.nextDue());
        assertEquals(1.0, p.confidence());
        assertFalse(p.isPossible());
        assertFalse(p.hasPriceIncrease());
        assertEquals(4, p.occurrences());
    }

    @Test
    void twoPaymentsAreNotEnoughHistory() {
        assertTrue(detector.detect(monthly("Netflix", "16.49", "2026-09-05", "2026-10-05"), TODAY).isEmpty());
    }

    @Test
    void weeklyBiweeklyAndYearlyPatternsAreRecognised() {
        List<Transaction> all = new ArrayList<>();
        all.addAll(monthly("Meal kit", "50", "2026-10-07", "2026-10-14", "2026-10-21", "2026-10-28"));
        all.addAll(monthly("Cleaner", "80", "2026-09-16", "2026-09-30", "2026-10-14", "2026-10-28"));
        all.addAll(monthly("Domain", "20", "2024-10-20", "2025-10-20", "2026-10-20"));
        List<RecurringPayment> found = detector.detect(all, TODAY);
        assertEquals(3, found.size());
        assertEquals(Frequency.WEEKLY, byName(found, "Meal kit").frequency());
        assertEquals(LocalDate.of(2026, 11, 4), byName(found, "Meal kit").nextDue());
        assertEquals(Frequency.BIWEEKLY, byName(found, "Cleaner").frequency());
        assertEquals(LocalDate.of(2026, 11, 11), byName(found, "Cleaner").nextDue());
        assertEquals(Frequency.YEARLY, byName(found, "Domain").frequency());
        assertEquals(LocalDate.of(2027, 10, 20), byName(found, "Domain").nextDue());
    }

    private static RecurringPayment byName(List<RecurringPayment> list, String name) {
        return list.stream().filter(p -> p.merchant().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void irregularGapsAreNotRecurring() {
        assertTrue(detector.detect(monthly("Hardware store", "30",
                "2026-08-01", "2026-08-09", "2026-09-20", "2026-10-02"), TODAY).isEmpty());
    }

    @Test
    void oneMissedMonthLowersConfidenceButStillCounts() {
        List<RecurringPayment> found = detector.detect(monthly("Gym", "40",
                "2026-05-03", "2026-06-03", "2026-08-03", "2026-09-03", "2026-10-03"), TODAY);
        assertEquals(1, found.size());
        assertEquals(0.85, found.get(0).confidence());
        assertFalse(found.get(0).isPossible());
    }

    @Test
    void wildlyVaryingAmountsAreNotRecurring() {
        assertTrue(detector.detect(List.of(tx("2026-08-07", "Loblaws", "20"), tx("2026-09-07", "Loblaws", "95"),
                tx("2026-10-07", "Loblaws", "31")), TODAY).isEmpty());
    }

    @Test
    void moderatelyVaryingAmountsAreShownAsPossible() {
        List<RecurringPayment> found = detector.detect(List.of(
                tx("2026-06-07", "Hydro", "60"), tx("2026-07-07", "Hydro", "62"), tx("2026-08-07", "Hydro", "90"),
                tx("2026-09-07", "Hydro", "93"), tx("2026-10-07", "Hydro", "130")), TODAY);
        assertEquals(1, found.size());
        assertTrue(found.get(0).isPossible());
        assertEquals(0.8, found.get(0).confidence());
    }

    @Test
    void aPriceIncreaseIsReported() {
        List<Transaction> txs = new ArrayList<>(monthly("Spotify", "10.99", "2026-07-12", "2026-08-12", "2026-09-12"));
        txs.add(tx("2026-10-12", "Spotify", "11.99"));
        RecurringPayment p = detector.detect(txs, TODAY).get(0);
        assertEquals(Money.of("11.99"), p.amount());
        assertEquals(Money.of("1.00"), p.priceIncrease());
        assertTrue(p.hasPriceIncrease());
    }

    @Test
    void aSubscriptionThatStoppedIsLeftOut() {
        assertTrue(detector.detect(monthly("Old gym", "40", "2026-01-03", "2026-02-03", "2026-03-03", "2026-04-03"),
                TODAY).isEmpty());
    }

    @Test
    void incomeAndFutureRowsAreIgnored() {
        List<Transaction> txs = new ArrayList<>();
        for (String d : List.of("2026-08-01", "2026-09-01", "2026-10-01")) {
            txs.add(new Transaction("i" + d, LocalDate.parse(d), Money.of("2000"), "Payroll", "", true, Categories.INCOME));
        }
        assertTrue(detector.detect(txs, TODAY).isEmpty());
        List<Transaction> future = monthly("Future", "9", "2026-11-10", "2026-12-10", "2027-01-10");
        assertTrue(detector.detect(future, TODAY).isEmpty());
    }

    @Test
    void merchantNamesAreGroupedIgnoringCase() {
        List<Transaction> txs = List.of(tx("2026-08-05", "NETFLIX", "16.49"), tx("2026-09-05", "netflix", "16.49"),
                tx("2026-10-05", "Netflix", "16.49"));
        List<RecurringPayment> found = detector.detect(txs, TODAY);
        assertEquals(1, found.size());
        assertEquals("Netflix", found.get(0).merchant());
    }

    @Test
    void severalPaymentsOnOneDayAreNotAPattern() {
        assertTrue(detector.detect(monthly("Coffee", "4", "2026-10-01", "2026-10-01", "2026-10-01"), TODAY).isEmpty());
    }

    @Test
    void resultsAreSortedByMonthlyCostThenName() {
        List<Transaction> all = new ArrayList<>();
        all.addAll(monthly("Small", "5", "2026-08-01", "2026-09-01", "2026-10-01"));
        all.addAll(monthly("Big", "50", "2026-08-02", "2026-09-02", "2026-10-02"));
        all.addAll(monthly("Alpha", "5", "2026-08-03", "2026-09-03", "2026-10-03"));
        assertEquals(List.of("Big", "Alpha", "Small"),
                detector.detect(all, TODAY).stream().map(RecurringPayment::merchant).toList());
    }

    @Test
    void monthlyCostConvertsOtherFrequencies() {
        RecurringPayment yearly = detector.detect(monthly("Domain", "120", "2024-10-20", "2025-10-20", "2026-10-20"), TODAY).get(0);
        assertEquals(Money.of("10.00"), yearly.monthlyCost());
    }
}
