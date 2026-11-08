# Class diagram changes against the submitted Stage 1 UXF files

Every Stage 1 class box is kept (orange = changed in Stage 2, green = new, white = unchanged). This file lists every change by figure and class. Methods dropped from view classes are Stage 1 button-handler names (`onXClicked`) that are anonymous listeners in the Swing code.


## Figure 3.1 — Presentation and facade

- **MainWindow**: show() replaced by launch()
- **CliApp**: run: parameters/types now +run(in: BufferedReader)…; dropped +onAlert() (handler or not in code)
- **TransactionsView**: dropped +onImportClicked() (handler or not in code); dropped +onAddClicked() (handler or not in code); dropped +onCategoryChanged() (handler or not in code); dropped +onUndoClicked() (handler or not in code)
- **DashboardView**: dropped +onPlanSaved() (handler or not in code); dropped +onBudgetSaved() (handler or not in code); dropped +onAlert() (handler or not in code)
- **GoalsView**: dropped +onCreateGoal() (handler or not in code); dropped +onReplan() (handler or not in code)
- **AssistantView**: dropped +onAffordCheck() (handler or not in code); dropped +onWhatIf() (handler or not in code); dropped +onAskSubmitted() (handler or not in code); dropped +onShowTrace() (handler or not in code)
- **ReportsView**: renamed from ReportView; dropped +onGenerateReview() (handler or not in code); dropped +onExport() (handler or not in code)
- **ProposalsView**: renamed from ProposalView; dropped +onApprove() (handler or not in code); dropped +onReject() (handler or not in code)
- **WealthSeiFacade**: dropped private field explainer; dropped private field history; dropped private field traces; importTransactions: parameters/types now +importTransactions(file: Path,…; dropped +undo() (handler or not in code); dropped +redo() (handler or not in code); getRecurringPayments() replaced by recurringPayments(); getHabitInsights() replaced by getHabits(); getHealthScore: parameters/types now +getHealthScore(month: YearMonth,…; checkAffordability() replaced by canIAfford(); planGoal() replaced by addGoal(); replanGoal() replaced by getGoalFeasibility(); replanGoal() replaced by proposeGoalFix(); listPendingProposals: parameters/types now +listPendingProposals(today: LocalDate) List<Proposal>…; decideProposal: parameters/types now +decideProposal(idOrPrefix: String,…; exportReport: parameters/types now +exportReport(month: YearMonth,…; setProtectedCategory: parameters/types now +setProtectedCategory(category: Category,…
- **ShellPanel**: NEW class added
- **WealthSeiFactory**: NEW class added
- **ArgTokenizer**: NEW class added
- **Main**: NEW class added

## Figure 3.2 — Import, categorization, plan, budgets, alerts

- **TransactionService**: added public methods now in the code: findById
- **CsvImporter**: dropped private field adapterTypes
- **BankCsvAdapter**: dropped field parser; dropped #toTransaction() (handler or not in code); added public methods now in the code: layoutDescription
- **SignedAmountCsvAdapter**: added public methods now in the code: canHandle, layoutDescription
- **DebitCreditCsvAdapter**: added public methods now in the code: canHandle, layoutDescription
- **LearnedRuleStrategy**: added public methods now in the code: ruleFor
- **KeywordRuleStrategy**: added public methods now in the code: withDefaults
- **LLMCategorizationStrategy**: dropped private field llm; dropped private field allowed
- **RuleRepository**: added public methods now in the code: find, keyOf
- **PlanService**: added public methods now in the code: setBalance, setSafetyBuffer, addIncome, addFixedBill, addPlannedExpense
- **BudgetService**: added public methods now in the code: limitOf, hasBudget, primeAlerts; added public methods now in the code: limitOf, hasBudget, primeAlerts
- **BudgetRepository**: added public methods now in the code: findAll
- **AlertMonitor**: added public methods now in the code: prime, evaluateLowBalance, evaluateGoal
- **CsvParser**: renamed from CSVParser; added public methods now in the code: parse, header, records
- **AbstractCsvAdapter**: NEW class added
- **CardStatementCsvAdapter**: NEW class added
- **CsvRecord**: NEW class added
- **CategoryCorrector**: NEW class added
- **BudgetCalculator**: NEW class added
- **InMemoryTransactionRepository**: NEW class added
- **PersistentTransactionRepository**: NEW class added
- **TsvFile**: NEW class added

## Figure 3.3 — Analytics, goals, scenarios, recurring

- **AnalyticsService**: dropped private field recurringRepo; dropped private field budgets; detectRecurring: parameters/types now +detectRecurring(asOf: LocalDate) List<RecurringPayment>…; flagRecurring: parameters/types now +flagRecurring(merchant: String,…; healthScore: parameters/types now +healthScore(status: BudgetStatus,…; added public methods now in the code: recurringReport, useReviewer, reviewRecurring, purchaseImpact, baselineInputs, monthlySurplus
- **RecurringDetector**: detect: parameters/types now +detect(txs: List<Transaction>,…
- **ForecastInputs**: fields now today, openingBalance, transactions, recurring, incomeSources, fixedBills, plannedExpenses, typicalMonthlySpending (was plan, transactions, recurring, startDate, startBalance)
- **HealthScoreCalculator**: added public methods now in the code: biggestGap
- **GoalService**: dropped private field txRepo; dropped private field planRepo; dropped private field forecastEngine; createGoal: parameters/types now +createGoal(request: GoalRequest,…; evaluate: parameters/types now +evaluate(goalId: String, today: LocalDate) SavingsGoal…; evaluateAll: parameters/types now +evaluateAll(today: LocalDate) List<SavingsGoal>…; feasibility: parameters/types now +feasibility(goalId: String,…; updateContribution: parameters/types now +updateContribution(goalId: String,…; added public methods now in the code: goals, find, statusOf, addSaved
- **GoalPlanner**: projectedCompletion: parameters/types now +projectedCompletion(goal: SavingsGoal,…; buildOptions: parameters/types now +buildOptions(goal: SavingsGoal,…; added public methods now in the code: monthsLeft, isFeasible
- **ScenarioSimulator**: dropped private field forecastEngine; dropped private field txRepo; dropped private field planRepo; simulate: parameters/types now +simulate(scenario: Scenario,…
- **CancelRecurringChange**: added public methods now in the code: applyTo, describe
- **CapCategoryChange**: added public methods now in the code: applyTo, describe
- **IncomeChange**: added public methods now in the code: applyTo, describe
- **OneTimeExpenseChange**: added public methods now in the code: applyTo, describe
- **RecurringRepository**: added public methods now in the code: isFlagged, keyOf
- **ExplanationService**: NOT BUILT: removed from the diagram
- **RecurringReviewer**: NEW class added
- **RecurringReport**: NEW class added
- **RecurringSummaryTemplate**: NEW class added
- **SafeToSpendSummaryTemplate**: NEW class added
- **HealthScoreSummaryTemplate**: NEW class added
- **ScenarioParser**: NEW class added
- **MonthlySurplusProvider**: NEW class added

## Figure 3.4 — Agent subsystem

- **AgentController**: added public methods now in the code: getTrace, latestTrace
- **AgentContext**: dropped private field protectedCategories; dropped private field lastReviewSummary; addObservation: parameters/types now +addObservation(call: ToolCall,…; 14 more public methods exist in the code and are not drawn (key ones only, as in Stage 1)
- **AgentResult**: fields now answer, drafts, status, grounding, traceId, verdict, tokensUsed (was answer, drafts, status, grounding, traceId)
- **PromptBuilder**: buildRepairPrompt: parameters/types now +buildRepairPrompt(ctx: AgentContext,…
- **ToolManager**: added public methods now in the code: has
- **ToolCall**: fields now id, toolName, args (was toolName, args)
- **TransactionQueryTool**: added public methods now in the code: spec, run
- **BudgetStatusTool**: added public methods now in the code: spec, run
- **ForecastTool**: added public methods now in the code: spec, run
- **InsightTool**: added public methods now in the code: spec, run
- **GoalTool**: added public methods now in the code: spec, run
- **ScenarioTool**: added public methods now in the code: spec, run
- **MemoryManager**: buildContext: parameters/types now +buildContext(task: AgentTask,…; setProtectedCategory: parameters/types now +setProtectedCategory(category: Category,…; added public methods now in the code: forgetConversation, protectedCategories, rememberReview, pastReviewSummaries
- **ConversationMemory**: dropped private field chatMemory; added public methods now in the code: clear
- **UserProfileMemory**: added public methods now in the code: setProtected, replaceProtected, getPastReviewSummaries, addReviewSummary, replaceReviewSummaries
- **GroundingChecker**: verify: parameters/types now +verify(answer: String,…; added public methods now in the code: numbersIn
- **AgentTrace**: added public methods now in the code: id, taskType, question, steps
- **TraceRepository**: added public methods now in the code: latest
- **PurchaseAssessor**: NEW class added
- **Verdict**: NEW class added
- **AffordabilityFallback**: NEW class added
- **ScenarioDrafts**: NEW class added
- **PlainText**: NEW class added

## Figure 3.5 — LLM provider layer

- **ClaudeProvider**: dropped private field chatModel; dropped private field model; dropped private toChatRequest(); dropped private toLlmResponse()
- **MockLLMProvider**: dropped private field scriptedReplies; added public methods now in the code: thenReply, thenFail, requests
- **RetryingLLMProvider**: dropped private field backoffSeconds
- **LoggingLLMProvider**: dropped private field logger; added public methods now in the code: calls, failures, totalTokens
- **ChatModel**: NOT BUILT: removed from the diagram
- **AnthropicChatModel**: NOT BUILT: removed from the diagram
- **HttpTransport**: NEW class added
- **JdkHttpTransport**: NEW class added
- **UnavailableLLMProvider**: NEW class added
- **Json**: NEW class added
- **LlmConfig**: NEW class added

## Figure 3.6 — Proposals, commands, reviews, reports

- **ProposalService**: dropped private field memory; decide: parameters/types now +decide(idOrPrefix: String,…; expireOld: parameters/types now +expireOld(current: YearMonth) int…; added public methods now in the code: lastDiscarded, listAll, find, undoLast, setProtectedCategory, protectedCategories
- **Proposal**: setState: parameters/types now +setState(next: ProposalState)…; 9 more public methods exist in the code and are not drawn (key ones only, as in Stage 1)
- **ProposalState**: added public methods now in the code: undo, refuse
- **PendingState**: added public methods now in the code: approve, reject, expire, name
- **AppliedState**: added public methods now in the code: approve, reject, expire, undo, name
- **RejectedState**: added public methods now in the code: approve, reject, expire, name
- **ExpiredState**: added public methods now in the code: approve, reject, expire, name
- **ProposalRepository**: added public methods now in the code: findAll
- **CommandHistory**: dropped private field undoStack; dropped private field redoStack; added public methods now in the code: canRedo
- **RecategorizeCommand**: added public methods now in the code: execute, undo, describe, transaction
- **SetBudgetLimitCommand**: added public methods now in the code: execute, undo, describe
- **UpdateGoalContributionCommand**: added public methods now in the code: execute, undo, describe
- **FlagSubscriptionCommand**: added public methods now in the code: execute, undo, describe
- **ReviewService**: dropped private field repo; dropped private field planRepo; added public methods now in the code: find
- **ReportService**: export: parameters/types now +export(month: YearMonth,…; exporterFor: parameters/types now -exporterFor(format: ExportFormat) ReportExporter…
- **ReportData**: fields now month, generatedOn, income, spending, transactionCount, categories, goals, review (was month, status, review)
- **BudgetService**: added public methods now in the code: limitOf, hasBudget, primeAlerts
- **UndoneState**: NEW class added

## Figure 3.7 — Domain model

- **Money**: 13 more public methods exist in the code and are not drawn (key ones only, as in Stage 1)
- **FinancialPlan**: fields now currentBalance, balanceAsOf, safetyBuffer, incomeSources, fixedBills, plannedExpenses (was currentBalance, balanceAsOf, safetyBuffer)
- **FixedBill**: fields now name, amount, frequency, nextDueDate, category (was name, amount, frequency, nextDueDate)
- **PlannedExpense**: fields now description, amount, date, category (was description, amount, date)
- **Transaction**: fields now id, date, amount, merchant, description, income, category (was id, date, amount, merchant, description, income)
- **Budget**: fields now month, lines (was month)
- **BudgetLine**: fields now category, limit (was limit)
- **BudgetStatus**: fields now month, lines (was month, totalSpent)
- **BudgetLineStatus**: fields now category, spent, limit (was spent, limit, level)
- **RecurringPayment**: fields now merchant, amount, frequency, nextDue, confidence, flaggedForReview, priceIncrease, occurrences (was merchant, amount, frequency, nextDue, confidence, flaggedForReview, income)
- **DailyBalance**: fields now date, balance, income, outgoing (was date, balance)
- **SafeToSpendResult**: fields now dailyAllowance, remainingThisMonth, upcomingBills, goalReserve, explanation, balance, expectedIncome, safetyBuffer, daysLeft, overCommittedBy (was dailyAllowance, remainingThisMonth, upcomingBills, goalReserve, explanation)
- **HealthScore**: fields now total, components, excluded, explanation, previousTotal (was total, explanation)
- **ScoreComponent**: fields now name, points, maxPoints, detail (was name, points, maxPoints)
- **ScenarioResult**: fields now months, baselineEndBalance, scenarioEndBalance, baselineLowest, scenarioLowest, baselineMonthlySavings, scenarioMonthlySavings, monthlySavingsDelta, changes, goalImpacts, typicalMonthlySpending (was baselineEndBalance, scenarioEndBalance, monthlySavingsDelta)
- **MonthlyReview**: fields now month, healthScore, narrative, proposalIds, traceId, generatedAt (was month, narrative, proposalIds, traceId, generatedAt)
- **PurchaseImpact**: NEW class added
- **RecurringReview**: NEW class added
