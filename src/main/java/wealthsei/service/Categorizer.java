package wealthsei.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.Category;
import wealthsei.domain.Transaction;

/**
 * Tries each {@link CategorizationStrategy} in order and uses the first answer. If none can
 * decide, the transaction is {@link Category#UNCATEGORIZED} and left for manual review.
 */
public class Categorizer {

    private final List<CategorizationStrategy> strategies;

    public Categorizer(List<CategorizationStrategy> strategies) {
        this.strategies = List.copyOf(Objects.requireNonNull(strategies, "strategies"));
    }

    public Category categorize(Transaction tx) {
        for (CategorizationStrategy strategy : strategies) {
            Optional<Category> answer = strategy.categorize(tx);
            if (answer.isPresent()) {
                return answer.get();
            }
        }
        return Category.UNCATEGORIZED;
    }
}
