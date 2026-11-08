# Stage 1 conformance (generated from the Stage 1 report)

Stage 1 names 144 classes, interfaces and enums (diagrams plus the value-object table). As of 2026-11-07: **137 built with the Stage 1 name**, **3 built under a different name** (see below), **4 not built under any name** (replaced by other classes, see below).

**Update (final Stage 1 report):** the counts above were made from an older Stage 1 copy. Checking the class names in the final Stage 1 text against the code shows the same result: the only names not in the code are the library classes (`CSVParser`, `ChatModel`, `AnthropicChatModel`, `MessageWindowChatMemory`, LangChain4j message types), `ExplanationService`, `AnalysisFacts`, and the renamed `ProposalView` and `ReportView`. The six plan classes and `NewTransaction` are Stage 1 classes and are built.

## Built under a different name

| Stage 1 name | In code | Why |
|---|---|---|
| `CSVParser` | `CsvParser` | Java naming style |
| `ProposalView` | `ProposalsView` | plural, because the screen lists many proposals |
| `ReportView` | `ReportsView` | plural, the screen holds the review and the exports |

## Not built under any name, and what replaced it

| Stage 1 class | What the code does instead |
|---|---|
| `ChatModel`, `AnthropicChatModel` | Stage 1 planned the langchain4j library. The code talks to the Claude API directly with `ClaudeProvider`, `HttpTransport`/`JdkHttpTransport` and our own `Json`, behind the `LLMProvider` interface (Adapter pattern, with `RetryingLLMProvider` and `LoggingLLMProvider` as Decorators). |
| `ExplanationService`, `AnalysisFacts` | Stage 1 planned one service that turns computed facts into explanations. The code gives each feature its own small piece: fixed summary templates (`RecurringSummaryTemplate`, `SafeToSpendSummaryTemplate`, `HealthScoreSummaryTemplate`, `AffordabilityFallback`), the agent prompts (`PromptBuilder`) for the explanations of F05 to F13, and `RecurringReviewer` for F04. The computed facts reach the model as tool results, checked by `GroundingChecker`. |

## Built with the Stage 1 name

`Account`, `AgentContext`, `AgentController`, `AgentResult`, `AgentStatus`, `AgentStep`, `AgentTask`, `AgentTrace`, `Alert`, `AlertListener`, `AlertMonitor`, `AlertSeverity`, `AlertType`, `AnalyticsService`, `AppliedState`, `AssistantView`, `BankCsvAdapter`, `Budget`, `BudgetLimits`, `BudgetLine`, `BudgetLineStatus`, `BudgetRepository`, `BudgetService`, `BudgetStatus`, `BudgetStatusTool`, `CancelRecurringChange`, `CapCategoryChange`, `CategorizationStrategy`, `Categorizer`, `Category`, `ClaudeProvider`, `CliApp`, `Command`, `CommandFactory`, `CommandHistory`, `ConversationMemory`, `CsvExporter`, `CsvImporter`, `DailyBalance`, `DashboardView`, `DebitCreditCsvAdapter`, `ExpiredState`, `FeasibilityReport`, `FinalAnswerStep`, `FlagSubscriptionCommand`, `Forecast`, `ForecastEngine`, `ForecastInputs`, `ForecastTool`, `Frequency`, `GoalOption`, `GoalPlanner`, `GoalRepository`, `GoalRequest`, `GoalService`, `GoalStatus`, `GoalTool`, `GoalsView`, `GroundingChecker`, `GroundingReport`, `HabitAnalyzer`, `HealthScore`, `HealthScoreCalculator`, `ImportResult`, `IncomeChange`, `Insight`, `InsightTool`, `KeywordRuleStrategy`, `LLMCategorizationStrategy`, `LLMProvider`, `LLMProviderDecorator`, `LLMRequest`, `LLMResponse`, `LearnedRule`, `LearnedRuleStrategy`, `LoggingLLMProvider`, `MainWindow`, `MarkdownExporter`, `MemoryManager`, `MemoryRepository`, `Message`, `MockLLMProvider`, `Money`, `MonthlyReview`, `OneTimeExpenseChange`, `ParseResult`, `PendingState`, `Planner`, `PromptBuilder`, `Proposal`, `ProposalDraft`, `ProposalRepository`, `ProposalService`, `ProposalState`, `RecategorizeCommand`, `RecurringDetector`, `RecurringPayment`, `RecurringRepository`, `RejectedState`, `ReportData`, `ReportExporter`, `ReportService`, `ResponseParser`, `RetryingLLMProvider`, `ReviewRepository`, `ReviewService`, `RuleRepository`, `SafeToSpendCalculator`, `SafeToSpendResult`, `SavingsGoal`, `Scenario`, `ScenarioChange`, `ScenarioResult`, `ScenarioSimulator`, `ScenarioTool`, `ScoreComponent`, `SetBudgetLimitCommand`, `SignedAmountCsvAdapter`, `TaskType`, `TextExporter`, `Tool`, `ToolCall`, `ToolCallStep`, `ToolManager`, `ToolResult`, `ToolSpec`, `TraceRepository`, `TraceStep`, `Transaction`, `TransactionQueryTool`, `TransactionRepository`, `TransactionService`, `TransactionsView`, `TxFilter`, `UpdateGoalContributionCommand`, `UserProfileMemory`, `WealthSeiFacade`



