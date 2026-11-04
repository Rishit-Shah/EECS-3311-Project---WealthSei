package wealthsei.repository.file;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.Category;
import wealthsei.domain.LearnedRule;
import wealthsei.repository.RuleRepository;

/** Adds saving to a file on top of any {@link RuleRepository} (Decorator pattern). */
public class PersistentRuleRepository implements RuleRepository {

    private final RuleRepository inner;
    private final Path file;

    public PersistentRuleRepository(RuleRepository inner, Path file) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.file = Objects.requireNonNull(file, "file");
        for (TsvFile.Row row : TsvFile.read(file)) {
            FileCodec.parse(file, row, r -> {
                Category category = FileCodec.readCategory(r, 1);
                if (category == null) {
                    throw new IllegalArgumentException("missing category");
                }
                inner.save(FileCodec.field(r, 0), category);
                return category;
            });
        }
    }

    @Override
    public void save(String merchant, Category category) {
        inner.save(merchant, category);
        persist();
    }

    @Override
    public void delete(String merchant) {
        inner.delete(merchant);
        persist();
    }

    @Override
    public Optional<Category> find(String merchant) {
        return inner.find(merchant);
    }

    @Override
    public List<LearnedRule> findAll() {
        return inner.findAll();
    }

    private void persist() {
        List<List<String>> rows = new ArrayList<>();
        for (LearnedRule rule : inner.findAll()) {
            List<String> row = new ArrayList<>();
            row.add(rule.merchant());
            FileCodec.addCategory(row, rule.category());
            rows.add(row);
        }
        TsvFile.write(file, rows);
    }
}
