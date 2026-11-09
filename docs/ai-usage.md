# WealthSei - AI token and cost tracking (Stage 2)

This file keeps the numbers for Section 5 of the report ("AI tools: model, tasks, token cost, money cost") in one place. It has three parts: what the app itself used when running, what building the project with Claude used, and how the numbers were obtained.

## 1. The app at runtime (calls made by WealthSei)

Every model call is recorded in `llm.log` in the data folder (model, token total and time only: never the key, the prompts or any transactions). The script `scripts/ai-usage.sh` totals them:

```
bash scripts/ai-usage.sh
```

| Model | Used for | Calls | Tokens | Money cost (Console) |
|---|---|---|---|---|
| `claude-haiku-5-5` | F02 category for an unknown merchant; F04 recurring-payments review | 28 (kept logs) | 5,889 (kept logs); Console 57,404 | part of $1.00 total |
| `claude-sonnet-5-5` | Tool-using agent: F08 can I afford it, F10 what-if, F11 monthly review, F12 chat, F13 proposals; explanations in F05, F06, F07 | 20 (kept logs) | 44,589 (kept logs); Console 362,079 | part of $1.00 total |
| **Total** | | 48 (kept logs) | 50,478 (kept logs); Console 419,483 | $1.00 |

Notes:
- The log has one token total per call, not input and output separately, so the money cost comes from the Console, which does split them.
- Folders that were deleted between recordings lost their logs, so the script's totals cover only the logs that still exist. The Console totals cover everything the key did, including testing.
- Typical size of one call: a categorization is about 175 tokens (Haiku); a recurring review about 400 (Haiku); an agent answer is usually two or three calls and about 5,000 tokens in total (Sonnet), for example 1,859 + 3,126 for one habits question.

Console figures (usage export read on Nov 9 2026, data as of 6:51 PM UTC; usage on Nov 5-7 UTC, no cache tokens, no web searches):

| Item | Value |
|---|---|
| Date range read | Nov 1 - Nov 9 2026 (UTC); usage on Nov 5, 6 and 7 |
| `claude-haiku-5-5` input / output tokens | 47,702 / 9,702 |
| `claude-haiku-5-5` cost | not split by model on the dashboard |
| `claude-sonnet-5-5` input / output tokens | 338,620 / 23,459 |
| `claude-sonnet-5-5` cost | not split by model on the dashboard |
| Total runtime cost | $1.00 (Console dashboard, "Spend this month", as of Nov 9 6:51 PM UTC) |

## 2. Building the project with Claude (development)

| Item | Value |
|---|---|
| Tool and model | Claude, chat with code tools; `claude-sonnet-5-5` |
| Early checkpoint (2026-10-20 ~22:50) | about $5.27 API-equivalent (about 15K plain input, 63K output, 10.1M cache-read and 1.07M cache-write tokens); included in the total below |
| Whole session, Oct 20 21:34 to Nov 9 14:41 (Toronto) | about 321.1M tokens: 49,951 uncached input, 7,827,351 cache writes, 311,583,766 cache reads, 1,665,363 output; $78.51 API-equivalent |
| Total development cost | $78.51 API-equivalent as of Nov 9 14:41 |

The figure is a computed API price, not necessarily what was billed (it depends on the plan used). Stage 1 usage is not included.

## 3. How the numbers were obtained

- Runtime calls and tokens: `llm.log` and `scripts/ai-usage.sh`, and the CLI command `aicalls` (shows the last calls, the total for the current session and the whole log).
- Runtime money cost: the Anthropic Console, usage and cost pages, grouped by model and filtered to the days used and the key used by WealthSei.
- Development cost: the usage figure of the Claude session used for the work.
