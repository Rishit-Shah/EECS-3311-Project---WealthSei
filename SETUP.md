# WealthSei - setup

Requirements: JDK 17 or newer, Maven 3.9+.

```
mvn test          # build and run all unit tests
```

## API key (needed later, once the LLM code exists)
1. Copy `.env.example` to `.env` (the real key goes only in `.env`).
2. `.env` is listed in `.gitignore` and must never be committed.


## AI features (optional)

The numbers (budgets, forecast, goals, habits, score, affordability rules) are computed by ordinary code and
work with no key. The AI model is used to look at extra data, phrase answers and answer questions.

1. Create a key in the Anthropic Console (console.anthropic.com), and add a few dollars of credit.
2. `cp .env.example .env`, then put the key in `.env` (never in any other file).
3. Run the app from the project folder. The first line says `AI features: on (...)` when the key was found.
4. Try: `afford "laptop" 600`, then `trace` to see which tools were used and where each figure came from.

Without a key the same commands still work: the answer is written from the rules and says the AI explanation is unavailable.
Call details (model, tokens, time; never your questions, answers or the key) are appended to `llm.log` in the data folder.
