package wealthsei.llm;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Records each call (model, time taken, tokens used or the failure) and keeps running totals,
 * which is how the app reports its own AI usage. It never logs prompts, replies or keys.
 */
public class LoggingLLMProvider extends LLMProviderDecorator {

    private final Consumer<String> log;
    private long calls;
    private long failures;
    private long tokens;

    public LoggingLLMProvider(LLMProvider inner, Consumer<String> log) {
        super(inner);
        this.log = Objects.requireNonNull(log, "log");
    }

    @Override
    public LLMResponse complete(LLMRequest request) {
        long start = System.nanoTime();
        calls++;
        try {
            LLMResponse response = inner.complete(request);
            tokens += response.tokensUsed();
            log.accept("LLM call " + calls + " model=" + request.model() + " tokens=" + response.tokensUsed()
                    + " ms=" + (System.nanoTime() - start) / 1_000_000);
            return response;
        } catch (LLMException e) {
            failures++;
            log.accept("LLM call " + calls + " model=" + request.model() + " FAILED: " + e.getMessage());
            throw e;
        }
    }

    public long calls() {
        return calls;
    }

    public long failures() {
        return failures;
    }

    public long totalTokens() {
        return tokens;
    }
}
