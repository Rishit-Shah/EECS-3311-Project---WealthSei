# WealthSei - AI collaboration log (Stage 2)

Working log for Section 5 of the Stage 2 report ("AI tools and collaboration log"). Kept as notes during development and condensed into the report at the end. No API keys or credentials belong in this file.

## 1. How the work is divided

This project is developed as a human-AI collaboration. I direct the work, make the design and scope decisions, run and check everything on my own machine, and own the repository. The AI assistant drafts code, tests and explanations in response to my instructions, and helps diagnose problems.

| What I do | What the AI assistant does |
|---|---|
| Chose the project, its features and its stack (Java, as my professor advised; LangChain4j for the LLM calls) | Proposes designs and drafts the Stage 1 report text and diagrams from my requirements |
| Decides what to build next and how much (for example: finish one piece at a time, no week-by-week plan; defer buying the API key) | Drafts the classes and unit tests for the piece I ask for, following the Stage 1 design |
| Reads the output, asks for changes and questions design choices | Explains the code and the trade-offs when asked; revises on request |
| Installs the tools, runs `mvn test` with real JUnit, reports the results and errors | Diagnoses errors from the logs and output I paste, and proposes fixes |
| Reviews, commits and pushes to GitHub; keeps the key out of the repository | Checks drafts in its own sandbox (strict compile, a stand-in test runner) before handing them over |
| Reports the AI usage honestly in this log and the report | Keeps the entries in this log up to date |

## 2. Tools used

| Tool / platform | Model | Used for | Token / money cost |
|---|---|---|---|
| Claude (chat with code tools) | Claude Sonnet 5.5 (`claude-sonnet-5-5`) | AI-assisted development in Stage 2: project setup, domain classes, unit tests, build and editor debugging, report drafting. Earlier (Stage 1): design report, UML diagrams, use cases. | Stage 2 up to the 2026-10-20 ~22:50 checkpoint: about **$5.27 API-equivalent** (about 15K plain input, 63K output, 10.1M cache-read and 1.07M cache-write tokens). Later work not yet totalled. **Update at submission.** |
| Claude API inside WealthSei (runtime) | `claude-haiku-5-5`, `claude-sonnet-5-5` | The app's own LLM calls (categorization, explanations, the agent). **Not used yet.** | Fill in from the Anthropic Console usage page. |
| VS Code + Java extension | n/a | Editing, running tests | Free |

Notes on cost: the figure is a computed API price, not necessarily what was billed (it depends on the plan used). Stage 1 usage happened before the last conversation summary and is not included.

## 3. Log entries

