package wealthsei.repository;

import java.util.List;
import java.util.Locale;

/**
 * Remembers which recurring payments the user flagged for review. Merchants are compared
 * ignoring case and surrounding spaces.
 */
public interface RecurringRepository {

    void saveFlag(String merchant, boolean flagged);

    boolean isFlagged(String merchant);

    /** The normalised names of all flagged merchants. */
    List<String> flaggedMerchants();

    /** The form of a merchant name used as the lookup key. */
    static String keyOf(String merchant) {
        return merchant.trim().toLowerCase(Locale.ROOT);
    }
}
