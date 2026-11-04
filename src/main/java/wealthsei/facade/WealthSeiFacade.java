package wealthsei.facade;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import wealthsei.analytics.AnalyticsService;
import wealthsei.analytics.RecurringReport;
import wealthsei.command.CategoryCorrector;
import wealthsei.domain.BudgetLimits;
import wealthsei.domain.BudgetStatus;
import wealthsei.domain.FeasibilityReport;
import wealthsei.domain.GoalRequest;
import wealthsei.domain.SavingsGoal;
import wealthsei.domain.Category;
import wealthsei.domain.FinancialPlan;
import wealthsei.domain.FixedBill;
import wealthsei.domain.Forecast;
import wealthsei.domain.ImportResult;
import wealthsei.domain.IncomeSource;
import wealthsei.domain.Money;
import wealthsei.domain.PlannedExpense;
import wealthsei.domain.SafeToSpendResult;
import wealthsei.domain.Transaction;
import wealthsei.domain.TxFilter;
import wealthsei.service.AlertListener;
import wealthsei.service.AlertMonitor;
import wealthsei.service.GoalService;
import wealthsei.service.ValidationException;
import wealthsei.service.BudgetService;
import wealthsei.service.PlanService;
import wealthsei.service.TransactionService;

/**
 * The single entry point the GUI and the CLI call (Facade pattern). It hides which service
 * does what, and ties them together: after an import it refreshes budget alerts for the
 * months that received new transactions.
 *
 * <p>Services are passed in, so there is no global state and tests can wire in-memory parts.
 */
public class WealthSeiFacade {

    private final TransactionService transactions;
    private final BudgetService budgets;
    private final PlanService plans;
    private final AlertMonitor alerts;
    private final CategoryCorrector corrector;
    private final AnalyticsService analytics;
    private final GoalService goals;

