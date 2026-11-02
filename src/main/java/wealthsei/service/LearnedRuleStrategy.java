package wealthsei.service;

import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.Category;
import wealthsei.domain.Transaction;
import wealthsei.repository.LearnedRuleRepository;

/**
 * Uses the user's own earlier corrections. Put it before the keyword rules so a correction
 * wins over a built-in guess. Income is left to the other strategies.
 */
public class LearnedRuleStrategy implements CategorizationStrategy {

    private final LearnedRuleRepository rules;

    public LearnedRuleStrategy(LearnedRuleRepository rules) {
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    @Override
    public Optional<Category> categorize(Transaction tx) {
        if (tx.income()) {
            return Optional.empty();
        }
        return rules.find(tx.merchant());
    }
}
