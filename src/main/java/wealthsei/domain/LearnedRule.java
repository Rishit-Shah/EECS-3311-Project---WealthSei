package wealthsei.domain;

import java.util.Objects;

/**
 * "Transactions from this merchant are always this category", learned from a user correction.
 *
 * @param merchant the merchant name, lower-cased and trimmed
 * @param category the category the user chose
 */
public record LearnedRule(String merchant, Category category) {

    public LearnedRule {
        Objects.requireNonNull(merchant, "merchant");
        Objects.requireNonNull(category, "category");
        if (merchant.isBlank()) {
            throw new IllegalArgumentException("Rule merchant must not be blank");
        }
    }
}
