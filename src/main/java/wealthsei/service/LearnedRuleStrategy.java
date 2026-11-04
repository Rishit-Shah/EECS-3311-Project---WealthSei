package wealthsei.service;

import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.Category;
import wealthsei.domain.Transaction;
import wealthsei.repository.RuleRepository;

/**
 * Uses the user's own earlier corrections. Put it before the keyword rules so a correction
 * wins over a built-in guess. Income is left to the other strategies. It is also the receiver
 * of the undoable category correction, which teaches it ({@link #learn}) or makes it forget.
 */
public class LearnedRuleStrategy implements CategorizationStrategy {

    private final RuleRepository rules;

    public LearnedRuleStrategy(RuleRepository rules) {
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    @Override
    public Optional<Category> categorize(Transaction tx) {
        if (tx.income()) {
            return Optional.empty();
        }
        return rules.find(tx.merchant());
    }

    /** From now on, transactions from this merchant get this category. */
    public void learn(String merchant, Category category) {
        rules.save(merchant, category);
    }

    /** Removes the rule for this merchant, if any. */
    public void forget(String merchant) {
        rules.delete(merchant);
    }

    /** The category currently learned for a merchant, if any. */
    public Optional<Category> ruleFor(String merchant) {
        return rules.find(merchant);
    }
}
