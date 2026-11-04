package wealthsei.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import wealthsei.domain.GoalOption;
import wealthsei.domain.GoalOptionKind;
import wealthsei.domain.GoalStatus;
import wealthsei.domain.Money;
import wealthsei.domain.SavingsGoal;

class GoalPlannerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);
    private final GoalPlanner planner = new GoalPlanner();

    private static SavingsGoal goal(String target, String saved, String deadline) {
        return new SavingsGoal("g1", "Laptop", Money.of(target), LocalDate.parse(deadline), Money.of(saved),
                Money.ZERO, GoalStatus.BEHIND);
    }

    @Test
    void monthsLeftRoundsPartMonthsUp() {
        assertEquals(1, planner.monthsLeft(TODAY.plusDays(1), TODAY));
        assertEquals(1, planner.monthsLeft(TODAY.plusDays(30), TODAY));
        assertEquals(2, planner.monthsLeft(TODAY.plusDays(31), TODAY));
        assertEquals(12, planner.monthsLeft(TODAY.plusYears(1), TODAY));
        assertEquals(13, planner.monthsLeft(TODAY.plusYears(1).plusDays(1), TODAY));
        assertEquals(0, planner.monthsLeft(TODAY, TODAY));
        assertEquals(0, planner.monthsLeft(TODAY.minusDays(5), TODAY));
    }

    @Test
    void requiredMonthlySplitsTheRemainingAmountEvenly() {
        SavingsGoal g = goal("1200", "0", "2027-11-04");
        assertEquals(Money.of("100.00"), planner.requiredMonthly(g, TODAY));
    }

    @Test
    void requiredMonthlyRoundsUpToTheCent() {
        SavingsGoal g = goal("100", "0", TODAY.plusDays(90).toString());
        assertEquals(Money.of("33.34"), planner.requiredMonthly(g, TODAY));
    }

    @Test
    void requiredMonthlyCountsWhatIsAlreadySaved() {
        SavingsGoal g = goal("1200", "600", "2027-05-04");
        assertEquals(Money.of("100.00"), planner.requiredMonthly(g, TODAY));
    }

    @Test
    void requiredMonthlyIsZeroWhenReachedAndAllOfItWhenLate() {
        assertEquals(Money.ZERO, planner.requiredMonthly(goal("500", "500", "2027-01-01"), TODAY));
        assertEquals(Money.of("400"), planner.requiredMonthly(goal("500", "100", "2026-10-01"), TODAY));
    }

    @Test
    void projectedCompletionUsesTheSurplus() {
        SavingsGoal g = goal("1000", "0", "2027-01-01");
        assertEquals(LocalDate.of(2027, 2, 4), planner.projectedCompletion(g, Money.of("400"), TODAY).orElseThrow());
        assertEquals(LocalDate.of(2027, 2, 4), planner.projectedCompletion(g, Money.of("334"), TODAY).orElseThrow());
    }

    @Test
    void projectedCompletionIsEmptyWithoutASurplus() {
        SavingsGoal g = goal("1000", "0", "2027-01-01");
        assertTrue(planner.projectedCompletion(g, Money.ZERO, TODAY).isEmpty());
        assertTrue(planner.projectedCompletion(g, Money.of("-50"), TODAY).isEmpty());
        assertEquals(TODAY, planner.projectedCompletion(goal("10", "10", "2027-01-01"), Money.ZERO, TODAY).orElseThrow());
    }

    @Test
    void feasibleGoalGetsOneKeepGoingOption() {
        SavingsGoal g = goal("1200", "0", "2027-11-04");
        List<GoalOption> options = planner.buildOptions(g, Money.of("250"), TODAY);
        assertEquals(1, options.size());
        assertEquals(GoalOptionKind.KEEP_PLAN, options.get(0).kind());
        assertTrue(options.get(0).impact().contains("150.00"));
    }

    @Test
    void infeasibleGoalGetsThreeWaysOut() {
        SavingsGoal g = goal("1200", "0", "2027-11-04");
        List<GoalOption> options = planner.buildOptions(g, Money.of("60"), TODAY);
        assertEquals(List.of(GoalOptionKind.INCREASE_SAVING, GoalOptionKind.EXTEND_DEADLINE,
                GoalOptionKind.REDUCE_TARGET), options.stream().map(GoalOption::kind).toList());
        assertTrue(options.get(0).description().contains("40.00"));
        assertTrue(options.get(1).description().contains("2028-07-04"));
        assertTrue(options.get(2).description().contains("720.00"));
    }

    @Test
    void noSurplusLeavesOnlyIncreasingSaving() {
        SavingsGoal g = goal("1200", "0", "2027-11-04");
        List<GoalOption> options = planner.buildOptions(g, Money.of("-20"), TODAY);
        assertEquals(1, options.size());
        assertEquals(GoalOptionKind.INCREASE_SAVING, options.get(0).kind());
        assertTrue(options.get(0).description().contains("100.00"));
    }

    @Test
    void reachedGoalNeedsNoOptions() {
        assertTrue(planner.buildOptions(goal("500", "500", "2027-01-01"), Money.of("100"), TODAY).isEmpty());
    }

    @Test
    void isFeasibleComparesSurplusToRequirement() {
        SavingsGoal g = goal("1200", "0", "2027-11-04");
        assertTrue(planner.isFeasible(g, Money.of("100"), TODAY));
        assertFalse(planner.isFeasible(g, Money.of("99.99"), TODAY));
    }
}
