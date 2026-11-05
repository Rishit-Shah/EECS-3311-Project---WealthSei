package wealthsei.llm;

/** Anything that can answer a request: the real model, a scripted test double, or a wrapper around either. */
public interface LLMProvider {

    /** @throws LLMException if the model cannot be reached or reports an error */
    LLMResponse complete(LLMRequest request);
}
