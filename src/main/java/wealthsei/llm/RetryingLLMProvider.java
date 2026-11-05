package wealthsei.llm;

/** Tries again, with a growing pause, when the failure is the kind that may pass (network trouble, rate limits). */
public class RetryingLLMProvider extends LLMProviderDecorator {

    /** Pauses between tries; tests replace it so they do not wait. */
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private final int maxRetries;
    private final long backoffMillis;
    private final Sleeper sleeper;

    public RetryingLLMProvider(LLMProvider inner, int maxRetries, long backoffMillis) {
        this(inner, maxRetries, backoffMillis, Thread::sleep);
    }

    public RetryingLLMProvider(LLMProvider inner, int maxRetries, long backoffMillis, Sleeper sleeper) {
        super(inner);
        if (maxRetries < 0 || backoffMillis < 0) {
            throw new IllegalArgumentException("Retries and back-off must not be negative");
        }
        this.maxRetries = maxRetries;
        this.backoffMillis = backoffMillis;
        this.sleeper = sleeper;
    }

    @Override
    public LLMResponse complete(LLMRequest request) {
        long pause = backoffMillis;
        for (int attempt = 0; ; attempt++) {
            try {
                return inner.complete(request);
            } catch (LLMException e) {
                if (!e.isRetryable() || attempt >= maxRetries) {
                    throw e;
                }
                try {
                    sleeper.sleep(pause);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
                pause *= 2;
            }
        }
    }
}
