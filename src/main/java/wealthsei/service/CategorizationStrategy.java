package wealthsei.service;

import java.util.Optional;
import wealthsei.domain.Category;
import wealthsei.domain.Transaction;

/**
 * One way of choosing a category (Strategy pattern). Returns empty when it cannot decide, so
 * the {@link Categorizer} can try the next strategy.
 */
public interface CategorizationStrategy {
    Optional<Category> categorize(Transaction tx);
}
