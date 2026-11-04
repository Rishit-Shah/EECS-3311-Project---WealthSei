package wealthsei.repository.file;

import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.Budget;
import wealthsei.domain.BudgetLine;
import wealthsei.domain.Category;
import wealthsei.domain.Money;
import wealthsei.repository.BudgetRepository;

/** Adds saving to a file on top of any {@link BudgetRepository} (Decorator pattern). */
public class PersistentBudgetRepository implements BudgetRepository {

    private final BudgetRepository inner;
    private final Path file;

    public PersistentBudgetRepository(BudgetRepository inner, Path file) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.file = Objects.requireNonNull(file, "file");
        Map<YearMonth, List<BudgetLine>> byMonth = new LinkedHashMap<>();
        for (TsvFile.Row row : TsvFile.read(file)) {
            FileCodec.parse(file, row, r -> {
                YearMonth month = YearMonth.parse(FileCodec.field(r, 0));
                Category category = FileCodec.readCategory(r, 1);
                if (category == null) {
                    throw new IllegalArgumentException("missing category");
                }
                byMonth.computeIfAbsent(month, m -> new ArrayList<>())
                        .add(new BudgetLine(category, Money.of(FileCodec.field(r, 4))));
                return month;
            });
        }
        byMonth.forEach((month, lines) -> inner.save(new Budget(month, lines)));
    }

    @Override
    public void save(Budget budget) {
        inner.save(budget);
        List<List<String>> rows = new ArrayList<>();
        for (Budget b : inner.findAll()) {
            for (BudgetLine line : b.lines()) {
                List<String> row = new ArrayList<>();
                row.add(b.month().toString());
                FileCodec.addCategory(row, line.category());
                row.add(line.limit().toString());
                rows.add(row);
            }
        }
        TsvFile.write(file, rows);
    }

    @Override
    public Optional<Budget> find(YearMonth month) {
        return inner.find(month);
    }

    @Override
    public List<Budget> findAll() {
        return inner.findAll();
    }
}
