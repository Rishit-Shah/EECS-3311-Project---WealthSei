package wealthsei.repository;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class InMemoryRecurringRepositoryTest {

    private final InMemoryRecurringRepository repo = new InMemoryRecurringRepository();

    @Test
    void flagsIgnoreCaseAndSpaces() {
        repo.setFlagged("  Netflix ", true);
        assertTrue(repo.isFlagged("NETFLIX"));
        assertEquals(1, repo.flagged().size());
    }

    @Test
    void clearingAFlagRemovesIt() {
        repo.setFlagged("Netflix", true);
        repo.setFlagged("netflix", false);
        assertFalse(repo.isFlagged("Netflix"));
        assertTrue(repo.flagged().isEmpty());
    }

    @Test
    void clearingAFlagThatWasNeverSetIsHarmless() {
        repo.setFlagged("nothing", false);
        assertTrue(repo.flagged().isEmpty());
    }
}
