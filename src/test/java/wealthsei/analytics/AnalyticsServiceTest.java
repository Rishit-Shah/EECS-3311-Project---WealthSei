package wealthsei.analytics;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Categories;
import wealthsei.domain.Money;
import wealthsei.domain.Transaction;
import wealthsei.repository.InMemoryGoalRepository;
import wealthsei.repository.InMemoryPlanRepository;
import wealthsei.repository.InMemoryRecurringRepository;
import wealthsei.repository.InMemoryTransactionRepository;
import wealthsei.service.AlertMonitor;
import wealthsei.service.ValidationException;

class AnalyticsServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);
    private final InMemoryTransactionRepository txRepo = new InMemoryTransactionRepository();
    private final InMemoryRecurringRepository flags = new InMemoryRecurringRepository();
    private final AnalyticsService service = new AnalyticsService(txRepo, new InMemoryGoalRepository(), flags,
            new InMemoryPlanRepository(), new AlertMonitor(), new RecurringDetector(), new ForecastEngine(),
            new SafeToSpendCalculator());
    private int n;

    private void add(String merchant, String amount, String... dates) {
        for (String d : dates) {
            txRepo.saveAll(List.of(new Transaction("t" + (++n), LocalDate.parse(d), Money.of(amount), merchant, "",
                    false, Categories.SUBSCRIPTIONS)));
        }
    }

    @Test
    void emptyHistoryAsksForAnImport() {
        RecurringReport r = service.recurringReport(TODAY);
        assertTrue(r.payments().isEmpty());
        assertEquals(Money.ZERO, r.monthlyTotal());
        assertTrue(r.summary().contains("Import a bank CSV"));
    }

    @Test
    void historyWithoutPatternsSaysNotEnoughHistory() {
        add("Netflix", "16.49", "2026-09-05", "2026-10-05");
        assertTrue(service.recurringReport(TODAY).summary().contains("at least 3 payments"));
    }

    @Test
    void reportTotalsTheMonthlyCostAndSummarises() {
        add("Netflix", "16.49", "2026-07-05", "2026-08-05", "2026-09-05", "2026-10-05");
        add("Spotify", "10.99", "2026-07-12", "2026-08-12", "2026-09-12");
        add("Spotify", "11.99", "2026-10-12");
        RecurringReport r = service.recurringReport(TODAY);
        assertEquals(2, r.payments().size());
        assertEquals(Money.of("28.48"), r.monthlyTotal());
        assertTrue(r.summary().contains("2 recurring payments costing about 28.48 per month"), r.summary());
        assertTrue(r.summary().contains("The biggest is Netflix at 16.49 monthly."), r.summary());
        assertTrue(r.summary().contains("Spotify went up by 1.00."), r.summary());
    }

    @Test
    void flagsAreAppliedAndCanBeCleared() {
        add("Netflix", "16.49", "2026-08-05", "2026-09-05", "2026-10-05");
        service.flagRecurring("netflix", true, TODAY);
        assertTrue(service.detectRecurring(TODAY).get(0).flaggedForReview());
        assertTrue(service.recurringReport(TODAY).summary().contains("1 flagged for review."));
        service.flagRecurring("NETFLIX", false, TODAY);
        assertFalse(service.detectRecurring(TODAY).get(0).flaggedForReview());
    }

    @Test
    void flaggingSomethingThatIsNotRecurringIsRejected() {
        add("Netflix", "16.49", "2026-08-05", "2026-09-05", "2026-10-05");
        assertThrows(ValidationException.class, () -> service.flagRecurring("Costco", true, TODAY));
        assertTrue(flags.flaggedMerchants().isEmpty());
    }
}
