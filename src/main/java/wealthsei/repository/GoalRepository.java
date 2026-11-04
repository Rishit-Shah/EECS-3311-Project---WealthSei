package wealthsei.repository;

import java.util.List;
import java.util.Optional;
import wealthsei.domain.SavingsGoal;

/** Storage for savings goals. */
public interface GoalRepository {

    /** Saves a new goal or replaces the goal with the same id. */
    void save(SavingsGoal goal);

    /** All goals in the order they were first saved. */
    List<SavingsGoal> findAll();

    Optional<SavingsGoal> findById(String goalId);
}
