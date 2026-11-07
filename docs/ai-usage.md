# WealthSei - AI token and cost tracking (Stage 2)

This file keeps the numbers for Section 5 of the report ("AI tools: model, tasks, token cost, money cost") in one place. It has three parts: what the app itself used when running, what building the project with Claude used, and how the numbers were obtained.

## 1. The app at runtime (calls made by WealthSei)

Every model call is recorded in `llm.log` in the data folder (model, token total and time only: never the key, the prompts or any transactions). The script `scripts/ai-usage.sh` totals them:

```
bash scripts/ai-usage.sh
```

| Model | Used for | Calls | Tokens | Money cost (Console) |
|---|---|---|---|---|
| `claude-haiku-5-5` | F02 category for an unknown merchant; F04 recurring-payments review | (from the script) | (from the script) | (from the Console) |
| `claude-sonnet-5-5` | Tool-using agent: F08 can I afford it, F10 what-if, F11 monthly review, F12 chat, F13 proposals; explanations in F05, F06, F07 | (from the script) | (from the script) | (from the Console) |
| **Total** | | | | |

Notes:
- The log has one token total per call, not input and output separately, so the money cost comes from the Console, which does split them.
- Folders that were deleted between recordings lost their logs, so the script's totals cover only the logs that still exist. The Console totals cover everything the key did, including testing.
- Typical size of one call: a categorization is about 175 tokens (Haiku); a recurring review about 400 (Haiku); an agent answer is usually two or three calls and about 5,000 tokens in total (Sonnet), for example 1,859 + 3,126 for one habits question.

Console figures (fill in at submission):

| Item | Value |
|---|---|
| Date range read | |
| `claude-haiku-5-5` input / output tokens | |
| `claude-haiku-5-5` cost | |
| `claude-sonnet-5-5` input / output tokens | |
| `claude-sonnet-5-5` cost | |
| Total runtime cost | |

## 2. Building the project with Claude (development)

| Item | Value |
|---|---|
| Tool and model | Claude, chat with code tools; `claude-sonnet-5-5` |
| Stage 2 up to the 2026-10-20 ~22:50 checkpoint | about $5.27 API-equivalent (about 15K plain input, 63K output, 10.1M cache-read and 1.07M cache-write tokens) |
| Work after that checkpoint (GUI, demo data, videos, fixes) | not yet totalled: fill in at submission |
| Total development cost | |

The figure is a computed API price, not necessarily what was billed (it depends on the plan used). Stage 1 usage is not included.

## 3. How the numbers were obtained

- Runtime calls and tokens: `llm.log` and `scripts/ai-usage.sh`, and the CLI command `aicalls` (shows the last calls, the total for the current session and the whole log).
- Runtime money cost: the Anthropic Console, usage and cost pages, grouped by model and filtered to the days used and the key used by WealthSei.
- Development cost: the usage figure of the Claude session used for the work.
