package wealthsei.importing;

/** The file cannot be read, is empty, or its header matches no supported bank layout. */
public class UnsupportedFormatException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UnsupportedFormatException(String message) {
        super(message);
    }

    public UnsupportedFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
