package wealthsei.llm;

import java.util.function.Consumer;

/** Builds the provider the app uses: logging around retrying around Claude, or an "unavailable" stand-in when there is no key. */
public final class LLMProviders {

    private LLMProviders() {
    }

    public static LLMProvider create(LlmConfig config, Consumer<String> log) {
        if (!config.hasKey()) {
            return new UnavailableLLMProvider();
        }
        return new LoggingLLMProvider(new RetryingLLMProvider(new ClaudeProvider(new JdkHttpTransport(config)), 2, 1000), log);
    }
}
