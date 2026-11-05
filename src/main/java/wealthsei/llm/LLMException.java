package wealthsei.llm;

/** The model could not be reached or gave an error. {@code retryable} says whether trying again may help. */
public class LLMException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final boolean retryable;

    public LLMException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public LLMException(String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
