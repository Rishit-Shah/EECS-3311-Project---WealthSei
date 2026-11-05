package wealthsei.analytics;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Categories;
import wealthsei.domain.FixedBill;
import wealthsei.domain.Frequency;
import wealthsei.domain.GoalStatus;
import wealthsei.domain.IncomeSource;
import wealthsei.domain.Money;
import wealthsei.domain.RecurringPayment;
import wealthsei.domain.SavingsGoal;
import wealthsei.domain.Transaction;
import wealthsei.repository.InMemoryGoalRepository;
import wealthsei.service.GoalPlanner;
import wealthsei.service.ValidationException;

class ScenarioSimulatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);

    private final InMemoryGoalRepository goals = new InMemoryGoalRepository();
    private final ScenarioSimulator simulator = new ScenarioSimulator(new ForecastEngine(), goals, new GoalPlanner(),
            t -> baseline());

    private static RecurringPayment recurring(String merchant, String amount) {
        return new RecurringPayment(merchant, Money.of(amount), Frequency.MONTHLY, LocalDate.of(2026, 11, 10), 0.95,
                false, Money.ZERO, 4);
    }

    private static Transaction spend(String id, String date, String amount, wealthsei.domain.Category category) {
        return new Transaction(id, LocalDate.parse(date), Money.of(amount), "Shop", "SHOP", false, category);
    }

    /** 3000 now, 3000 income on the 15th, rent 1200 on the 1st, Spotify 10 and Netflix 20, dining 300 in each of the last 3 months. */
    private static ForecastInputs baseline() {
        List<Transaction> history = new ArrayList<>();
        int n = 0;
        for (int month = 8; month <= 10; month++) {
            history.add(spend("t" + n++, "2026-" + String.format("%02d", month) + "-12", "300", Categories.DINING));
        }
        return new ForecastInputs(TODAY, Money.of("3000"), history,
                List.of(recurring("Spotify", "10"), recurring("Netflix", "20")),
                List.of(new IncomeSource("Job", Money.of("3000"), Frequency.MONTHLY, LocalDate.of(2026, 11, 15))),
                List.of(new FixedBill("Rent", Money.of("1200"), Frequency.MONTHLY, LocalDate.of(2026, 12, 1), Categories.RENT)),
                List.of());
    }

    private static Scenario scenario(ScenarioChange... changes) {
        return new Scenario("s1", "test", List.of(changes));
    }

    @Test
    void cancellingARecurringPaymentRaisesMonthlySavingsByItsCost() {
        ScenarioResult r = simulator.simulate(scenario(new CancelRecurringChange("spotify")), TODAY);
        assertEquals(Money.of("10.00"), r.monthlySavingsDelta());
        assertEquals(Money.of("1770.00"), r.baselineMonthlySavings());
        assertEquals(Money.of("1780.00"), r.scenarioMonthlySavings());
        assertTrue(r.endBalanceDelta().isPositive());
        assertEquals(List.of("Cancel spotify"), r.changes());
        assertEquals(6, r.months());
    }

    @Test
    void aPartOfTheNameIsEnoughWhenItPicksOneItem() {
        ForecastInputs base = baseline();
        List<RecurringPayment> withDomain = new ArrayList<>(base.recurring());
        withDomain.set(1, recurring("Netflix.com", "20"));
        ForecastInputs real = new ForecastInputs(TODAY, base.openingBalance(), base.transactions(), withDomain,
                base.incomeSources(), base.fixedBills(), base.plannedExpenses());
        ScenarioResult r = simulator.simulate(scenario(new CancelRecurringChange("netflix")), real);
        assertEquals(Money.of("20.00"), r.monthlySavingsDelta());
    }

    @Test
    void aPartOfTheNameThatFitsSeveralItemsIsRefused() {
        ForecastInputs base = baseline();
        List<RecurringPayment> two = List.of(recurring("Spotify Family", "10"), recurring("Spotify Duo", "8"));
        ForecastInputs real = new ForecastInputs(TODAY, base.openingBalance(), base.transactions(), two,
                base.incomeSources(), base.fixedBills(), base.plannedExpenses());
        ValidationException e = assertThrows(ValidationException.class,
                () -> simulator.simulate(scenario(new CancelRecurringChange("spotify")), real));
        assertTrue(e.getMessage().contains("more than one"));
        assertTrue(e.getMessage().contains("Spotify Duo"));
    }

    @Test
    void cancellingABillFromThePlanWorksToo() {
        ScenarioResult r = simulator.simulate(scenario(new CancelRecurringChange("Rent")), TODAY);
        assertEquals(Money.of("1200.00"), r.monthlySavingsDelta());
    }

    @Test
    void cancellingSomethingThatDoesNotExistListsWhatDoes() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> simulator.simulate(scenario(new CancelRecurringChange("Hulu")), TODAY));
        assertTrue(e.getMessage().contains("Hulu"));
        assertTrue(e.getMessage().contains("Spotify"));
        assertTrue(e.getMessage().contains("Rent"));
    }

    @Test
    void anExtraIncomeLiftsSavingsAndTheEndBalance() {
        ScenarioResult r = simulator.simulate(scenario(new IncomeChange(Money.of("200"))), TODAY);
        assertEquals(Money.of("200.00"), r.monthlySavingsDelta());
        assertEquals(Money.of("1200.00"), r.endBalanceDelta());
    }

    @Test
    void capCategoryUsesTheAverageOfTheLastFullMonths() {
        ScenarioResult r = simulator.simulate(scenario(new CapCategoryChange(Categories.DINING, Money.of("150"))), TODAY);
        assertEquals(Money.of("150.00"), r.monthlySavingsDelta());
    }

    @Test
    void capAboveCurrentSpendingChangesNothing() {
        ScenarioResult r = simulator.simulate(scenario(new CapCategoryChange(Categories.DINING, Money.of("400"))), TODAY);
        assertEquals(Money.ZERO, r.monthlySavingsDelta());
        assertEquals(Money.ZERO, r.endBalanceDelta());
    }

    @Test
    void capOnACategoryWithNoHistorySavesNothing() {
        ScenarioResult r = simulator.simulate(scenario(new CapCategoryChange(Categories.SHOPPING, Money.of("50"))), TODAY);
        assertEquals(Money.ZERO, r.monthlySavingsDelta());
    }

    @Test
    void capsCannotBeNegative() {
        assertThrows(ValidationException.class, () -> new CapCategoryChange(Categories.DINING, Money.of("-1")));
    }

    @Test
    void aOneTimeExpenseLowersTheEndAndLowestBalances() {
        ScenarioResult r = simulator.simulate(scenario(new OneTimeExpenseChange(Money.of("600"), LocalDate.of(2026, 11, 8))), TODAY);
        assertEquals(Money.of("-600.00"), r.endBalanceDelta());
        assertEquals(Money.of("2370.00"), r.scenarioLowest());
        assertEquals(Money.ZERO, r.monthlySavingsDelta());
    }

    @Test
    void oneTimeExpenseDatesMustBeInsideTheSixMonths() {
        assertThrows(ValidationException.class, () -> simulator.simulate(
                scenario(new OneTimeExpenseChange(Money.of("10"), TODAY)), TODAY));
        assertThrows(ValidationException.class, () -> simulator.simulate(
                scenario(new OneTimeExpenseChange(Money.of("10"), TODAY.plusDays(181))), TODAY));
        assertThrows(ValidationException.class, () -> new OneTimeExpenseChange(Money.ZERO, TODAY.plusDays(1)));
    }

    @Test
    void changesAreAppliedTogether() {
        ScenarioResult r = simulator.simulate(scenario(new CancelRecurringChange("Netflix"),
                new IncomeChange(Money.of("100")), new CapCategoryChange(Categories.DINING, Money.of("200"))), TODAY);
        assertEquals(Money.of("220.00"), r.monthlySavingsDelta());
        assertEquals(3, r.changes().size());
    }

    @Test
    void theBaselineIsNeverChanged() {
        ForecastInputs base = baseline();
        simulator.simulate(scenario(new CancelRecurringChange("Spotify"), new IncomeChange(Money.of("500"))), base);
        assertEquals(2, base.recurring().size());
        assertEquals(1, base.incomeSources().size());
        assertEquals(baseline().openingBalance(), base.openingBalance());
    }

    @Test
    void goalDatesMoveWithTheSavings() {
        goals.save(new SavingsGoal("g1", "Trip", Money.of("6000"), LocalDate.of(2027, 12, 1), Money.ZERO, Money.ZERO,
                GoalStatus.AT_RISK));
        ScenarioResult r = simulator.simulate(scenario(new IncomeChange(Money.of("240"))), TODAY);
        assertEquals(1, r.goalImpacts().size());
        ScenarioResult.GoalImpact g = r.goalImpacts().get(0);
        assertEquals("Trip", g.goalName());
        assertEquals(TODAY.plusMonths(4), g.baselineCompletion());
        assertEquals(TODAY.plusMonths(3), g.scenarioCompletion());
        ScenarioResult slower = simulator.simulate(scenario(new OneTimeExpenseChange(Money.of("10"), TODAY.plusDays(5))), TODAY);
        assertEquals(TODAY.plusMonths(4), slower.goalImpacts().get(0).baselineCompletion());
    }

    @Test
    void goalsWithNoSurplusAreNotReached() {
        goals.save(new SavingsGoal("g1", "Yacht", Money.of("90000"), LocalDate.of(2027, 12, 1), Money.ZERO, Money.ZERO,
                GoalStatus.BEHIND));
        goals.save(new SavingsGoal("g2", "House", Money.of("90000"), LocalDate.of(2027, 12, 1), Money.ZERO, Money.of("5000"),
                GoalStatus.BEHIND));
        ScenarioResult r = simulator.simulate(scenario(new IncomeChange(Money.of("10"))), TODAY);
        ScenarioResult.GoalImpact yacht = r.goalImpacts().stream().filter(g -> g.goalName().equals("Yacht")).findFirst().orElseThrow();
        assertNull(yacht.baselineCompletion());
        assertNull(yacht.scenarioCompletion());
    }

    @Test
    void achievedGoalsAreLeftOut() {
        goals.save(new SavingsGoal("g1", "Done", Money.of("100"), LocalDate.of(2027, 12, 1), Money.of("100"), Money.ZERO,
                GoalStatus.ACHIEVED));
        assertTrue(simulator.simulate(scenario(new IncomeChange(Money.of("10"))), TODAY).goalImpacts().isEmpty());
    }

    @Test
    void scenariosNeedBetweenOneAndFiveChanges() {
        assertThrows(ValidationException.class, () -> new Scenario("s", "n", List.of()));
        ScenarioChange c = new IncomeChange(Money.of("1"));
        assertThrows(ValidationException.class, () -> new Scenario("s", "n", List.of(c, c, c, c, c, c)));
        assertEquals(5, new Scenario("s", "n", List.of(c, c, c, c, c)).changes().size());
    }

    @Test
    void incomeChangesMustBePositive() {
        assertThrows(ValidationException.class, () -> new IncomeChange(Money.of("-5")));
        assertThrows(ValidationException.class, () -> new CancelRecurringChange("  "));
    }
}
