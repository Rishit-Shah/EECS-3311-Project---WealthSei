package wealthsei.analytics;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Alert;
import wealthsei.domain.AlertSeverity;
import wealthsei.domain.AlertType;
import wealthsei.domain.Categories;
import wealthsei.domain.FinancialPlan;
import wealthsei.domain.FixedBill;
import wealthsei.domain.Forecast;
import wealthsei.domain.Frequency;
import wealthsei.domain.GoalStatus;
import wealthsei.domain.IncomeSource;
import wealthsei.domain.Money;
import wealthsei.domain.SafeToSpendResult;
import wealthsei.domain.SavingsGoal;
import wealthsei.domain.Transaction;
import wealthsei.repository.InMemoryGoalRepository;
import wealthsei.repository.InMemoryPlanRepository;
import wealthsei.repository.InMemoryRecurringRepository;
import wealthsei.repository.InMemoryTransactionRepository;
import wealthsei.service.AlertMonitor;
import wealthsei.service.ValidationException;

class SafeToSpendServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);
    private final InMemoryTransactionRepository txRepo = new InMemoryTransactionRepository();
    private final InMemoryGoalRepository goals = new InMemoryGoalRepository();
    private final InMemoryPlanRepository planRepo = new InMemoryPlanRepository();
    private final AlertMonitor monitor = new AlertMonitor(() -> Instant.parse("2026-11-04T12:00:00Z"));
    private final List<Alert> alerts = new ArrayList<>();
    private final AnalyticsService service = new AnalyticsService(txRepo, goals, new InMemoryRecurringRepository(),
            planRepo, monitor, new RecurringDetector(), new ForecastEngine(), new SafeToSpendCalculator());
    private int n;

    SafeToSpendServiceTest() {
        monitor.attach(alerts::add);
    }

    private void tx(String date, String merchant, String amount, boolean income) {
        txRepo.saveAll(List.of(new Transaction("t" + (++n), LocalDate.parse(date), Money.of(amount), merchant, "", income,
                income ? Categories.INCOME : Categories.SHOPPING)));
    }

    @Test
    void withoutAPlanItAsksForOne() {
        ValidationException e = assertThrows(ValidationException.class, () -> service.safeToSpend(TODAY));
        assertTrue(e.getMessage().contains("plan balance"));
        assertThrows(ValidationException.class, () -> service.forecast(30, TODAY));
    }

    @Test
    void aBareBalanceGivesBalanceMinusBufferSpreadOverTheMonth() {
        planRepo.save(FinancialPlan.starting(Money.of("1000"), TODAY).withSafetyBuffer(Money.of("200")));
        SafeToSpendResult r = service.safeToSpend(TODAY);
        assertEquals(Money.of("800"), r.remainingThisMonth());
        assertEquals(27, r.daysLeft());
        assertEquals(Money.of("800").divide(27), r.dailyAllowance());
        assertTrue(r.explanation().contains("You can spend about"), r.explanation());
        assertTrue(r.explanation().contains("No transactions are imported yet"), r.explanation());
        assertTrue(alerts.isEmpty());
    }

    @Test
    void transactionsAfterTheBalanceDateChangeTheBalanceToday() {
        planRepo.save(FinancialPlan.starting(Money.of("1000"), LocalDate.of(2026, 11, 1)).withSafetyBuffer(Money.ZERO));
        tx("2026-11-01", "Before", "999", false);     // on the balance date: already in the balance
        tx("2026-11-02", "Coffee", "40", false);
        tx("2026-11-03", "Refund", "15", true);
        tx("2026-11-10", "Future", "500", false);     // after today: ignored
        SafeToSpendResult r = service.safeToSpend(TODAY);
        assertEquals(Money.of("975"), r.balance());   // 1000 - 40 + 15
    }

    @Test
    void incomeBillsAndGoalsAreAppliedUpToMonthEnd() {
        planRepo.save(FinancialPlan.starting(Money.of("1500"), TODAY).withSafetyBuffer(Money.of("200"))
                .withIncomeSource(new IncomeSource("Job", Money.of("2000"), Frequency.MONTHLY, LocalDate.of(2026, 11, 15)))
                .withFixedBill(new FixedBill("Rent", Money.of("900"), Frequency.MONTHLY, LocalDate.of(2026, 11, 20), Categories.RENT)));
        goals.save(new SavingsGoal("g1", "Laptop", Money.of("1200"), LocalDate.of(2027, 2, 1), Money.ZERO, Money.of("100"),
                GoalStatus.ON_TRACK));
        SafeToSpendResult r = service.safeToSpend(TODAY);
        assertEquals(Money.of("2000"), r.expectedIncome());
        assertEquals(Money.of("900"), r.upcomingBills());
        assertEquals(Money.of("100"), r.goalReserve());
        assertEquals(Money.of("2300"), r.remainingThisMonth());
    }

    @Test
    void recurringPaymentsFoundInTheHistoryAreCountedAsUpcomingBills() {
        planRepo.save(FinancialPlan.starting(Money.of("1000"), LocalDate.of(2026, 10, 1)).withSafetyBuffer(Money.ZERO));
        tx("2026-08-12", "Spotify", "10.99", false);
        tx("2026-09-12", "Spotify", "10.99", false);
        tx("2026-10-12", "Spotify", "10.99", false);
        SafeToSpendResult r = service.safeToSpend(TODAY);
        assertEquals(Money.of("10.99"), r.upcomingBills());   // next due Nov 12
        // only the Oct 12 payment is after the Oct 1 balance date: 1000 - 10.99 = 989.01, then Nov 12 takes 10.99 more
        assertEquals(Money.of("989.01"), r.balance());
        assertEquals(Money.of("978.02"), r.remainingThisMonth());
    }

    @Test
    void overCommittedShowsZeroAndSaysSo() {
        planRepo.save(FinancialPlan.starting(Money.of("300"), TODAY).withSafetyBuffer(Money.of("200"))
                .withFixedBill(new FixedBill("Rent", Money.of("900"), Frequency.MONTHLY, LocalDate.of(2026, 11, 20), null)));
        SafeToSpendResult r = service.safeToSpend(TODAY);
        assertEquals(Money.ZERO, r.dailyAllowance());
        assertEquals(Money.of("800"), r.overCommittedBy());
        assertTrue(r.explanation().contains("over-committed by 800.00"), r.explanation());
    }

    @Test
    void lowBalanceAlertIsRaisedOnceAndAgainOnlyAfterItClears() {
        planRepo.save(FinancialPlan.starting(Money.of("500"), TODAY).withSafetyBuffer(Money.of("200"))
                .withFixedBill(new FixedBill("Rent", Money.of("400"), Frequency.MONTHLY, LocalDate.of(2026, 11, 20), null)));
        service.safeToSpend(TODAY);
        service.safeToSpend(TODAY);
        assertEquals(1, alerts.size());
        assertEquals(AlertType.LOW_BALANCE, alerts.get(0).type());
        assertEquals(AlertSeverity.WARNING, alerts.get(0).severity());
        assertTrue(alerts.get(0).message().contains("2026-11-20"), alerts.get(0).message());
        // add money: the problem goes away, then comes back
        planRepo.save(FinancialPlan.starting(Money.of("5000"), TODAY).withSafetyBuffer(Money.of("200")));
        service.safeToSpend(TODAY);
        planRepo.save(FinancialPlan.starting(Money.of("500"), TODAY).withSafetyBuffer(Money.of("200"))
                .withFixedBill(new FixedBill("Rent", Money.of("400"), Frequency.MONTHLY, LocalDate.of(2026, 11, 20), null)));
        service.safeToSpend(TODAY);
        assertEquals(2, alerts.size());
    }

    @Test
    void goingBelowZeroEscalatesToOver() {
        planRepo.save(FinancialPlan.starting(Money.of("500"), TODAY).withSafetyBuffer(Money.of("200"))
                .withFixedBill(new FixedBill("Rent", Money.of("400"), Frequency.MONTHLY, LocalDate.of(2026, 11, 20), null)));
        service.safeToSpend(TODAY);
        planRepo.save(FinancialPlan.starting(Money.of("500"), TODAY).withSafetyBuffer(Money.of("200"))
                .withFixedBill(new FixedBill("Rent", Money.of("900"), Frequency.MONTHLY, LocalDate.of(2026, 11, 20), null)));
        service.safeToSpend(TODAY);
        assertEquals(2, alerts.size());
        assertEquals(AlertSeverity.OVER, alerts.get(1).severity());
    }

    @Test
    void forecastUsesTheSameBalanceAndHorizon() {
        planRepo.save(FinancialPlan.starting(Money.of("1000"), TODAY));
        Forecast f = service.forecast(10, TODAY);
        assertEquals(11, f.days().size());
        assertEquals(Money.of("1000"), f.days().get(0).balance());
    }
}
