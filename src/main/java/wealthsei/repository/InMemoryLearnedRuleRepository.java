package wealthsei.repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.Category;

/** Keeps learned rules in memory. A SQLite version replaces it for the real app. */
public class InMemoryLearnedRuleRepository implements LearnedRuleRepository {

    private final Map<String, Category> rules = new LinkedHashMap<>();

    @Override
    public void save(String merchant, Category category) {
        Objects.requireNonNull(category, "category");
        rules.put(LearnedRuleRepository.keyOf(merchant), category);
    }

    @Override
    public void remove(String merchant) {
        rules.remove(LearnedRuleRepository.keyOf(merchant));
    }

    @Override
    public Optional<Category> find(String merchant) {
        return Optional.ofNullable(rules.get(LearnedRuleRepository.keyOf(merchant)));
    }

    @Override
    public Map<String, Category> all() {
        return Map.copyOf(rules);
    }
}