### 2026-10-20 - Project skeleton and first domain classes
- **My direction:** build in small finished pieces, no week-by-week schedule; keep API keys out of Git as the assignment requires; buy the API key later.
- **AI contribution:** drafted the Maven setup, a Java `.gitignore`, `.env.example`, and `Money`, `Category`, `Transaction` with unit tests.
- **Defect found by tests:** `Money.ZERO` was created before the rounding constant was initialised, so the class crashed on load (14 of 17 tests failed in the AI's check). Fixed by reordering the declarations.
- **My verification:** ran `mvn test` on my machine with real JUnit 5: 17 tests passing. I committed and pushed.

### 2026-10-20 - Environment setup problems
- **Problem:** Maven was not installed. Homebrew started compiling about 45 packages from source (Intel Mac on a new macOS), which would have taken hours. SDKMAN failed because macOS ships Bash 3.2.
- **How we solved it:** I ran the commands and pasted the output; the AI diagnosed it. I chose to stop the Homebrew build; we switched to downloading the JDK 17 and Maven archives into `~/tools`.
- **Problem:** VS Code showed 80 false errors ("record" not recognised, lambdas "only at source level 1.8"). The Java engine crashed on Java 17 (it needs 21), and the project was imported as "J2SE-1.5".
- **Fix:** I installed JDK 21 for the editor engine and edited my VS Code settings; the AI traced the "J2SE-1.5" library to the missing `maven.compiler.source` / `target` properties, and I applied them to `pom.xml`. All problems cleared.
- **Lesson:** the command-line build (`mvn test`) and the editor are separate; trust Maven for correctness.

### 2026-10-27 - Domain batch 2: planning inputs and budget model
- **My direction:** continue with the next piece; commit each part separately; skip per-class cost details in the report and keep one row per tool.
- **AI contribution:** drafted `Frequency`, `IncomeSource`, `FixedBill`, `PlannedExpense`, `FinancialPlan`, `BudgetLine`, `BudgetLimits`, `Budget`, `BudgetLineStatus`, `BudgetStatus`, `AlertType`, `AlertSeverity`, `Alert` and 5 test classes.
- **Design decisions discussed and recorded (to list in "changes from Stage 1"):**
  - `Frequency` gained `BIWEEKLY` (pay is often every two weeks).
  - `BudgetLineStatus` includes its `category` so it can be matched to a budget line.
  - `ratio` and `totalSpent` are derived by methods, not stored.
  - Alert levels follow the Stage 1 rule: 80% or more is WARNING, over 100% is OVER. Exactly 100% stays WARNING (limit reached, not exceeded).
  - Payment dates are computed from the first date, so a Jan 31 bill falls on Feb 28 then Mar 31.
- **My verification:** ran `mvn test` with real JUnit 5: 51 tests passing. I committed in separate pieces and pushed.

### 2026-10-27 - F01 Import transactions (CSV) and the first part of F02
- **My direction:** move on to the first working feature; I did not object to writing a small CSV reader instead of using Apache Commons CSV (the AI's sandbox cannot download Maven libraries, so code using it could not be tested before handing over).
- **AI contribution:** drafted `CsvParser`, `CsvRecord`, `RowParsing`, `BankCsvAdapter` with `SignedAmountCsvAdapter` and `DebitCreditCsvAdapter`, `CsvImporter`, `TransactionRepository` with an in-memory version, `TransactionService`, `CategorizationStrategy`, `KeywordRuleStrategy`, `Categorizer`, the records `ImportResult`, `ParseResult`, `TxFilter`, the `Categories` list, and 60 tests.
- **Design decisions to list in "changes from Stage 1":**
  - Own small `CsvParser` class instead of Apache Commons CSV; it plays the "adaptee" role in the Adapter pattern.
  - `CsvImporter` keeps a list of adapter factories instead of a list of `Class` objects.
  - `findById` returns an `Optional`.
  - Duplicate detection matches by count, so two real identical purchases in one file are both kept, but re-importing a file adds nothing.
  - An amount like `12,50` is rejected rather than guessed, to avoid silently misreading a European-style number.
  - Known limitation: some credit-card exports show purchases as positive numbers; those would be read as income.
- **Defect found while building:** the strict compiler flagged missing `serialVersionUID` fields on the new exception classes; fixed before handing over.
- **My verification:** (fill in after running `mvn test` with real JUnit 5; expected 111 tests.)

### 2026-10-27 - F03 Budget limits, tracking and alerts
- **My direction:** continue straight into the next feature so the app has something runnable; real `mvn test` after F01 passed with 111 tests, which I reported back before this started.
- **AI contribution:** drafted `BudgetRepository` (with an in-memory version), `BudgetCalculator`, `BudgetService`, `AlertMonitor` and `AlertListener` (Observer pattern), `ValidationException`, and 27 tests, following the Stage 1 design for Fig 3.2.
- **Design decisions to list in "changes from Stage 1":**
  - `BudgetRepository.find` returns an `Optional`.
  - `AlertMonitor` only raises a budget alert when the level for a category gets worse (nothing to WARNING, WARNING to OVER), so the same alert is not repeated on every refresh; dropping back under 80% resets it.
  - A listener that throws is skipped so it cannot stop the other listeners.
  - `BudgetCalculator` counts only expenses dated in the month; income (including refunds marked as income) is ignored, and spending in categories with no limit is not shown.
- **My verification:** (fill in after running `mvn test` with real JUnit 5; expected 138 tests.)

### 2026-11-02 - Plan service and facade (rest of F03)
- **My direction:** keep moving forward one piece at a time; the user-facing CLI comes next.
- **AI contribution:** drafted `PlanRepository` with an in-memory version, `PlanService` (balance, safety buffer, income, fixed bills, planned expenses), `WealthSeiFacade` (Facade pattern: one entry point for the GUI and CLI that refreshes budget alerts after an import or a category correction), `affectedMonths` on `ImportResult`, and 14 tests.
- **Design decisions to list in "changes from Stage 1":**
  - `ImportResult` carries the months touched by an import so the facade refreshes only those budgets.
  - `PlanService` starts from a zero balance dated today when no plan is saved yet; a balance date in the future is rejected.
- **My verification:** (fill in after running `mvn test` with real JUnit 5; expected 152 tests.)

### 2026-11-02 - F02 learned rules and undo (Command pattern)
- **My direction:** finish F02 properly: a correction should teach the app and be reversible. I questioned why F03 was pushed before F02 was finished, and the AI explained which classes are shared and which are not.
- **AI contribution:** drafted `LearnedRuleRepository` (in-memory version), `LearnedRuleStrategy`, the `Command` interface, `CommandHistory` (undo/redo), `CorrectCategoryCommand`, `CategoryCorrector`, the facade changes (`undoCorrection`, `redoCorrection`) and 20 tests.
- **Design decisions to list in "changes from Stage 1":**
  - A correction saves a merchant rule used by later imports; the learned rule is checked before the keyword rules.
  - Undo restores the old category and the old learned rule (or removes the new one). Income and "Uncategorized" corrections are not learned.
  - Past transactions from the same merchant are not changed automatically, only future imports.
  - The LLM fallback for unknown merchants is still to do (needs the API key and LangChain4j).
- **My verification:** (fill in after running `mvn test` with real JUnit 5; expected 172 tests.)

### 2026-11-02 - First runnable version: command-line interface
- **My direction:** I wanted something I can actually run and talk to, not only classes. I left the CLI library choice to the AI; it recommended a small hand-written command reader because it can be fully tested in the AI's sandbox (picocli could not be downloaded there).
- **AI contribution:** drafted `CliApp` (commands: import, transactions with filters, categories, correct, undo, redo, budget set/status, plan show/balance/buffer/income/bill/expense), `ArgTokenizer` (quoted arguments), `Main`, `WealthSeiFactory` (Factory pattern: one place that wires the services), `Categories.find`, and 25 tests.
- **Defects found while testing:** the first test run showed that merchant names are stored title-cased ("Loblaws"), so my expected text was wrong; and 36-character ids made the table unreadable. Fixed by showing 8-character ids and accepting any unique prefix in `correct`.
- **Design decisions to list in "changes from Stage 1":**
  - Hand-written command reader instead of picocli.
  - Data is in memory for now; it is lost when the program exits (persistence comes next).
- **My verification:** (fill in after running `mvn test` with real JUnit 5, expected 199 tests, and trying the CLI by hand.)

### 2026-11-02 - Saving data between sessions (file-based storage)
- **My direction:** I told the AI to go with its own suggestion for storage. It recommended plain text files over SQLite because it could test them fully (the SQLite library could not be downloaded in its sandbox).
- **AI contribution:** drafted `TsvFile` (tab-separated files with escaping and atomic replace), four file-backed repositories (`PersistentTransactionRepository`, `PersistentBudgetRepository`, `PersistentPlanRepository`, `PersistentLearnedRuleRepository`) that wrap the in-memory ones (Decorator pattern), `WealthSeiFactory.persistent(dir)`, a data folder setting in `Main` (`WEALTHSEI_HOME`, default `~/.wealthsei`), `BudgetRepository.findAll`, and 26 tests.
- **Design decisions to list in "changes from Stage 1":**
  - Text files instead of SQLite (no extra library; data can be opened in a text editor). The repository interfaces are unchanged, so SQLite could still be added later.
  - A bad data file stops start-up with a message naming the file and line.
  - The undo history and the alert monitor's memory are not saved; after a restart a budget already at WARNING may alert once more.
- **My verification:** (fill in after running `mvn test` with real JUnit 5, expected 225 tests, and quitting and restarting the CLI to check the data is still there.)

### 2026-11-04 - F04 Recurring payment and subscription detector
- **My direction:** continue with the next feature in the design (F04, then F05).
- **AI contribution:** drafted `RecurringPayment`, `RecurringDetector` (no AI: groups expenses by merchant, checks the gaps between payments and the amounts), `AnalyticsService`, `RecurringReport` with a template summary, `RecurringRepository` (in-memory and file-backed, for the review flags), the facade methods, the CLI commands `recurring` and `recurring flag|unflag`, a sample CSV, and 28 tests.
- **Defect found by tests:** a pattern whose amounts were steady only half the time scored exactly 0.80 and was not shown as "possible"; fixed by moving the cut-off to 0.85.
- **Design decisions to list in "changes from Stage 1":**
  - Confidence is a fixed formula: 0.6 x share of regular gaps + 0.4 x share of steady amounts. Groups with fewer than 3 payments, under 75% regular gaps, or under 50% steady amounts are not reported.
  - A pattern whose last payment is more than two intervals old is treated as cancelled.
  - `flagRecurring` also takes today's date so it can refuse a merchant that is not a detected recurring payment.
  - The summary is a fixed template for now. The LLM will be able to phrase it later; the template stays as the fallback the design asks for.
  - `RecurringPayment` gained `priceIncrease` and `occurrences`.
- **My verification:** (fill in after running `mvn test` with real JUnit 5, expected 253 tests, and trying `recurring` in the CLI with `sample-subscriptions.csv`.)

### 2026-11-04 - Checking the code against the Stage 1 design
- **My direction:** I asked the AI to make sure classes and methods follow Stage 1; more classes are fine, fewer are not.
- **AI contribution:** compared every class named in the Stage 1 report with the code (40 built with the Stage 1 name, 103 still to build, each belonging to a feature not built yet), then aligned the places where the code had drifted: `LearnedRuleRepository` became `RuleRepository` (`save`, `delete`, `findAll` returning the new `LearnedRule` record), `CorrectCategoryCommand` became `RecategorizeCommand` and now calls `learn()` and `forget()` on `LearnedRuleStrategy` as the Stage 1 sequence diagram shows, `Command.description()` became `describe()`, `CommandHistory.run()` became `execute()`. Generated `docs/stage1-conformance.md`, and added 4 tests.
- **Finding:** the plan classes (`FinancialPlan`, `IncomeSource`, `FixedBill`, `PlannedExpense`) are not in Stage 1 at all. They are extras that carry the balance, income and bills; Stage 1 only had `Account` with an opening balance. To be listed in the report's changes section.
- **My verification:** (fill in after running `mvn test` with real JUnit 5, expected 257 tests.)

### 2026-11-04 - F05 Safe-to-Spend Coach and 30-day forecast
- **My direction:** continue with F05 and keep the class and method names as in Stage 1.
- **AI contribution:** drafted `Forecast`, `DailyBalance`, `SafeToSpendResult`, `ForecastInputs` (with `deepCopy()`, the Prototype pattern), `ForecastEngine` (`project`, `monthlySurplus`), `SafeToSpendCalculator` (`calculate`), `SavingsGoal`, `GoalStatus`, `GoalRepository` (in-memory), the low-balance alert in `AlertMonitor`, `AnalyticsService.safeToSpend/forecast`, the facade methods, the CLI commands `safe [date]` and `forecast [days]`, and 45 tests. It also aligned `RecurringRepository` with Stage 1 (`saveFlag`, `flaggedMerchants`).
- **Defect found by tests:** one of the AI's test expectations was wrong (it subtracted payments that happened before the balance date); the code was right and the test was corrected after I checked the arithmetic by hand.
- **Design decisions to list in "changes from Stage 1":**
  - The balance today is the plan's balance plus the transactions dated after the balance date, up to today.
  - The forecast includes only committed money (income, plan bills, recurring payments, planned expenses); a recurring payment whose name matches a plan bill is counted once. Everyday spending is not projected.
  - `DailyBalance` carries the day's income and outgoing as well as the balance; `SafeToSpendResult` carries extra fields (balance, expected income, buffer, days left, shortfall).
  - A new plan starts with a safety buffer of $200 (the Stage 1 default). With no plan, `safe` asks the user to set a balance first.
  - The LOW_BALANCE alert is raised once, again if the balance would drop below zero, and again only after the balance has recovered.
  - The explanation is a fixed template for now; the LLM can phrase it later.
  - Goals can be stored but not yet created from the CLI (that is F09).
- **My verification:** (fill in after running `mvn test` with real JUnit 5, expected 298 tests, and trying `safe` and `forecast` in the CLI.)

### 2026-11-04: F06 habit detective and F07 health score

- **My direction:** I asked about F06, F07 and F08 before starting F09. Together we agreed F06 and F07 are plain calculations I can build and test now, and F08 needs the language-model layer, so it comes later with the agent.
- **AI contribution:** `HabitAnalyzer` (five rules with fixed thresholds), `Insight` and `InsightType`, `HealthScoreCalculator` (30/25/25/20 weights, re-scaled when a part has no data), `HealthScore`, `ScoreComponent`, template explanations, the `habits` and `score` CLI commands, and 31 new tests (329 in total in the AI's stand-in runner).
- **Design decisions to list in "changes from Stage 1":**
  - Habit rules look only at non-essential spending, so rent and groceries do not hide habits.
  - Fewer than 30 days of history gives a "need more data" error; no findings gives "no notable patterns".
  - Savings rate and emergency buffer are measured over the 30-day forecast, so they need a plan.
  - `ScoreComponent` has an extra `detail` field and `HealthScore` has an `excluded` list.
  - "Change from last month" is not computed yet (there is no stored history of past scores); the field exists and the explanation uses it once known.
- **My verification:** (fill in after running `mvn test`, expected 329 tests, and trying `habits 2026-10` and `score` in the CLI.)

### 2026-11-04: F09 savings goals

- **My direction:** finish F09 before the language-model work because it is plain arithmetic and F08 needs goals as a tool.
- **AI contribution:** `GoalService`, `GoalPlanner`, `GoalRequest`, `FeasibilityReport`, `GoalOption`, a goal alert in `AlertMonitor`, saved goals (`PersistentGoalRepository`), `goal add/list/replan/save/monthly` commands, goals re-checked after every import, `sample-habits.csv`, and 32 new tests (361 in the AI's stand-in runner).
- **Defects found while testing:** three of the AI's own test expectations were wrong (month arithmetic), and the first month-counting rule (days divided by 30.4) made "one year away" count as 13 months, so it was changed to calendar months.
- **Design decisions to list in "changes from Stage 1":**
  - Methods take today's date as a parameter (`createGoal(request, today)`, `projectedCompletion(goal, surplus, today)`), like `asOf` earlier, so tests control the date.
  - Part of a month counts as a whole month; one year away is 12 months.
  - The surplus available to a goal is the plan surplus minus what the other open goals already take each month.
  - Status rules: achieved when saved reaches the target; on track when the monthly contribution covers the requirement; at risk when it does not but the surplus could; behind otherwise. Behind or at risk raises a GOAL_RISK alert only when it gets worse.
  - `GoalOptionKind` enum added; `MonthlySurplusProvider` interface lets the goal service ask the analytics layer for the surplus; goals now persist in `goals.tsv`.
- **My verification:** (fill in after running `mvn test`, expected 361 tests, and trying `goal add`, `goal list` and `goal replan` in the CLI.)

## 4. Weekly commit summary (fill in with links)

| Period | Dates (Toronto time) | Required | Commits (links) |
|---|---|---|---|
| 1 | Oct 20-26 | 5 | |
| 2 | Oct 27-Nov 2 | 5 | |
| 3 | Nov 3-9 | 5 | |

## 5. Representative interactions to keep for the report

- A test failure, its root cause and the fix (the `Money.ZERO` initialisation bug).
- Editor errors that looked like code errors, traced to a build-file setting (my log output → the AI's diagnosis → my fix).
- Design-to-code decisions (alert thresholds, `BIWEEKLY`, date drift) discussed and recorded.
- Questions I raised about the design and the AI's answers, such as where `Alert` and `BudgetStatus` sit in the diagrams.

## 6. Still to add as work continues

- Each new feature or class group: date, my direction, AI contribution, defects found, design changes.
- The LLM layer: model names, what the prompts are for, and the runtime cost from the Console.
- Final token and money totals.
