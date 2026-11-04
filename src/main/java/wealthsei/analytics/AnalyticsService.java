package wealthsei.analytics;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import wealthsei.domain.FinancialPlan;
import wealthsei.domain.Forecast;
import wealthsei.domain.Money;
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
import wealthsei.service.ValidationException;

/**
 * Looks at the stored history and the user's plan and answers questions about them. Everything
 * here is computed by ordinary code; a language model, when added, only phrases the result.
 */
public class AnalyticsService {

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

    public AnalyticsService(TransactionRepository txRepo, GoalRepository goalRepo, RecurringRepository flags,
                            PlanRepository planRepo, AlertMonitor monitor, RecurringDetector detector,
                            ForecastEngine engine, SafeToSpendCalculator calculator) {
        this.txRepo = Objects.requireNonNull(txRepo, "txRepo");
        this.goalRepo = Objects.requireNonNull(goalRepo, "goalRepo");
        this.flags = Objects.requireNonNull(flags, "flags");
        this.planRepo = Objects.requireNonNull(planRepo, "planRepo");
        this.monitor = Objects.requireNonNull(monitor, "monitor");
        this.detector = Objects.requireNonNull(detector, "detector");
        this.engine = Objects.requireNonNull(engine, "engine");
        this.calculator = Objects.requireNonNull(calculator, "calculator");
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
