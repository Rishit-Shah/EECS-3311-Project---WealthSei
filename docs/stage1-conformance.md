# Stage 1 conformance (generated from the Stage 1 report)

Stage 1 names 144 classes, interfaces and enums (diagrams plus the value-object table). As of 2026-11-04: **54 built with the Stage 1 name**, 1 built under a Java-style spelling, **89 still to build** (each belongs to a feature that is not implemented yet).

## Built under a different spelling

- `CSVParser` (Stage 1) is `CsvParser` in code (Java naming style).

## Still to build, by feature

- **F02 (rest):** `LLMCategorizationStrategy`
- **F06/F07 Habits and Health Score:** `InsightTool`
- **F08/F10 Affordability and What-If:** `ScenarioSimulator`, `Scenario`, `ScenarioChange`, `ScenarioResult`, `IncomeChange`, `OneTimeExpenseChange`, `CapCategoryChange`, `CancelRecurringChange`, `ScenarioTool`, `GoalRequest`, `FeasibilityReport`, `GoalOption`, `ForecastTool`
- **F09 Goals:** `GoalPlanner`, `GoalService`, `GoalTool`, `GoalsView`
- **F11 Monthly Review:** `MonthlyReview`, `ReviewService`, `ReviewRepository`
- **LLM layer:** `LLMProvider`, `ClaudeProvider`, `ChatModel`, `AnthropicChatModel`, `MockLLMProvider`, `LLMProviderDecorator`, `RetryingLLMProvider`, `LoggingLLMProvider`, `LLMRequest`, `LLMResponse`, `ExplanationService`, `PromptBuilder`, `ResponseParser`, `AnalysisFacts`
- **F12 Agent and Chat:** `AgentContext`, `AgentController`, `AgentResult`, `AgentStatus`, `AgentStep`, `AgentTask`, `AgentTrace`, `Planner`, `Tool`, `ToolCall`, `ToolManager`, `ToolResult`, `ToolSpec`, `TransactionQueryTool`, `BudgetStatusTool`, `GroundingChecker`, `GroundingReport`, `ConversationMemory`, `MemoryManager`, `MemoryRepository`, `UserProfileMemory`, `TraceRepository`, `TraceStep`, `ToolCallStep`, `FinalAnswerStep`, `TaskType`, `Message`
- **F13 Proposals and Commands:** `Proposal`, `ProposalDraft`, `ProposalRepository`, `ProposalService`, `ProposalState`, `PendingState`, `AppliedState`, `RejectedState`, `ExpiredState`, `CommandFactory`, `SetBudgetLimitCommand`, `UpdateGoalContributionCommand`, `FlagSubscriptionCommand`
- **F14 Reports:** `ReportService`, `ReportData`, `ReportExporter`, `CsvExporter`, `MarkdownExporter`, `TextExporter`
- **GUI:** `MainWindow`, `DashboardView`, `AssistantView`, `TransactionsView`, `ProposalView`, `ReportView`
- **Domain (still to add):** `Account`

## Built with the Stage 1 name

`Alert`, `AlertListener`, `AlertMonitor`, `AlertSeverity`, `AlertType`, `AnalyticsService`, `BankCsvAdapter`, `Budget`, `BudgetLimits`, `BudgetLine`, `BudgetLineStatus`, `BudgetRepository`, `BudgetService`, `BudgetStatus`, `CategorizationStrategy`, `Categorizer`, `Category`, `CliApp`, `Command`, `CommandHistory`, `CsvImporter`, `DailyBalance`, `DebitCreditCsvAdapter`, `Forecast`, `ForecastEngine`, `ForecastInputs`, `Frequency`, `GoalRepository`, `GoalStatus`, `HabitAnalyzer`, `HealthScore`, `HealthScoreCalculator`, `ImportResult`, `Insight`, `KeywordRuleStrategy`, `LearnedRule`, `LearnedRuleStrategy`, `Money`, `ParseResult`, `RecategorizeCommand`, `RecurringDetector`, `RecurringPayment`, `RecurringRepository`, `RuleRepository`, `SafeToSpendCalculator`, `SafeToSpendResult`, `SavingsGoal`, `ScoreComponent`, `SignedAmountCsvAdapter`, `Transaction`, `TransactionRepository`, `TransactionService`, `TxFilter`, `WealthSeiFacade`


## Added beyond Stage 1

`AbstractCsvAdapter`, `ArgTokenizer`, `BudgetCalculator`, `Categories`, `CategoryCorrector`, `CsvFormatException`, `CsvRecord`, `FileCodec`, `FinancialPlan`, `FixedBill`, `HealthScoreSummaryTemplate`, `InMemoryBudgetRepository`, `InMemoryGoalRepository`, `InMemoryPlanRepository`, `InMemoryRecurringRepository`, `InMemoryRuleRepository`, `InMemoryTransactionRepository`, `IncomeSource`, `InsightType`, `InvalidRowException`, `Main`, `PersistentBudgetRepository`, `PersistentPlanRepository`, `PersistentRecurringRepository`, `PersistentRuleRepository`, `PersistentTransactionRepository`, `PlanRepository`, `PlanService`, `PlannedExpense`, `RecurringReport`, `RecurringSummaryTemplate`, `RowParsing`, `SafeToSpendSummaryTemplate`, `StorageException`, `TransactionImportException`, `TsvFile`, `UnsupportedFormatException`, `ValidationException`, `WealthSeiFactory`


What these extras are:
- **Plan and balance setup (F03):** `FinancialPlan`, `IncomeSource`, `FixedBill`, `PlannedExpense`, `PlanService`, `PlanRepository`. Stage 1 only had `Account` with an opening balance; the plan classes carry the balance, income, bills and safety buffer that the budget and Safe-to-Spend features need.
- **Storage:** `TsvFile`, `FileCodec`, `StorageException`, the `InMemory*` and `Persistent*` repositories (Decorator pattern). Stage 1 planned SQLite; the repository interfaces are unchanged.
- **Command line:** `CliApp`, `ArgTokenizer`, `Main`. Stage 1 planned picocli.
- **Wiring:** `WealthSeiFactory` (Factory pattern), `CategoryCorrector`.
- **Forecast helpers:** `SafeToSpendSummaryTemplate`; `DailyBalance` also carries the day's income and outgoing, and `SafeToSpendResult` also carries the balance, expected income, buffer, days left and over-commitment.
- **Small helpers:** `Categories`, `RecurringReport`, `RecurringSummaryTemplate`, the CSV exception types, `RowParsing`, `CsvRecord`, `AbstractCsvAdapter` (Template Method).
