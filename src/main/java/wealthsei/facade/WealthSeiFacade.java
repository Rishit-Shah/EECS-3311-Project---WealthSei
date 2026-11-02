package wealthsei.facade;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import wealthsei.command.CategoryCorrector;
import wealthsei.domain.BudgetLimits;
import wealthsei.domain.BudgetStatus;
import wealthsei.domain.Category;
import wealthsei.domain.FinancialPlan;
import wealthsei.domain.FixedBill;
import wealthsei.domain.ImportResult;
import wealthsei.domain.IncomeSource;
import wealthsei.domain.Money;
import wealthsei.domain.PlannedExpense;
import wealthsei.domain.Transaction;
import wealthsei.domain.TxFilter;
import wealthsei.service.AlertListener;
import wealthsei.service.AlertMonitor;
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

    public WealthSeiFacade(TransactionService transactions, BudgetService budgets, PlanService plans,
                           AlertMonitor alerts, CategoryCorrector corrector) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.budgets = Objects.requireNonNull(budgets, "budgets");
        this.plans = Objects.requireNonNull(plans, "plans");
        this.alerts = Objects.requireNonNull(alerts, "alerts");
        this.corrector = Objects.requireNonNull(corrector, "corrector");
    }

    // ---- F01 import ----

    /** Imports a bank CSV, then refreshes budget alerts for the affected months. */
    public ImportResult importTransactions(Path file) {
        ImportResult result = transactions.importFile(file);
        budgets.refreshAlerts(result.affectedMonths());
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

    // ---- alerts ----

    /** Registers a screen or console to be told about alerts (Observer pattern). */
    public void addAlertListener(AlertListener listener) {
        alerts.attach(listener);
    }
}
