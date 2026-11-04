package wealthsei.repository;

import java.util.Locale;
import java.util.Set;

/**
 * Remembers which recurring payments the user flagged for review. Merchants are compared
 * ignoring case and surrounding spaces.
 */
public interface RecurringRepository {

    void setFlagged(String merchant, boolean flagged);

    boolean isFlagged(String merchant);

    /** The normalised names of all flagged merchants. */
    Set<String> flagged();

    /** The form of a merchant name used as the lookup key. */
    static String keyOf(String merchant) {
        return merchant.trim().toLowerCase(Locale.ROOT);
    }
}
