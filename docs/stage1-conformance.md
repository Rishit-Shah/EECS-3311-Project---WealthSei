# Stage 1 conformance (generated from the Stage 1 report)

Stage 1 names 144 classes, interfaces and enums (diagrams plus the value-object table). As of 2026-11-04: **106 built with the Stage 1 name**, 1 built under a Java-style spelling, **37 still to build** (each belongs to a feature that is not implemented yet).

## Built under a different spelling

- `CSVParser` (Stage 1) is `CsvParser` in code (Java naming style).

## Still to build, by feature

- **F06/F07 Habits and Health Score:** `InsightTool`
- **F09 Goals:** `GoalsView`
- **F11 Monthly Review:** `MonthlyReview`, `ReviewService`, `ReviewRepository`
- **LLM layer:** `ChatModel`, `AnthropicChatModel`, `ExplanationService`, `AnalysisFacts`
- **F12 Agent and Chat:** `ConversationMemory`, `MemoryManager`, `MemoryRepository`, `UserProfileMemory`
- **F13 Proposals and Commands:** `Proposal`, `ProposalRepository`, `ProposalService`, `ProposalState`, `PendingState`, `AppliedState`, `RejectedState`, `ExpiredState`, `CommandFactory`, `SetBudgetLimitCommand`, `UpdateGoalContributionCommand`, `FlagSubscriptionCommand`
- **F14 Reports:** `ReportService`, `ReportData`, `ReportExporter`, `CsvExporter`, `MarkdownExporter`, `TextExporter`
- **GUI:** `MainWindow`, `DashboardView`, `AssistantView`, `TransactionsView`, `ProposalView`, `ReportView`

## Built with the Stage 1 name

`Account`, `AgentContext`, `AgentController`, `AgentResult`, `AgentStatus`, `AgentStep`, `AgentTask`, `AgentTrace`, `Alert`, `AlertListener`, `AlertMonitor`, `AlertSeverity`, `AlertType`, `AnalyticsService`, `BankCsvAdapter`, `Budget`, `BudgetLimits`, `BudgetLine`, `BudgetLineStatus`, `BudgetRepository`, `BudgetService`, `BudgetStatus`, `BudgetStatusTool`, `CancelRecurringChange`, `CapCategoryChange`, `CategorizationStrategy`, `Categorizer`, `Category`, `ClaudeProvider`, `CliApp`, `Command`, `CommandHistory`, `CsvImporter`, `DailyBalance`, `DebitCreditCsvAdapter`, `FeasibilityReport`, `FinalAnswerStep`, `Forecast`, `ForecastEngine`, `ForecastInputs`, `ForecastTool`, `Frequency`, `GoalOption`, `GoalPlanner`, `GoalRepository`, `GoalRequest`, `GoalService`, `GoalStatus`, `GoalTool`, `GroundingChecker`, `GroundingReport`, `HabitAnalyzer`, `HealthScore`, `HealthScoreCalculator`, `ImportResult`, `IncomeChange`, `Insight`, `KeywordRuleStrategy`, `LLMCategorizationStrategy`, `LLMProvider`, `LLMProviderDecorator`, `LLMRequest`, `LLMResponse`, `LearnedRule`, `LearnedRuleStrategy`, `LoggingLLMProvider`, `Message`, `MockLLMProvider`, `Money`, `OneTimeExpenseChange`, `ParseResult`, `Planner`, `PromptBuilder`, `ProposalDraft`, `RecategorizeCommand`, `RecurringDetector`, `RecurringPayment`, `RecurringRepository`, `ResponseParser`, `RetryingLLMProvider`, `RuleRepository`, `SafeToSpendCalculator`, `SafeToSpendResult`, `SavingsGoal`, `Scenario`, `ScenarioChange`, `ScenarioResult`, `ScenarioSimulator`, `ScenarioTool`, `ScoreComponent`, `SignedAmountCsvAdapter`, `TaskType`, `Tool`, `ToolCall`, `ToolCallStep`, `ToolManager`, `ToolResult`, `ToolSpec`, `TraceRepository`, `TraceStep`, `Transaction`, `TransactionQueryTool`, `TransactionRepository`, `TransactionService`, `TxFilter`, `WealthSeiFacade`


## Added beyond Stage 1

`AbstractCsvAdapter`, `AffordabilityFallback`, `ArgTokenizer`, `BudgetCalculator`, `CardMerchantCleaner`, `CardStatementCsvAdapter`, `Categories`, `CategoryCorrector`, `CsvFormatException`, `CsvRecord`, `FileCodec`, `FinancialPlan`, `FixedBill`, `GoalOptionKind`, `HealthScoreSummaryTemplate`, `HttpTransport`, `InMemoryBudgetRepository`, `InMemoryGoalRepository`, `InMemoryPlanRepository`, `InMemoryRecurringRepository`, `InMemoryRuleRepository`, `InMemoryTraceRepository`, `InMemoryTransactionRepository`, `IncomeSource`, `InsightType`, `InvalidRowException`, `JdkHttpTransport`, `Json`, `LLMException`, `LLMProviders`, `LlmConfig`, `Main`, `MonthlySurplusProvider`, `ParamSpec`, `ParamType`, `PersistentBudgetRepository`, `PersistentGoalRepository`, `PersistentPlanRepository`, `PersistentRecurringRepository`, `PersistentRuleRepository`, `PersistentTransactionRepository`, `PlanRepository`, `PlanService`, `PlannedExpense`, `PurchaseAssessor`, `PurchaseImpact`, `RecurringReport`, `RecurringSummaryTemplate`, `RowParsing`, `SafeToSpendSummaryTemplate`, `ScenarioParser`, `StorageException`, `TaskParams`, `ToolArgs`, `TransactionImportException`, `TsvFile`, `UnavailableLLMProvider`, `UnsupportedFormatException`, `ValidationException`, `Verdict`, `WealthSeiFactory`


What these extras are:
- **Plan and balance setup (F03):** `FinancialPlan`, `IncomeSource`, `FixedBill`, `PlannedExpense`, `PlanService`, `PlanRepository`. Stage 1 only had `Account` with an opening balance; the plan classes carry the balance, income, bills and safety buffer that the budget and Safe-to-Spend features need.
- **Storage:** `TsvFile`, `FileCodec`, `StorageException`, the `InMemory*` and `Persistent*` repositories (Decorator pattern). Stage 1 planned SQLite; the repository interfaces are unchanged.
- **Command line:** `CliApp`, `ArgTokenizer`, `Main`. Stage 1 planned picocli.
- **Wiring:** `WealthSeiFactory` (Factory pattern), `CategoryCorrector`.
- **Forecast helpers:** `SafeToSpendSummaryTemplate`; `DailyBalance` also carries the day's income and outgoing, and `SafeToSpendResult` also carries the balance, expected income, buffer, days left and over-commitment.
- **Small helpers:** `Categories`, `RecurringReport`, `RecurringSummaryTemplate`, the CSV exception types, `RowParsing`, `CsvRecord`, `AbstractCsvAdapter` (Template Method).
