package wealthsei.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A scripted model for tests and offline work: returns the prepared replies in order and records what it was asked. */
public class MockLLMProvider implements LLMProvider {

    private final List<Object> script = new ArrayList<>();
    private final List<LLMRequest> requests = new ArrayList<>();

    public MockLLMProvider(LLMResponse... replies) {
        script.addAll(List.of(replies));
    }

    public MockLLMProvider(List<LLMResponse> replies) {
        script.addAll(replies);
    }

    /** Adds a reply to the end of the script. */
    public MockLLMProvider thenReply(LLMResponse reply) {
        script.add(Objects.requireNonNull(reply, "reply"));
        return this;
    }

    /** Adds a failure to the script: the call at this point throws. */
    public MockLLMProvider thenFail(LLMException failure) {
        script.add(Objects.requireNonNull(failure, "failure"));
        return this;
    }

    @Override
    public LLMResponse complete(LLMRequest request) {
        requests.add(Objects.requireNonNull(request, "request"));
        if (script.isEmpty()) {
            throw new IllegalStateException("The mock model has no more scripted replies");
        }
        Object next = script.remove(0);
        if (next instanceof LLMException e) {
            throw e;
        }
        return (LLMResponse) next;
    }

    /** Every request received so far, oldest first. */
    public List<LLMRequest> requests() {
        return List.copyOf(requests);
    }
}
