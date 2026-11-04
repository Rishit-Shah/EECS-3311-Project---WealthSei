package wealthsei.facade;

import java.nio.file.Path;
import java.util.List;
import wealthsei.command.CategoryCorrector;
import wealthsei.importing.CsvImporter;
import wealthsei.repository.BudgetRepository;
import wealthsei.repository.InMemoryBudgetRepository;
import wealthsei.repository.InMemoryLearnedRuleRepository;
import wealthsei.repository.InMemoryPlanRepository;
import wealthsei.repository.InMemoryTransactionRepository;
import wealthsei.repository.LearnedRuleRepository;
import wealthsei.repository.PlanRepository;
import wealthsei.repository.TransactionRepository;
import wealthsei.repository.file.PersistentBudgetRepository;
import wealthsei.repository.file.PersistentLearnedRuleRepository;
import wealthsei.repository.file.PersistentPlanRepository;
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
                new InMemoryPlanRepository(), new InMemoryLearnedRuleRepository());
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
                new PersistentLearnedRuleRepository(new InMemoryLearnedRuleRepository(), dataDir.resolve("rules.tsv")));
    }

    private static WealthSeiFacade build(TransactionRepository txRepo, BudgetRepository budgetRepo,
                                         PlanRepository planRepo, LearnedRuleRepository rules) {
        AlertMonitor monitor = new AlertMonitor();
        Categorizer categorizer = new Categorizer(
                List.of(new LearnedRuleStrategy(rules), KeywordRuleStrategy.withDefaults()));
        TransactionService transactions = new TransactionService(txRepo, new CsvImporter(), categorizer);
        BudgetService budgets = new BudgetService(budgetRepo, txRepo, monitor);
        PlanService plans = new PlanService(planRepo);
        return new WealthSeiFacade(transactions, budgets, plans, monitor,
                new CategoryCorrector(transactions, rules));
    }
}
