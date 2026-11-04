package wealthsei.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Alert;
import wealthsei.domain.AlertSeverity;
import wealthsei.domain.AlertType;
import wealthsei.domain.FeasibilityReport;
import wealthsei.domain.GoalRequest;
import wealthsei.domain.GoalStatus;
import wealthsei.domain.Money;
import wealthsei.domain.SavingsGoal;
import wealthsei.repository.InMemoryGoalRepository;

class GoalServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);
    private final List<Alert> alerts = new ArrayList<>();
    private final AlertMonitor monitor = new AlertMonitor(() -> Instant.parse("2026-11-04T12:00:00Z"));
    private final AtomicInteger counter = new AtomicInteger();
    private Money surplus = Money.of("300");
    private final GoalService service = new GoalService(new InMemoryGoalRepository(), new GoalPlanner(),
            today -> surplus, monitor, () -> "id" + counter.incrementAndGet());

    GoalServiceTest() {
        monitor.attach(alerts::add);
    }

    private SavingsGoal laptop() {
        return service.createGoal(new GoalRequest("Laptop", Money.of("1200"), LocalDate.of(2027, 11, 4)), TODAY);
    }

    @Test
    void newGoalWithNoContributionIsAtRiskWhenTheSurplusCouldCoverIt() {
        SavingsGoal g = laptop();
        assertEquals("id1", g.id());
        assertEquals(GoalStatus.AT_RISK, g.status());
        assertEquals(1, alerts.size());
        assertEquals(AlertType.GOAL_RISK, alerts.get(0).type());
        assertEquals(AlertSeverity.WARNING, alerts.get(0).severity());
        assertTrue(alerts.get(0).message().contains("100.00"));
    }

    @Test
    void goalBecomesOnTrackWhenTheContributionCoversIt() {
        SavingsGoal g = laptop();
        SavingsGoal updated = service.updateContribution(g.id(), Money.of("100"), TODAY);
        assertEquals(GoalStatus.ON_TRACK, updated.status());
        assertEquals(GoalStatus.ON_TRACK, service.find(g.id()).status());
    }

    @Test
    void goalIsBehindWhenTheSurplusCannotCoverIt() {
        surplus = Money.of("40");
        SavingsGoal g = laptop();
        assertEquals(GoalStatus.BEHIND, g.status());
        assertEquals(AlertSeverity.OVER, alerts.get(0).severity());
    }

    @Test
    void noPlanMeansNoSurplusSoOnlyAContributionKeepsAGoalOnTrack() {
        surplus = Money.ZERO;
        SavingsGoal g = laptop();
        assertEquals(GoalStatus.BEHIND, g.status());
        assertEquals(GoalStatus.ON_TRACK, service.updateContribution(g.id(), Money.of("100"), TODAY).status());
    }

    @Test
    void alertIsRaisedOnlyWhenTheStatusGetsWorse() {
        SavingsGoal g = laptop();
        service.evaluate(g.id(), TODAY);
        service.evaluateAll(TODAY);
        assertEquals(1, alerts.size());
        surplus = Money.of("10");
        service.evaluateAll(TODAY);
        assertEquals(2, alerts.size());
        assertEquals(AlertSeverity.OVER, alerts.get(1).severity());
    }

    @Test
    void anAlertCanComeBackAfterTheGoalRecovered() {
        SavingsGoal g = laptop();
        service.updateContribution(g.id(), Money.of("100"), TODAY);
        service.updateContribution(g.id(), Money.ZERO, TODAY);
        assertEquals(2, alerts.size());
    }

    @Test
    void addingEnoughMoneyAchievesTheGoal() {
        SavingsGoal g = laptop();
        SavingsGoal done = service.addSaved(g.id(), Money.of("1200"), TODAY);
        assertEquals(GoalStatus.ACHIEVED, done.status());
        assertEquals(Money.of("1200.00"), done.saved());
    }

    @Test
    void aPastDeadlineIsBehindUnlessAchieved() {
        SavingsGoal g = laptop();
        assertEquals(GoalStatus.BEHIND, service.statusOf(g, LocalDate.of(2027, 12, 1)));
    }

    @Test
    void feasibilityUsesTheSurplusMinusOtherGoals() {
        SavingsGoal first = laptop();
        SavingsGoal second = service.createGoal(new GoalRequest("Trip", Money.of("600"), LocalDate.of(2027, 5, 4)), TODAY);
        service.updateContribution(second.id(), Money.of("250"), TODAY);
        FeasibilityReport report = service.feasibility(first.id(), TODAY);
        assertEquals(Money.of("50.00"), report.monthlySurplus());
        assertFalse(report.feasible());
        assertEquals(Money.of("100.00"), report.requiredMonthly());
        assertFalse(report.options().isEmpty());
    }

    @Test
    void anAchievedGoalDoesNotTakeFromTheSurplus() {
        SavingsGoal first = laptop();
        SavingsGoal second = service.createGoal(new GoalRequest("Trip", Money.of("600"), LocalDate.of(2027, 5, 4)), TODAY);
        service.updateContribution(second.id(), Money.of("250"), TODAY);
        service.addSaved(second.id(), Money.of("600"), TODAY);
        assertEquals(Money.of("300"), service.feasibility(first.id(), TODAY).monthlySurplus());
    }

    @Test
    void createGoalChecksItsInput() {
        assertThrows(ValidationException.class, () -> service.createGoal(
                new GoalRequest(" ", Money.of("10"), LocalDate.of(2027, 1, 1)), TODAY));
        assertThrows(ValidationException.class, () -> service.createGoal(
                new GoalRequest("X", Money.ZERO, LocalDate.of(2027, 1, 1)), TODAY));
        assertThrows(ValidationException.class, () -> service.createGoal(
                new GoalRequest("X", Money.of("10"), LocalDate.of(2027, 1, 1), Money.of("-1")), TODAY));
        assertThrows(ValidationException.class, () -> service.createGoal(
                new GoalRequest("X", Money.of("10"), TODAY), TODAY));
    }

    @Test
    void updatesCheckTheirInputAndTheId() {
        SavingsGoal g = laptop();
        assertThrows(ValidationException.class, () -> service.updateContribution(g.id(), Money.of("-1"), TODAY));
        assertThrows(ValidationException.class, () -> service.addSaved(g.id(), Money.ZERO, TODAY));
        assertThrows(ValidationException.class, () -> service.find("missing"));
    }

    @Test
    void goalsAreListedInTheOrderTheyWereCreated() {
        laptop();
        service.createGoal(new GoalRequest("Trip", Money.of("600"), LocalDate.of(2027, 5, 4)), TODAY);
        assertEquals(List.of("Laptop", "Trip"), service.goals().stream().map(SavingsGoal::name).toList());
    }
}
