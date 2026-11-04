package wealthsei.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.SavingsGoal;

/** Keeps goals in memory. */
public class InMemoryGoalRepository implements GoalRepository {

    private final Map<String, SavingsGoal> byId = new LinkedHashMap<>();

    @Override
    public void save(SavingsGoal goal) {
        Objects.requireNonNull(goal, "goal");
        byId.put(goal.id(), goal);
    }

    @Override
    public List<SavingsGoal> findAll() {
        return List.copyOf(byId.values());
    }

    @Override
    public Optional<SavingsGoal> findById(String goalId) {
        return Optional.ofNullable(byId.get(goalId));
    }
}
