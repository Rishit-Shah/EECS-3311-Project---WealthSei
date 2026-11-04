package wealthsei.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import wealthsei.domain.GoalStatus;
import wealthsei.domain.Money;
import wealthsei.domain.SavingsGoal;

class InMemoryGoalRepositoryTest {

    private final InMemoryGoalRepository repo = new InMemoryGoalRepository();

    private static SavingsGoal goal(String id, String saved) {
        return new SavingsGoal(id, "Goal " + id, Money.of("1000"), LocalDate.of(2027, 1, 1), Money.of(saved),
                Money.of("50"), GoalStatus.ON_TRACK);
    }

    @Test
    void startsEmptyAndFindsNothing() {
        assertTrue(repo.findAll().isEmpty());
        assertTrue(repo.findById("x").isEmpty());
    }

    @Test
    void savedGoalsAreFoundInInsertionOrder() {
        repo.save(goal("b", "0"));
        repo.save(goal("a", "0"));
        assertEquals(java.util.List.of("b", "a"), repo.findAll().stream().map(SavingsGoal::id).toList());
        assertEquals("Goal a", repo.findById("a").orElseThrow().name());
    }

    @Test
    void savingAgainReplacesTheGoalWithTheSameId() {
        repo.save(goal("a", "0"));
        repo.save(goal("a", "300"));
        assertEquals(1, repo.findAll().size());
        assertEquals(Money.of("300"), repo.findById("a").orElseThrow().saved());
    }
}
