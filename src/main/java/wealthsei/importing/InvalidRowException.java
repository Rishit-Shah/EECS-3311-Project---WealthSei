package wealthsei.importing;

/** One CSV row cannot become a transaction. Rejects only that row, not the whole file. */
public class InvalidRowException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidRowException(String message) {
        super(message);
    }
}
