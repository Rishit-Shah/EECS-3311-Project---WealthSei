package wealthsei.service;

/** The user's input is not acceptable. The message is plain language, safe to show as is. */
public class ValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
