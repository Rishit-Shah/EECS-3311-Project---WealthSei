package wealthsei.llm;

import java.util.Objects;

/** Base for wrappers that add behaviour around another provider (Decorator pattern). Passes calls straight through. */
public abstract class LLMProviderDecorator implements LLMProvider {

    protected final LLMProvider inner;

    protected LLMProviderDecorator(LLMProvider inner) {
        this.inner = Objects.requireNonNull(inner, "inner");
    }

    @Override
    public LLMResponse complete(LLMRequest request) {
        return inner.complete(request);
    }
}