## Method-level comparison against the submitted UXF files (final check, Nov 7)

The earlier method comparison below was made from an older Stage 1 copy and missed manual entry. This one reads the `.uxf` files in `docs/uxf_files_for_diagrams_relevant` directly: 432 attributes and methods in the class diagrams (Figures 3-1 to 3-7) and 178 calls in the sequence diagrams (SD01 to SD11), each looked up by name in the code. Parameters and return types were not compared.

- **Sequence diagrams:** 173 of 178 calls exist in the code. The 5 that do not are `getRecurringPayments`, `getHabitInsights`, `checkAffordability`, `planGoal` (renamed facade methods, report C5) and `explain` (`ExplanationService`, report C4).
- **Class diagrams, all present by name** except:
  - the 5 renamed facade methods above and `MainWindow.show()` (now `launch`);
  - `ExplanationService`, `ChatModel`, `AnthropicChatModel` and their fields (not built, C2 and C4);
  - 18 screen handlers (`onApprove`, `onAddClicked`, `onPlanSaved`, …): button actions inside the Swing views (C1);
  - private fields and helpers that the changed design no longer needs, such as `chatMemory`, `undoStack`/`redoStack`, `backoffSeconds`, `logger`, `scriptedReplies`, `adapterTypes`, and repository or helper fields that are passed in as arguments (C18);
  - reshaped records: `ForecastInputs` (`plan`, `startDate`, `startBalance` became `today`, `openingBalance` and the plan's three lists), `ReportData.status`, and `RecurringPayment.income` (C18).
- `BudgetStatus.totalSpent` and `BudgetLineStatus.level` exist as computed methods, and every enum constant is present.
- Manual entry (`NewTransaction`, `TransactionService.addTransaction`/`validate`, `WealthSeiFacade.addTransaction`) is now built (report C17).

## Earlier method-level comparison (older Stage 1 copy; superseded by the section above)

418 members (methods and fields) in the Stage 1 class diagrams were checked by name in the classes that exist in the code. 370 are present with the same name. The 48 that are not are listed below. (Parameters and return types were not compared, only names; some methods gained extra parameters such as a date, and some gained overloads.)

**Renamed or reshaped methods on `WealthSeiFacade`** (the behaviour exists):

| Stage 1 | In code |
|---|---|
| `getRecurringPayments()` | `recurringPayments(asOf)` (returns a `RecurringReport` with the total and a summary) |
| `getHabitInsights(month)` | `getHabits(month)` |
| `checkAffordability(item, price)` | `canIAfford(item, price, date)` |
| `planGoal(request)` returning an agent result | `addGoal(request, today)` and `getGoalFeasibility(id, today)`: goal planning is plain code (`GoalPlanner`); the AI agent reads goals through `GoalTool` |
| `replanGoal(goalId)` returning an agent result | `getGoalFeasibility(id, today)` and `proposeGoalFix(id, today)` |

**Screen event methods** (`onAffordCheck`, `onWhatIf`, `onAskSubmitted`, `onShowTrace`, `onCreateGoal`, `onReplan`, `onApprove`, `onReject`, `onGenerateReview`, `onExport`, `onImportClicked`, `onCategoryChanged`, `onUndoClicked`, `onBudgetSaved`, and `onAlert` on `CliApp` and `DashboardView`): the Swing screens attach a button action directly instead of a named handler method. `onAlert` is the `AlertListener` that the CLI and the shell register with `addAlertListener` (Observer pattern). `MainWindow.show()` is `MainWindow.launch(...)`.

**Private fields that the changed design no longer needs:** `chatMemory` (our own `ConversationMemory` window instead of langchain4j), `chatModel`, `toChatRequest`, `toLlmResponse` (the langchain4j types), `parser` and `CSVRecord` in `BankCsvAdapter` (our own `CsvParser`/`CsvRecord`), `adapterTypes` in `CsvImporter`, `undoStack` and `redoStack` in `CommandHistory` (two deques named `done` and `undone`), `logger` (a log-line consumer), `scriptedReplies` in `MockLLMProvider` (a script list), `backoffSeconds` (milliseconds in `RetryingLLMProvider`), and dependencies that are passed in as method arguments instead of being stored (`txRepo`, `forecastEngine`, `budgets`, `recurringRepo`, `repo`, `memory`, `status`, `allowed`, `explainer`, `traces`, `protectedCategories`, `lastReviewSummary`).

Everything else in the Stage 1 diagrams, including every service, strategy, tool, state and command method, is present in the code.

## Added beyond Stage 1

`AbstractCsvAdapter`, `AffordabilityFallback`, `AppContext`, `ArgTokenizer`, `AttentionRow`, `Bubble`, `BudgetCalculator`, `Card`, `CardMerchantCleaner`, `CardStatementCsvAdapter`, `Categories`, `CategoryCorrector`, `ChatDock`, `ChatPanel`, `CsvFormatException`, `CsvRecord`, `DashboardData`, `Dialogs`, `DonutChart`, `Dropdown`, `ExportException`, `ExportFormat`, `FileCodec`, `FinancialPlan`, `FixedBill`, `FlatButton`, `GoalOptionKind`, `GoalRow`, `HealthScoreSummaryTemplate`, `HeroBalanceCard`, `HeroHeader`, `HttpTransport`, `Icons`, `IllegalStateTransitionException`, `InMemoryBudgetRepository`, `InMemoryGoalRepository`, `InMemoryMemoryRepository`, `InMemoryPlanRepository`, `InMemoryProposalRepository`, `InMemoryRecurringRepository`, `InMemoryReviewRepository`, `InMemoryRuleRepository`, `InMemoryTraceRepository`, `InMemoryTransactionRepository`, `IncomeSource`, `InputField`, `InsightType`, `InvalidRowException`, `JdkHttpTransport`, `Json`, `LLMException`, `LLMProviders`, `Label`, `LegendRow`, `LineChart`, `ListRow`, `LlmConfig`, `Main`, `Menus`, `ModernScrollBar`, `MonthlySurplusProvider`, `ParamSet`, `ParamSpec`, `ParamType`, `PersistentBudgetRepository`, `PersistentGoalRepository`, `PersistentMemoryRepository`, `PersistentPlanRepository`, `PersistentRecurringRepository`, `PersistentRuleRepository`, `PersistentTransactionRepository`, `Pill`, `PlaceholderView`, `PlainText`, `PlanDialogs`, `PlanRepository`, `PlanService`, `PlannedExpense`, `ProgressRow`, `ProposalType`, `ProposalsView`, `PurchaseAssessor`, `PurchaseImpact`, `RecurringReport`, `RecurringReview`, `RecurringReviewer`, `RecurringSummaryTemplate`, `ReportsView`, `ReviewResult`, `RowParsing`, `SafeToSpendSummaryTemplate`, `ScenarioDrafts`, `ScenarioParser`, `ScoreRing`, `SearchField`, `Segmented`, `ShellPanel`, `StorageException`, `TaskParams`, `Text`, `Theme`, `Toast`, `ToggleChip`, `ToolArgs`, `TopBar`, `TraceDialog`, `TransactionImportException`, `TsvFile`, `UnavailableLLMProvider`, `UndoneState`, `UnsupportedFormatException`, `ValidationException`, `Verdict`, `View`, `WealthSeiFactory`


What these extras are:
- **Plan and balance setup (F03):** `FinancialPlan`, `IncomeSource`, `FixedBill`, `PlannedExpense`, `PlanService`, `PlanRepository` are named in the final Stage 1 report (F03 and the domain package list), so they are Stage 1 classes, built under the same name. (An earlier draft of this file compared against an older Stage 1 copy and listed them as extras.)
- **Storage:** `TsvFile`, `FileCodec`, `StorageException`, the `InMemory*` and `Persistent*` repositories (Decorator pattern). Stage 1 planned SQLite; the repository interfaces are unchanged.
- **Command line:** `CliApp`, `ArgTokenizer`, `Main`. Stage 1 planned picocli.
- **Wiring:** `WealthSeiFactory` (Factory pattern), `CategoryCorrector`.
- **Forecast helpers:** `SafeToSpendSummaryTemplate`; `DailyBalance` also carries the day's income and outgoing, and `SafeToSpendResult` also carries the balance, expected income, buffer, days left and over-commitment.
- **Small helpers:** `Categories`, `RecurringReport`, `RecurringSummaryTemplate`, the CSV exception types, `RowParsing`, `CsvRecord`, `AbstractCsvAdapter` (Template Method).
