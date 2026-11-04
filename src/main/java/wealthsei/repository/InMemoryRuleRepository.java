package wealthsei.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.Category;
import wealthsei.domain.LearnedRule;

/** Keeps learned rules in memory. */
public class InMemoryRuleRepository implements RuleRepository {

    private final Map<String, Category> rules = new LinkedHashMap<>();

    @Override
    public void save(String merchant, Category category) {
        Objects.requireNonNull(category, "category");
        rules.put(RuleRepository.keyOf(merchant), category);
    }

    @Override
    public void delete(String merchant) {
        rules.remove(RuleRepository.keyOf(merchant));
    }

    @Override
    public List<LearnedRule> findAll() {
        return rules.entrySet().stream().map(e -> new LearnedRule(e.getKey(), e.getValue())).toList();
    }

    @Override
    public Optional<Category> find(String merchant) {
        return Optional.ofNullable(rules.get(RuleRepository.keyOf(merchant)));
    }
}
