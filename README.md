# WealthSei — An AI Personal Finance Agent

EECS3311 Software Design, Fall 2026 course project.

WealthSei is an AI agent that helps with everyday money decisions: safe-to-spend
guidance, "can I afford this?" checks, savings-goal planning, what-if
simulations and monthly reviews. It has a GUI and a CLI.

## Project stages

| Stage | Description | Status |
|---|---|---|
| 1 | Design (features, UML, patterns, traceability) | [Stage 1 report](docs/WealthSei_Stage1_Report.md) |
| 2 | AI-assisted implementation |[Stage 2 report](docs/WealthSei_Stage2_Report.md) |
| 3 | Testing (pytest and KUMA agent testing) |

**Student:** Rishit Shah 
## Stage 2: build, run and demo

Stage 2 implementation: full report in [WealthSei_Stage2_Report.md](WealthSei_Stage2_Report.md). Differences from the Stage 1 design are listed in [docs/stage1-conformance.md](docs/stage1-conformance.md) and in Section 4 of that report.

## Prerequisites

- JDK 17 or newer (`java -version`)
- Maven 3.9 or newer (`mvn -version`)
- Optional, for the AI features: an Anthropic API key (console.anthropic.com). Everything else works without one.

## Install dependencies and build

The only dependency is JUnit 5 (tests, downloaded by Maven). The application itself uses only the JDK.

```
mvn -q compile
```

Run all unit tests (587 tests):

```
mvn test
```

## Local configuration (API key)

1. Copy the example file:

```
cp .env.example .env
```

2. Open `.env` and replace `sk-ant-REPLACE_ME` with your own key. `.env` is in `.gitignore` and is never committed.
   The key is read from `.env` (or the `ANTHROPIC_API_KEY` environment variable) and is never printed or logged.
3. The models are set in `.env` too: `WEALTHSEI_HAIKU_MODEL` (categorizing and the recurring-payments review) and
   `WEALTHSEI_SONNET_MODEL` (the question-answering agent).

Without a key the app starts normally; AI features fall back to fixed templates or say the AI is unavailable.
With a key, the first line printed says `AI features: on (...)`.

Your data is saved in `~/.wealthsei` (override with the environment variable `WEALTHSEI_HOME`).
Call details (model, tokens, time; never questions, answers or the key) go to `llm.log` in that folder.

## Launch

Run from the project folder (the one containing `pom.xml` and `.env`). Use `compile exec:java` so edits are rebuilt.

GUI:

```
mvn -q compile exec:java -Dexec.mainClass=wealthsei.Main -Dexec.args="gui"
```

CLI (interactive; type `help` for every command, `exit` to quit):

```
mvn -q compile exec:java -Dexec.mainClass=wealthsei.Main
```

CLI, one command and exit:

```
mvn -q compile exec:java -Dexec.mainClass=wealthsei.Main -Dexec.args="safe"
```

## Safe sample data (try it without any real data)

`demo/` contains invented statements (no real transactions). This script loads them, a plan and three goals
into a separate folder, `~/.wealthsei-demo`, so your real data is never touched:

```
bash demo/load-demo.sh
```

Then open the app on it:

```
WEALTHSEI_HOME=~/.wealthsei-demo mvn -q compile exec:java -Dexec.mainClass=wealthsei.Main -Dexec.args="gui"
```

Other small sample files in the project root: `sample-transactions.csv`, `sample-subscriptions.csv`,
`sample-habits.csv`.

## Example usage (CLI)

```
import demo/NeoDemoMastercard__2026-10-01_2026-10-31.csv
transactions month=2026-10 category=dining
budget set 2026-10 dining=450 groceries=400
budget status 2026-10
plan balance 2860.40
plan income Paycheque 1685 biweekly 2026-11-13
safe
forecast 30
recurring
recurring review
habits 2026-10
score 2026-10
goal add "Laptop" 1400 2026-12-20 350
goal replan <goalId>
afford "laptop" 600
trace
whatif cancel=Netflix caps=dining=150
proposals from-whatif
proposals approve <id>
proposals undo
review 2026-10
ask "How much did I spend on dining last month?"
export 2026-10 md ~/october.md
```

`correct <txId> <categoryId>` fixes a category (the app remembers the merchant); `undo` / `redo` reverse it.

## Features and where they live

