package wealthsei.analytics;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import wealthsei.domain.FinancialPlan;
import wealthsei.domain.BudgetStatus;
import wealthsei.domain.Forecast;
import wealthsei.domain.HealthScore;
import wealthsei.domain.Insight;
import wealthsei.domain.Money;
import wealthsei.domain.PlannedExpense;
import wealthsei.domain.PurchaseImpact;
import wealthsei.domain.RecurringPayment;
import wealthsei.domain.SafeToSpendResult;
import wealthsei.domain.SavingsGoal;
import wealthsei.domain.Transaction;
import wealthsei.domain.TxFilter;
import wealthsei.repository.GoalRepository;
import wealthsei.repository.PlanRepository;
import wealthsei.repository.RecurringRepository;
import wealthsei.repository.TransactionRepository;
import wealthsei.service.AlertMonitor;
import wealthsei.service.MonthlySurplusProvider;
import wealthsei.service.ValidationException;

/**
 * Looks at the stored history and the user's plan and answers questions about them. Everything
 * here is computed by ordinary code; a language model, when added, only phrases the result.
 */
public class AnalyticsService implements MonthlySurplusProvider {

    /** How many days ahead the safe-to-spend forecast looks. */
    public static final int FORECAST_DAYS = 30;

    private final TransactionRepository txRepo;
    private final GoalRepository goalRepo;
    private final RecurringRepository flags;
    private final PlanRepository planRepo;
    private final AlertMonitor monitor;
    private final RecurringDetector detector;
    private final ForecastEngine engine;
    private final SafeToSpendCalculator calculator;
    private final HabitAnalyzer habitAnalyzer;
    private final HealthScoreCalculator scoreCalculator;

    public AnalyticsService(TransactionRepository txRepo, GoalRepository goalRepo, RecurringRepository flags,
                            PlanRepository planRepo, AlertMonitor monitor, RecurringDetector detector,
                            ForecastEngine engine, SafeToSpendCalculator calculator) {
        this(txRepo, goalRepo, flags, planRepo, monitor, detector, engine, calculator, new HabitAnalyzer(),
                new HealthScoreCalculator());
    }

    public AnalyticsService(TransactionRepository txRepo, GoalRepository goalRepo, RecurringRepository flags,
                            PlanRepository planRepo, AlertMonitor monitor, RecurringDetector detector,
                            ForecastEngine engine, SafeToSpendCalculator calculator, HabitAnalyzer habitAnalyzer,
                            HealthScoreCalculator scoreCalculator) {
        this.txRepo = Objects.requireNonNull(txRepo, "txRepo");
        this.goalRepo = Objects.requireNonNull(goalRepo, "goalRepo");
        this.flags = Objects.requireNonNull(flags, "flags");
        this.planRepo = Objects.requireNonNull(planRepo, "planRepo");
        this.monitor = Objects.requireNonNull(monitor, "monitor");
        this.detector = Objects.requireNonNull(detector, "detector");
        this.engine = Objects.requireNonNull(engine, "engine");
        this.calculator = Objects.requireNonNull(calculator, "calculator");
        this.habitAnalyzer = Objects.requireNonNull(habitAnalyzer, "habitAnalyzer");
        this.scoreCalculator = Objects.requireNonNull(scoreCalculator, "scoreCalculator");
    }

    // ---- F04 recurring payments ----

    /** Recurring payments as of {@code asOf}, with the user's review flags applied. */
    public List<RecurringPayment> detectRecurring(LocalDate asOf) {
        return detector.detect(txRepo.query(TxFilter.all()), asOf).stream()
                .map(p -> p.withFlag(flags.isFlagged(p.merchant())))
                .toList();
    }

    /** The recurring-payments panel: the list, the monthly total and a summary. */
    public RecurringReport recurringReport(LocalDate asOf) {
        List<RecurringPayment> payments = detectRecurring(asOf);
        Money total = payments.stream().map(RecurringPayment::monthlyCost).reduce(Money.ZERO, Money::add);
        boolean hasTransactions = !txRepo.query(TxFilter.all()).isEmpty();
        return new RecurringReport(payments, total,
                RecurringSummaryTemplate.write(payments, total, hasTransactions));
    }

