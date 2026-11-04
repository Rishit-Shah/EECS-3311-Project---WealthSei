package wealthsei.repository.file;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import wealthsei.domain.GoalStatus;
import wealthsei.domain.Money;
import wealthsei.domain.SavingsGoal;
import wealthsei.repository.InMemoryGoalRepository;

class PersistentGoalRepositoryTest {

    private static SavingsGoal goal(String id, String name, GoalStatus status) {
        return new SavingsGoal(id, name, Money.of("1500"), LocalDate.of(2027, 6, 1), Money.of("200"),
                Money.of("75.50"), status);
    }

    @Test
    void goalsSurviveARestart() throws IOException {
        Path file = Files.createTempDirectory("wealthsei-goals").resolve("goals.tsv");
        PersistentGoalRepository first = new PersistentGoalRepository(new InMemoryGoalRepository(), file);
        first.save(goal("a", "Laptop", GoalStatus.AT_RISK));
        first.save(goal("b", "Trip\twith tab", GoalStatus.ON_TRACK));
        first.save(goal("a", "Laptop", GoalStatus.BEHIND));

        PersistentGoalRepository second = new PersistentGoalRepository(new InMemoryGoalRepository(), file);
        assertEquals(2, second.findAll().size());
        assertEquals(GoalStatus.BEHIND, second.findById("a").orElseThrow().status());
        assertEquals("Trip\twith tab", second.findById("b").orElseThrow().name());
        assertEquals(Money.of("75.50"), second.findById("a").orElseThrow().monthlyContribution());
    }

    @Test
    void aMissingFileMeansNoGoals() throws IOException {
        Path file = Files.createTempDirectory("wealthsei-goals").resolve("goals.tsv");
        assertTrue(new PersistentGoalRepository(new InMemoryGoalRepository(), file).findAll().isEmpty());
    }

    @Test
    void aDamagedFileIsReportedWithItsLine() throws IOException {
        Path file = Files.createTempFile("wealthsei-goals", ".tsv");
        Files.writeString(file, "a\tLaptop\tnot-money\t2027-06-01\t0\t0\tON_TRACK\n");
        StorageException e = assertThrows(StorageException.class,
                () -> new PersistentGoalRepository(new InMemoryGoalRepository(), file));
        assertTrue(e.getMessage().contains("line 1"));
    }
}
