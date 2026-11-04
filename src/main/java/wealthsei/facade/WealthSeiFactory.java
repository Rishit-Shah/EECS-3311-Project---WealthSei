package wealthsei.facade;

import java.nio.file.Path;
import java.util.List;
import wealthsei.analytics.AnalyticsService;
import wealthsei.analytics.ForecastEngine;
import wealthsei.analytics.RecurringDetector;
import wealthsei.analytics.SafeToSpendCalculator;
import wealthsei.command.CategoryCorrector;
import wealthsei.importing.CsvImporter;
import wealthsei.repository.BudgetRepository;
import wealthsei.repository.GoalRepository;
import wealthsei.repository.InMemoryBudgetRepository;
import wealthsei.repository.InMemoryGoalRepository;
import wealthsei.repository.InMemoryRuleRepository;
import wealthsei.repository.InMemoryPlanRepository;
import wealthsei.repository.InMemoryTransactionRepository;
import wealthsei.repository.RuleRepository;
import wealthsei.repository.InMemoryRecurringRepository;
import wealthsei.repository.PlanRepository;
import wealthsei.repository.RecurringRepository;
import wealthsei.repository.TransactionRepository;
import wealthsei.repository.file.PersistentBudgetRepository;
import wealthsei.repository.file.PersistentRuleRepository;
import wealthsei.repository.file.PersistentPlanRepository;
import wealthsei.repository.file.PersistentRecurringRepository;
import wealthsei.repository.file.PersistentTransactionRepository;
import wealthsei.service.AlertMonitor;
import wealthsei.service.BudgetService;
import wealthsei.service.Categorizer;
import wealthsei.service.KeywordRuleStrategy;
import wealthsei.service.LearnedRuleStrategy;
import wealthsei.service.PlanService;
import wealthsei.service.TransactionService;

/**
 * Builds a fully wired {@link WealthSeiFacade} (Factory pattern), so the CLI, the GUI and the
 * tests never repeat the wiring. The only place that knows which repositories are used.
 */
public final class WealthSeiFactory {

    private WealthSeiFactory() {
    }

    /** Everything kept in memory: data is lost when the program exits. */
    public static WealthSeiFacade inMemory() {
        return build(new InMemoryTransactionRepository(), new InMemoryBudgetRepository(),
                new InMemoryPlanRepository(), new InMemoryRuleRepository(), new InMemoryRecurringRepository());
    }

    /**
     * Keeps everything in text files inside {@code dataDir} (created if missing), so data is
     * still there the next time the program starts.
     *
     * @throws wealthsei.repository.file.StorageException if an existing file cannot be read
     */
    public static WealthSeiFacade persistent(Path dataDir) {
        return build(
                new PersistentTransactionRepository(new InMemoryTransactionRepository(), dataDir.resolve("transactions.tsv")),
                new PersistentBudgetRepository(new InMemoryBudgetRepository(), dataDir.resolve("budgets.tsv")),
                new PersistentPlanRepository(new InMemoryPlanRepository(), dataDir.resolve("plan.tsv")),
                new PersistentRuleRepository(new InMemoryRuleRepository(), dataDir.resolve("rules.tsv")),
                new PersistentRecurringRepository(new InMemoryRecurringRepository(), dataDir.resolve("recurring.tsv")));
    }

    private static WealthSeiFacade build(TransactionRepository txRepo, BudgetRepository budgetRepo,
                                         PlanRepository planRepo, RuleRepository rules,
                                         RecurringRepository recurring) {
        AlertMonitor monitor = new AlertMonitor();
        LearnedRuleStrategy learned = new LearnedRuleStrategy(rules);
        Categorizer categorizer = new Categorizer(List.of(learned, KeywordRuleStrategy.withDefaults()));
        TransactionService transactions = new TransactionService(txRepo, new CsvImporter(), categorizer);
        BudgetService budgets = new BudgetService(budgetRepo, txRepo, monitor);
        PlanService plans = new PlanService(planRepo);
        GoalRepository goals = new InMemoryGoalRepository();
        AnalyticsService analytics = new AnalyticsService(txRepo, goals, recurring, planRepo, monitor,
                new RecurringDetector(), new ForecastEngine(), new SafeToSpendCalculator());
        return new WealthSeiFacade(transactions, budgets, plans, monitor,
                new CategoryCorrector(transactions, learned), analytics);
    }
}