| ID | Feature | Main classes |
|---|---|---|
| F01 | Import transactions (CSV) | `CsvImporter`, `BankCsvAdapter` and subclasses, `CardStatementCsvAdapter`, `CsvParser` |
| F02 | Categorization and corrections | `Categorizer`, `KeywordRuleStrategy`, `LearnedRuleStrategy`, `LLMCategorizationStrategy`, `RecategorizeCommand` |
| F03 | Budgets and alerts | `BudgetService`, `BudgetCalculator`, `AlertMonitor` |
| F04 | Recurring payments / subscriptions | `RecurringDetector`, `AnalyticsService`, `RecurringReviewer` |
| F05 | Safe-to-spend and forecast | `ForecastEngine`, `SafeToSpendCalculator` |
| F06 | Habit detective | `HabitAnalyzer` |
| F07 | Financial health score | `HealthScoreCalculator` |
| F08 | Affordability agent | `AgentController`, `Planner`, `ToolManager`, `GroundingChecker`, `PurchaseAssessor` |
| F09 | Savings goals | `GoalService`, `GoalPlanner` |
| F10 | What-if scenarios | `ScenarioParser`, `ScenarioSimulator` |
| F11 | Monthly review | `ReviewService` |
| F12 | Chat with memory | `ConversationMemory`, `UserProfileMemory`, `MemoryManager` |
| F13 | Proposals needing approval | `ProposalService`, `Proposal` and its states, `CommandFactory` |
| F14 | Report export | `ReportService`, `ReportExporter` and subclasses |

Design patterns: Facade (`WealthSeiFacade`), Strategy (categorizers), Adapter (CSV and Claude API), Observer
(`AlertMonitor`), Command (`CommandHistory`), State (proposals), Decorator (`Persistent*` repositories, retrying and
logging LLM providers), Template Method (`ReportExporter`), Factory (`WealthSeiFactory`, `CommandFactory`),
Prototype (`ForecastInputs.deepCopy`).

Package layout under `src/main/java/wealthsei`: `domain`, `importing`, `repository`, `service`, `analytics`,
`command`, `llm`, `agent`, `report`, `facade`, `presentation/cli`, `presentation/gui`.

## How the AI is used (and kept honest)

- The AI never invents numbers: budgets, forecasts, scores and goal feasibility are computed by code; the agent
  reads them through tools, and a grounding check rejects an answer containing a figure that no tool returned
  (one correction attempt, then the answer is flagged).
- `trace` (and the Assistant's trace button) shows which tools were called and where each figure came from.
- `aicalls` shows the recent AI calls (model, tokens, time). Costs and the development-time AI use are in the
  Stage 2 report and in [`docs/ai-log.md`](docs/ai-log.md) and [`docs/ai-usage.md`](docs/ai-usage.md).

## Project documents

- [`docs/stage1-conformance.md`](docs/stage1-conformance.md): Stage 1 classes and methods vs. the code, and what was added or changed.
- [`docs/ai-log.md`](docs/ai-log.md): AI collaboration log.
- [`docs/ai-usage.md`](docs/ai-usage.md) and `scripts/ai-usage.sh`: how AI token usage is totalled.

## Demonstration videos

Also indexed by feature in the Stage 2 report (Section 7). All videos use invented demo data only.

| ID | Feature | GUI demo | CLI demo |
|---|---|---|---|
| F01 | Import transactions (CSV) | [GUI video](PASTE_LINK_F01_GUI) | [CLI video](PASTE_LINK_F01_CLI) |
| F02 | Categorization and corrections | [GUI video](PASTE_LINK_F02_GUI) | [CLI video](PASTE_LINK_F02_CLI) |
| F03 | Budgets and alerts | [GUI video](PASTE_LINK_F03_GUI) | [CLI video](PASTE_LINK_F03_CLI) |
| F04 | Recurring payments / subscriptions | [GUI video](PASTE_LINK_F04_GUI) | [CLI video](PASTE_LINK_F04_CLI) |
| F05 | Safe-to-spend and forecast | [GUI video](PASTE_LINK_F05_GUI) | [CLI video](PASTE_LINK_F05_CLI) |
| F06 | Habit detective | [GUI video](PASTE_LINK_F06_GUI) | [CLI video](PASTE_LINK_F06_CLI) |
| F07 | Financial health score | [GUI video](PASTE_LINK_F07_GUI) | [CLI video](PASTE_LINK_F07_CLI) |
| F08 | Affordability agent | [GUI video](PASTE_LINK_F08_GUI) | [CLI video](PASTE_LINK_F08_CLI) |
| F09 | Savings goals | [GUI video](PASTE_LINK_F09_GUI) | [CLI video](PASTE_LINK_F09_CLI) |
| F10 | What-if scenarios | [GUI video](PASTE_LINK_F10_GUI) | [CLI video](PASTE_LINK_F10_CLI) |
| F11 | Monthly review | [GUI video](PASTE_LINK_F11_GUI) | [CLI video](PASTE_LINK_F11_CLI) |
| F12 | Chat with memory | [GUI video](PASTE_LINK_F12_GUI) | [CLI video](PASTE_LINK_F12_CLI) |
| F13 | Proposals needing approval | [GUI video](PASTE_LINK_F13_GUI) | [CLI video](PASTE_LINK_F13_CLI) |
| F14 | Report export | [GUI video](PASTE_LINK_F14_GUI) | [CLI video](PASTE_LINK_F14_CLI) |

## Safety

No API keys or real financial data are in this repository. `.env` and private statement files are git-ignored.
All data under `demo/` is invented.
