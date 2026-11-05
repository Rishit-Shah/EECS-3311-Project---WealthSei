package wealthsei.facade;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.function.Supplier;
import wealthsei.agent.AgentController;
import wealthsei.agent.BudgetStatusTool;
import wealthsei.agent.ForecastTool;
import wealthsei.agent.GoalTool;
import wealthsei.agent.GroundingChecker;
import wealthsei.agent.InMemoryTraceRepository;
import wealthsei.agent.Planner;
import wealthsei.agent.PromptBuilder;
import wealthsei.agent.ResponseParser;
import wealthsei.agent.ScenarioTool;
import wealthsei.agent.ToolManager;
import wealthsei.agent.TransactionQueryTool;
import wealthsei.llm.LLMProvider;
import wealthsei.llm.LlmConfig;
import wealthsei.llm.UnavailableLLMProvider;
import java.util.List;
import wealthsei.analytics.AnalyticsService;
import wealthsei.analytics.ForecastEngine;
import wealthsei.analytics.ScenarioSimulator;
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
import wealthsei.repository.file.PersistentGoalRepository;
import wealthsei.repository.file.PersistentPlanRepository;
import wealthsei.repository.file.PersistentRecurringRepository;
import wealthsei.repository.file.PersistentTransactionRepository;
import wealthsei.service.AlertMonitor;
import wealthsei.service.GoalPlanner;
import wealthsei.service.GoalService;
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

    /** The most tool calls the agent may make for one question. */
    static final int MAX_AGENT_STEPS = 6;

    private WealthSeiFactory() {
    }

    /** Everything kept in memory: data is lost when the program exits. AI features are off. */
    public static WealthSeiFacade inMemory() {
        return inMemory(new UnavailableLLMProvider(), LlmConfig.none(), LocalDate::now);
    }

    /**
     * Everything in memory, with the given model (a scripted one in tests) and clock.
     *
     * @param today supplies the current date to the agent's tools (tests pass a fixed one)
     */
    public static WealthSeiFacade inMemory(LLMProvider provider, LlmConfig config, Supplier<LocalDate> today) {
        return build(new InMemoryTransactionRepository(), new InMemoryBudgetRepository(),
                new InMemoryPlanRepository(), new InMemoryRuleRepository(), new InMemoryRecurringRepository(),
                new InMemoryGoalRepository(), provider, config, today);
    }

    /** Saves to text files in {@code dataDir}; AI features are off. */
    public static WealthSeiFacade persistent(Path dataDir) {
        return persistent(dataDir, new UnavailableLLMProvider(), LlmConfig.none());
    }

    /**
     * Keeps everything in text files inside {@code dataDir} (created if missing), so data is
     * still there the next time the program starts.
     *
     * @throws wealthsei.repository.file.StorageException if an existing file cannot be read
     */
    public static WealthSeiFacade persistent(Path dataDir, LLMProvider provider, LlmConfig config) {
        return build(
                new PersistentTransactionRepository(new InMemoryTransactionRepository(), dataDir.resolve("transactions.tsv")),
                new PersistentBudgetRepository(new InMemoryBudgetRepository(), dataDir.resolve("budgets.tsv")),
                new PersistentPlanRepository(new InMemoryPlanRepository(), dataDir.resolve("plan.tsv")),
                new PersistentRuleRepository(new InMemoryRuleRepository(), dataDir.resolve("rules.tsv")),
                new PersistentRecurringRepository(new InMemoryRecurringRepository(), dataDir.resolve("recurring.tsv")),
                new PersistentGoalRepository(new InMemoryGoalRepository(), dataDir.resolve("goals.tsv")),
                provider, config, LocalDate::now);
    }

    private static WealthSeiFacade build(TransactionRepository txRepo, BudgetRepository budgetRepo,
                                         PlanRepository planRepo, RuleRepository rules,
                                         RecurringRepository recurring, GoalRepository goals,
                                         LLMProvider provider, LlmConfig config, Supplier<LocalDate> today) {
        AlertMonitor monitor = new AlertMonitor();
        LearnedRuleStrategy learned = new LearnedRuleStrategy(rules);
        List<wealthsei.service.CategorizationStrategy> chain = new java.util.ArrayList<>(
                List.of(learned, KeywordRuleStrategy.withDefaults()));
        if (config.hasKey()) {
            // Only with a key: the model is the last resort for merchants no rule knows.
            chain.add(new wealthsei.service.LLMCategorizationStrategy(provider, config.haikuModel()));
        }
        Categorizer categorizer = new Categorizer(chain);
        TransactionService transactions = new TransactionService(txRepo, new CsvImporter(), categorizer);
        BudgetService budgets = new BudgetService(budgetRepo, txRepo, monitor);
        PlanService plans = new PlanService(planRepo);
        AnalyticsService analytics = new AnalyticsService(txRepo, goals, recurring, planRepo, monitor,
                new RecurringDetector(), new ForecastEngine(), new SafeToSpendCalculator());
        GoalPlanner goalPlanner = new GoalPlanner();
        GoalService goalService = new GoalService(goals, goalPlanner, analytics, monitor);
        ToolManager tools = new ToolManager();
        tools.register(new ForecastTool(analytics, today));
        tools.register(new BudgetStatusTool(budgets, today));
        tools.register(new GoalTool(goalService, today));
        tools.register(new TransactionQueryTool(transactions));
        ScenarioSimulator simulator = new ScenarioSimulator(new ForecastEngine(), goals, goalPlanner, analytics::baselineInputs);
        tools.register(new ScenarioTool(simulator, today));
        Planner planner = new Planner(new PromptBuilder(config.sonnetModel()), new ResponseParser(), provider);
        AgentController agent = new AgentController(planner, tools, new GroundingChecker(),
                new InMemoryTraceRepository(), MAX_AGENT_STEPS, today);
        return new WealthSeiFacade(transactions, budgets, plans, monitor,
                new CategoryCorrector(transactions, learned), analytics, goalService, agent, simulator, today);
    }
}
