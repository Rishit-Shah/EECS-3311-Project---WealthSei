package wealthsei.llm;

/** Stands in when there is no key: every call fails with a clear reason, so callers use their templates. */
public class UnavailableLLMProvider implements LLMProvider {

    private final String reason;

    public UnavailableLLMProvider(String reason) {
        this.reason = reason;
    }

    public UnavailableLLMProvider() {
        this("AI features are off: no API key found. Put ANTHROPIC_API_KEY in the .env file.");
    }

    @Override
    public LLMResponse complete(LLMRequest request) {
        throw new LLMException(reason, false);
    }
}