    /**
     * Marks a recurring payment for review, or clears the mark.
     *
     * @throws ValidationException if no recurring payment with that merchant name is found
     */
    public void flagRecurring(String merchant, boolean flagged, LocalDate asOf) {
        Objects.requireNonNull(merchant, "merchant");
        boolean known = detectRecurring(asOf).stream()
                .anyMatch(p -> RecurringRepository.keyOf(p.merchant()).equals(RecurringRepository.keyOf(merchant)));
        if (!known) {
            throw new ValidationException("No recurring payment found for '" + merchant + "'");
        }
        flags.saveFlag(merchant, flagged);
    }

    // ---- F05 forecast and safe-to-spend ----

    /** The projected balance for the next {@code days} days, starting today. */
    public Forecast forecast(int days) {
        return forecast(days, LocalDate.now());
    }

    /**
     * @throws ValidationException if no plan has been set up yet
     */
    public Forecast forecast(int days, LocalDate today) {
        return engine.project(inputsFor(requirePlan(), today), days);
    }

    /**
     * How much can be spent per day for the rest of the month. Raises a LOW_BALANCE alert when
     * the projected balance falls below the safety buffer.
     *
     * @throws ValidationException if no plan has been set up yet
     */
    public SafeToSpendResult safeToSpend(LocalDate today) {
        FinancialPlan plan = requirePlan();
        ForecastInputs inputs = inputsFor(plan, today);
        Forecast forecast = engine.project(inputs, FORECAST_DAYS);
        List<SavingsGoal> goals = goalRepo.findAll();
        SafeToSpendResult result = calculator.calculate(forecast, goals, plan.safetyBuffer());
        monitor.evaluateLowBalance(forecast.lowest(), plan.safetyBuffer());
        boolean hasTransactions = !inputs.transactions().isEmpty();
        return result.withExplanation(SafeToSpendSummaryTemplate.write(result, forecast, hasTransactions));
    }

    // ---- F08 purchase impact ----

    /**
     * What buying something would do to the next month: the lowest balance and the daily
     * allowance with and without it, and the verdict the rules give.
     *
     * @param date when it would be bought (today or later)
     * @throws ValidationException if there is no plan, the price is not above zero, or the date is past
     */
    public PurchaseImpact purchaseImpact(String description, Money price, LocalDate date, LocalDate today) {
        if (description == null || description.isBlank()) {
            throw new ValidationException("Say what you want to buy");
        }
        if (price == null || !price.isPositive()) {
            throw new ValidationException("The price must be more than zero");
        }
        if (date.isBefore(today)) {
            throw new ValidationException("The purchase date cannot be in the past");
        }
        FinancialPlan plan = requirePlan();
        ForecastInputs base = inputsFor(plan, today);
        int days = (int) Math.min(365, Math.max(FORECAST_DAYS, java.time.temporal.ChronoUnit.DAYS.between(today, date) + 1));
        Forecast before = engine.project(base, days);
        ForecastInputs changed;
        if (date.equals(today)) {
            changed = base.withOpeningBalance(base.openingBalance().subtract(price));
        } else {
            List<PlannedExpense> expenses = new java.util.ArrayList<>(base.plannedExpenses());
            expenses.add(new PlannedExpense(description.trim(), price, date, null));
            changed = base.withPlannedExpenses(expenses);
        }
        Forecast after = engine.project(changed, days);
        List<SavingsGoal> goals = goalRepo.findAll();
        SafeToSpendResult allowanceBefore = calculator.calculate(before, goals, plan.safetyBuffer());
        SafeToSpendResult allowanceAfter = calculator.calculate(after, goals, plan.safetyBuffer());
        int needHelp = (int) goals.stream().filter(g -> g.status() == wealthsei.domain.GoalStatus.AT_RISK
                || g.status() == wealthsei.domain.GoalStatus.BEHIND).count();
        PurchaseAssessor.Assessment verdict = PurchaseAssessor.assess(after.lowestBalance(), plan.safetyBuffer(),
                allowanceBefore.dailyAllowance(), allowanceAfter.dailyAllowance(), allowanceAfter.isOverCommitted(),
                needHelp);
        return new PurchaseImpact(description.trim(), price, date, before.lowest(), after.lowest(),
                allowanceBefore.dailyAllowance(), allowanceAfter.dailyAllowance(), plan.safetyBuffer(), needHelp,
                verdict.verdict(), verdict.reason());
    }