    public WealthSeiFacade(TransactionService transactions, BudgetService budgets, PlanService plans,
                           AlertMonitor alerts, CategoryCorrector corrector, AnalyticsService analytics,
                           GoalService goals) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.budgets = Objects.requireNonNull(budgets, "budgets");
        this.plans = Objects.requireNonNull(plans, "plans");
        this.alerts = Objects.requireNonNull(alerts, "alerts");
        this.corrector = Objects.requireNonNull(corrector, "corrector");
        this.analytics = Objects.requireNonNull(analytics, "analytics");
        this.goals = Objects.requireNonNull(goals, "goals");
    }

    // ---- F01 import ----

    /** Imports a bank CSV, then refreshes budget alerts for the affected months and re-checks the goals. */
    public ImportResult importTransactions(Path file) {
        return importTransactions(file, LocalDate.now());
    }

    public ImportResult importTransactions(Path file, LocalDate today) {
        ImportResult result = transactions.importFile(file);
        budgets.refreshAlerts(result.affectedMonths());
        goals.evaluateAll(today);
        return result;
    }

    public List<Transaction> listTransactions(TxFilter filter) {
        return transactions.query(filter);
    }

    /** Changes a category, remembers the merchant for future imports, and refreshes alerts. */
    public Transaction correctCategory(String txId, Category category) {
        Transaction changed = corrector.correct(txId, category);
        refreshMonthOf(changed);
        return changed;
    }

    /** Reverses the latest category correction; empty when there is nothing to undo. */
    public Optional<Transaction> undoCorrection() {
        Optional<Transaction> restored = corrector.undo();
        restored.ifPresent(this::refreshMonthOf);
        return restored;
    }

    public Optional<Transaction> redoCorrection() {
        Optional<Transaction> redone = corrector.redo();
        redone.ifPresent(this::refreshMonthOf);
        return redone;
    }

    public boolean canUndoCorrection() {
        return corrector.canUndo();
    }

    private void refreshMonthOf(Transaction tx) {
        budgets.refreshAlerts(java.util.Set.of(YearMonth.from(tx.date())));
    }

    // ---- F03 plan and budget ----

    public Optional<FinancialPlan> getPlan() {
        return plans.getPlan();
    }

    public FinancialPlan savePlan(FinancialPlan plan) {
        return plans.savePlan(plan);
    }

    public FinancialPlan setBalance(Money balance, LocalDate asOf) {
        return plans.setBalance(balance, asOf);
    }

    public FinancialPlan setSafetyBuffer(Money buffer) {
        return plans.setSafetyBuffer(buffer);
    }

    public FinancialPlan addIncome(IncomeSource income) {
        return plans.addIncome(income);
    }

    public FinancialPlan addFixedBill(FixedBill bill) {
        return plans.addFixedBill(bill);
    }

    public FinancialPlan addPlannedExpense(PlannedExpense expense) {
        return plans.addPlannedExpense(expense);
    }

    public BudgetStatus setBudget(YearMonth month, BudgetLimits limits) {
        return budgets.setBudget(month, limits);
    }

    public BudgetStatus setBudgetLimit(YearMonth month, Category category, Money limit) {
        return budgets.setLimit(month, category, limit);
    }

    public BudgetStatus getBudgetStatus(YearMonth month) {
        return budgets.getStatus(month);
    }

    // ---- F04 recurring payments ----

    /** Recurring payments as of {@code asOf}, their monthly cost and a plain-language summary. */
    public RecurringReport recurringPayments(LocalDate asOf) {
        return analytics.recurringReport(asOf);
    }

    /** Marks a recurring payment for review (or clears the mark). */
    public void flagRecurring(String merchant, boolean flagged, LocalDate asOf) {
        analytics.flagRecurring(merchant, flagged, asOf);
    }

    // ---- F05 safe-to-spend ----

    /** What can be spent per day for the rest of the month; may raise a low-balance alert. */
    public SafeToSpendResult getSafeToSpend(LocalDate today) {
        return analytics.safeToSpend(today);
    }

    /** The projected balance for the next {@code days} days. */
    public Forecast getForecast(int days, LocalDate today) {
        return analytics.forecast(days, today);
    }

    // ---- F06 habits and F07 score ----

    /** Spending habits found in the month; fails with 'need more data' if under 30 days of history. */
    public java.util.List<wealthsei.domain.Insight> getHabits(YearMonth month) {
        return analytics.habits(month);
    }

    /** The health score for the month; parts without data (no budget, plan or goals) are left out. */
    public wealthsei.domain.HealthScore getHealthScore(YearMonth month, LocalDate today) {
        goals.evaluateAll(today);
        BudgetStatus status = budgets.hasBudget(month) ? budgets.getStatus(month) : null;
        return analytics.healthScore(status, today);
    }

    // ---- F09 goals ----

    /** Creates a goal and works out whether it is on track. */
    public SavingsGoal addGoal(GoalRequest request, LocalDate today) {
        return goals.createGoal(request, today);
    }

    public List<SavingsGoal> getGoals() {
        return goals.goals();
    }

    /** Can the goal be reached on time, and what to change if not. Accepts a full id or a unique prefix. */
    public FeasibilityReport getGoalFeasibility(String idOrPrefix, LocalDate today) {
        return goals.feasibility(resolveGoalId(idOrPrefix), today);
    }

    public SavingsGoal setGoalMonthlyContribution(String idOrPrefix, Money monthly, LocalDate today) {
        return goals.updateContribution(resolveGoalId(idOrPrefix), monthly, today);
    }

    public SavingsGoal addToGoal(String idOrPrefix, Money amount, LocalDate today) {
        return goals.addSaved(resolveGoalId(idOrPrefix), amount, today);
    }

    private String resolveGoalId(String typed) {
        List<String> matches = goals.goals().stream().map(SavingsGoal::id).filter(id -> id.startsWith(typed)).toList();
        if (matches.size() == 1) {
            return matches.get(0);
        }
        if (matches.isEmpty()) {
            throw new ValidationException("No goal with id '" + typed + "'. Type 'goal list' to see ids.");
        }
        throw new ValidationException("'" + typed + "' matches " + matches.size() + " goals; type more characters.");
    }

    // ---- alerts ----

    /** Registers a screen or console to be told about alerts (Observer pattern). */
    public void addAlertListener(AlertListener listener) {
        alerts.attach(listener);
    }
}
