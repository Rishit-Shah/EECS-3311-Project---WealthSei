package wealthsei.repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import wealthsei.domain.Category;
import wealthsei.domain.LearnedRule;

/**
 * Remembers "this merchant is always this category" rules that come from the user's own
 * corrections. Merchant names are compared ignoring case and surrounding spaces.
 */
public interface RuleRepository {

    void save(String merchant, Category category);

    void delete(String merchant);

    /** All rules, in the order they were first saved. */
    List<LearnedRule> findAll();

    /** The rule for one merchant, if there is one. */
    Optional<Category> find(String merchant);

    /** The form of a merchant name used as the lookup key. */
    static String keyOf(String merchant) {
        return merchant.trim().toLowerCase(Locale.ROOT);
    }
}
