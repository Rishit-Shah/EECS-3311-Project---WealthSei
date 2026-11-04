package wealthsei.repository.file;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import wealthsei.repository.RecurringRepository;

/** Adds saving to a file on top of any {@link RecurringRepository} (Decorator pattern). */
public class PersistentRecurringRepository implements RecurringRepository {

    private final RecurringRepository inner;
    private final Path file;

    public PersistentRecurringRepository(RecurringRepository inner, Path file) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.file = Objects.requireNonNull(file, "file");
        for (TsvFile.Row row : TsvFile.read(file)) {
            FileCodec.parse(file, row, r -> {
                inner.setFlagged(FileCodec.field(r, 0), true);
                return r;
            });
        }
    }

    @Override
    public void setFlagged(String merchant, boolean flagged) {
        inner.setFlagged(merchant, flagged);
        List<List<String>> rows = new ArrayList<>();
        for (String key : new TreeSet<>(inner.flagged())) {
            rows.add(List.of(key));
        }
        TsvFile.write(file, rows);
    }

    @Override
    public boolean isFlagged(String merchant) {
        return inner.isFlagged(merchant);
    }

    @Override
    public Set<String> flagged() {
        return inner.flagged();
    }
}
