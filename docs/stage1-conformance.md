# Stage 1 conformance (generated from the Stage 1 report)

Stage 1 names 144 classes, interfaces and enums (diagrams plus the value-object table). As of 2026-11-04: **126 built with the Stage 1 name**, 1 built under a Java-style spelling, **17 still to build** (each belongs to a feature that is not implemented yet).

## Built under a different spelling

- `CSVParser` (Stage 1) is `CsvParser` in code (Java naming style).

## Still to build, by feature

- **F06/F07 Habits and Health Score:** `InsightTool`
- **F09 Goals:** `GoalsView`
- **F11 Monthly Review:** `ReviewService`
- **LLM layer:** `ChatModel`, `AnthropicChatModel`, `ExplanationService`, `AnalysisFacts`
- **F12 Agent and Chat:** `ConversationMemory`, `MemoryManager`, `MemoryRepository`, `UserProfileMemory`
- **GUI:** `MainWindow`, `DashboardView`, `AssistantView`, `TransactionsView`, `ProposalView`, `ReportView`

## Built with the Stage 1 name

`Account`, `AgentContext`, `AgentController`, `AgentResult`, `AgentStatus`, `AgentStep`, `AgentTask`, `AgentTrace`, `Alert`, `AlertListener`, `AlertMonitor`, `AlertSeverity`, `AlertType`, `AnalyticsService`, `AppliedState`, `BankCsvAdapter`, `Budget`, `BudgetLimits`, `BudgetLine`, `BudgetLineStatus`, `BudgetRepository`, `BudgetService`, `BudgetStatus`, `BudgetStatusTool`, `CancelRecurringChange`, `CapCategoryChange`, `CategorizationStrategy`, `Categorizer`, `Category`, `ClaudeProvider`, `CliApp`, `Command`, `CommandFactory`, `CommandHistory`, `CsvExporter`, `CsvImporter`, `DailyBalance`, `DebitCreditCsvAdapter`, `ExpiredState`, `FeasibilityReport`, `FinalAnswerStep`, `FlagSubscriptionCommand`, `Forecast`, `ForecastEngine`, `ForecastInputs`, `ForecastTool`, `Frequency`, `GoalOption`, `GoalPlanner`, `GoalRepository`, `GoalRequest`, `GoalService`, `GoalStatus`, `GoalTool`, `GroundingChecker`, `GroundingReport`, `HabitAnalyzer`, `HealthScore`, `HealthScoreCalculator`, `ImportResult`, `IncomeChange`, `Insight`, `KeywordRuleStrategy`, `LLMCategorizationStrategy`, `LLMProvider`, `LLMProviderDecorator`, `LLMRequest`, `LLMResponse`, `LearnedRule`, `LearnedRuleStrategy`, `LoggingLLMProvider`, `MarkdownExporter`, `Message`, `MockLLMProvider`, `Money`, `MonthlyReview`, `OneTimeExpenseChange`, `ParseResult`, `PendingState`, `Planner`, `PromptBuilder`, `Proposal`, `ProposalDraft`, `ProposalRepository`, `ProposalService`, `ProposalState`, `RecategorizeCommand`, `RecurringDetector`, `RecurringPayment`, `RecurringRepository`, `RejectedState`, `ReportData`, `ReportExporter`, `ReportService`, `ResponseParser`, `RetryingLLMProvider`, `ReviewRepository`, `RuleRepository`, `SafeToSpendCalculator`, `SafeToSpendResult`, `SavingsGoal`, `Scenario`, `ScenarioChange`, `ScenarioResult`, `ScenarioSimulator`, `ScenarioTool`, `ScoreComponent`, `SetBudgetLimitCommand`, `SignedAmountCsvAdapter`, `TaskType`, `TextExporter`, `Tool`, `ToolCall`, `ToolCallStep`, `ToolManager`, `ToolResult`, `ToolSpec`, `TraceRepository`, `TraceStep`, `Transaction`, `TransactionQueryTool`, `TransactionRepository`, `TransactionService`, `TxFilter`, `UpdateGoalContributionCommand`, `WealthSeiFacade`


## Added beyond Stage 1

`AbstractCsvAdapter`, `AffordabilityFallback`, `ArgTokenizer`, `BudgetCalculator`, `CardMerchantCleaner`, `CardStatementCsvAdapter`, `Categories`, `CategoryCorrector`, `CsvFormatException`, `CsvRecord`, `ExportException`, `ExportFormat`, `FileCodec`, `FinancialPlan`, `FixedBill`, `GoalOptionKind`, `HealthScoreSummaryTemplate`, `HttpTransport`, `IllegalStateTransitionException`, `InMemoryBudgetRepository`, `InMemoryGoalRepository`, `InMemoryPlanRepository`, `InMemoryProposalRepository`, `InMemoryRecurringRepository`, `InMemoryReviewRepository`, `InMemoryRuleRepository`, `InMemoryTraceRepository`, `InMemoryTransactionRepository`, `IncomeSource`, `InsightType`, `InvalidRowException`, `JdkHttpTransport`, `Json`, `LLMException`, `LLMProviders`, `LlmConfig`, `Main`, `MonthlySurplusProvider`, `ParamSet`, `ParamSpec`, `ParamType`, `PersistentBudgetRepository`, `PersistentGoalRepository`, `PersistentPlanRepository`, `PersistentRecurringRepository`, `PersistentRuleRepository`, `PersistentTransactionRepository`, `PlanRepository`, `PlanService`, `PlannedExpense`, `ProposalType`, `PurchaseAssessor`, `PurchaseImpact`, `RecurringReport`, `RecurringSummaryTemplate`, `RowParsing`, `SafeToSpendSummaryTemplate`, `ScenarioDrafts`, `ScenarioParser`, `StorageException`, `TaskParams`, `ToolArgs`, `TransactionImportException`, `TsvFile`, `UnavailableLLMProvider`, `UndoneState`, `UnsupportedFormatException`, `ValidationException`, `Verdict`, `WealthSeiFactory`


What these extras are:
- **Plan and balance setup (F03):** `FinancialPlan`, `IncomeSource`, `FixedBill`, `PlannedExpense`, `PlanService`, `PlanRepository`. Stage 1 only had `Account` with an opening balance; the plan classes carry the balance, income, bills and safety buffer that the budget and Safe-to-Spend features need.
- **Storage:** `TsvFile`, `FileCodec`, `StorageException`, the `InMemory*` and `Persistent*` repositories (Decorator pattern). Stage 1 planned SQLite; the repository interfaces are unchanged.
- **Command line:** `CliApp`, `ArgTokenizer`, `Main`. Stage 1 planned picocli.
- **Wiring:** `WealthSeiFactory` (Factory pattern), `CategoryCorrector`.
- **Forecast helpers:** `SafeToSpendSummaryTemplate`; `DailyBalance` also carries the day's income and outgoing, and `SafeToSpendResult` also carries the balance, expected income, buffer, days left and over-commitment.
- **Small helpers:** `Categories`, `RecurringReport`, `RecurringSummaryTemplate`, the CSV exception types, `RowParsing`, `CsvRecord`, `AbstractCsvAdapter` (Template Method).