    // ---- F06 habit detective ----

    /** Days of history needed before habits are reported. */
    public static final int MIN_HABIT_DAYS = 30;

    /**
     * Habits found in {@code month}.
     *
     * @return the insights, empty when there are none
     * @throws ValidationException if the history spans fewer than {@value #MIN_HABIT_DAYS} days
     */
    public List<Insight> habits(YearMonth month) {
        List<Transaction> all = txRepo.query(TxFilter.all());
        if (!hasEnoughHistory(all)) {
            throw new ValidationException("Need more data: habits need at least " + MIN_HABIT_DAYS
                    + " days of transactions. Import more history first.");
        }
        return habitAnalyzer.analyze(all, month);
    }

    private static boolean hasEnoughHistory(List<Transaction> all) {
        if (all.isEmpty()) {
            return false;
        }
        LocalDate first = all.stream().map(Transaction::date).min(LocalDate::compareTo).orElseThrow();
        LocalDate last = all.stream().map(Transaction::date).max(LocalDate::compareTo).orElseThrow();
        return java.time.temporal.ChronoUnit.DAYS.between(first, last) + 1 >= MIN_HABIT_DAYS;
    }

    // ---- F07 health score ----

    /**
     * The financial health score for {@code month}. Parts that cannot be measured (no budget,
     * no plan, no goals) are left out and the rest re-scaled.
     *
     * @param status the month's budget status, or null if no budget is set
     * @param today  used for the savings and buffer parts, which look 30 days ahead
     */
    public HealthScore healthScore(BudgetStatus status, LocalDate today) {
        Forecast forecast = planRepo.find().map(p -> engine.project(inputsFor(p, today), FORECAST_DAYS)).orElse(null);
        HealthScore score = scoreCalculator.calculate(status, forecast, goalRepo.findAll());
        return score.withExplanation(HealthScoreSummaryTemplate.write(score));
    }

    /** The starting point for what-if scenarios: the balance, plan and history as of {@code today}. */
    public ForecastInputs baselineInputs(LocalDate today) {
        return inputsFor(requirePlan(), today);
    }

    /** Income minus bills and uncovered recurring payments per month; zero when there is no plan. */
    @Override
    public Money monthlySurplus(LocalDate today) {
        return planRepo.find().map(p -> engine.monthlySurplus(inputsFor(p, today))).orElse(Money.ZERO);
    }

    private FinancialPlan requirePlan() {
        return planRepo.find().orElseThrow(() ->
                new ValidationException("No plan yet. Set your balance first: plan balance <amount>"));
    }

    /**
     * The balance today is the plan's balance plus every transaction dated after the balance
     * date, up to and including today.
     */
    private ForecastInputs inputsFor(FinancialPlan plan, LocalDate today) {
        List<Transaction> history = txRepo.query(TxFilter.all());
        Money balance = plan.currentBalance();
        for (Transaction tx : history) {
            if (tx.date().isAfter(plan.balanceAsOf()) && !tx.date().isAfter(today)) {
                balance = balance.add(tx.signedAmount());
            }
        }
        return new ForecastInputs(today, balance, history, detectRecurring(today), plan.incomeSources(),
                plan.fixedBills(), plan.plannedExpenses());
    }
}
