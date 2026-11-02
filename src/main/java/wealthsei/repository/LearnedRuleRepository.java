package wealthsei.repository;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import wealthsei.domain.Category;

/**
 * Remembers "this merchant is always this category" rules that come from the user's own
 * corrections. Merchant names are compared ignoring case and surrounding spaces.
 */
public interface LearnedRuleRepository {

    void save(String merchant, Category category);

    void remove(String merchant);

    Optional<Category> find(String merchant);

    /** All rules, keyed by the normalised merchant name. */
    Map<String, Category> all();

    /** The form of a merchant name used as the lookup key. */
    static String keyOf(String merchant) {
        return merchant.trim().toLowerCase(Locale.ROOT);
    }
}
