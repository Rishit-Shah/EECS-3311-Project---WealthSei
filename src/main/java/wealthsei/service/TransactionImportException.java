package wealthsei.service;

/** An import failed as a whole and nothing was saved. The message is safe to show the user. */
public class TransactionImportException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public TransactionImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
