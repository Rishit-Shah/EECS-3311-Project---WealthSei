package wealthsei.importing;

/** The text is not well-formed CSV (for example a quote that is never closed). */
public class CsvFormatException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CsvFormatException(String message) {
        super(message);
    }
}
