package wealthsei.repository.file;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.Category;
import wealthsei.repository.LearnedRuleRepository;

/** Adds saving to a file on top of any {@link LearnedRuleRepository} (Decorator pattern). */
public class PersistentLearnedRuleRepository implements LearnedRuleRepository {

    private final LearnedRuleRepository inner;
    private final Path file;

    public PersistentLearnedRuleRepository(LearnedRuleRepository inner, Path file) {
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
    public void remove(String merchant) {
        inner.remove(merchant);
        persist();
    }

    @Override
    public Optional<Category> find(String merchant) {
        return inner.find(merchant);
    }

    @Override
    public Map<String, Category> all() {
        return inner.all();
    }

    private void persist() {
        List<List<String>> rows = new ArrayList<>();
        inner.all().forEach((merchant, category) -> {
            List<String> row = new ArrayList<>();
            row.add(merchant);
            FileCodec.addCategory(row, category);
            rows.add(row);
        });
        TsvFile.write(file, rows);
    }
}
