package wealthsei.report;

/** The report could not be written, for example because the folder does not exist or is read-only. */
public class ExportException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ExportException(String message) {
        super(message);
    }

    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
