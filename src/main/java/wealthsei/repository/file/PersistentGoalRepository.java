package wealthsei.repository.file;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.GoalStatus;
import wealthsei.domain.Money;
import wealthsei.domain.SavingsGoal;
import wealthsei.repository.GoalRepository;

/** Adds saving to a file on top of any {@link GoalRepository} (Decorator pattern). */
public class PersistentGoalRepository implements GoalRepository {

    private final GoalRepository inner;
    private final Path file;

    public PersistentGoalRepository(GoalRepository inner, Path file) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.file = Objects.requireNonNull(file, "file");
        for (TsvFile.Row row : TsvFile.read(file)) {
            inner.save(FileCodec.parse(file, row, PersistentGoalRepository::parseGoal));
        }
    }

    private static SavingsGoal parseGoal(TsvFile.Row r) {
        return new SavingsGoal(FileCodec.field(r, 0), FileCodec.field(r, 1), Money.of(FileCodec.field(r, 2)),
                LocalDate.parse(FileCodec.field(r, 3)), Money.of(FileCodec.field(r, 4)),
                Money.of(FileCodec.field(r, 5)), GoalStatus.valueOf(FileCodec.field(r, 6)));
    }

    @Override
    public void save(SavingsGoal goal) {
        inner.save(goal);
        List<List<String>> rows = new ArrayList<>();
        for (SavingsGoal g : inner.findAll()) {
            rows.add(List.of(g.id(), g.name(), g.target().toString(), g.deadline().toString(),
                    g.saved().toString(), g.monthlyContribution().toString(), g.status().name()));
        }
        TsvFile.write(file, rows);
    }

    @Override
    public List<SavingsGoal> findAll() {
        return inner.findAll();
    }

    @Override
    public Optional<SavingsGoal> findById(String goalId) {
        return inner.findById(goalId);
    }
}
