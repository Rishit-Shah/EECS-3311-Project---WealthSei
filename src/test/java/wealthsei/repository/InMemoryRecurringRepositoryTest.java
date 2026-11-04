package wealthsei.repository;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class InMemoryRecurringRepositoryTest {

    private final InMemoryRecurringRepository repo = new InMemoryRecurringRepository();

    @Test
    void flagsIgnoreCaseAndSpaces() {
        repo.saveFlag("  Netflix ", true);
        assertTrue(repo.isFlagged("NETFLIX"));
        assertEquals(1, repo.flaggedMerchants().size());
    }

    @Test
    void clearingAFlagRemovesIt() {
        repo.saveFlag("Netflix", true);
        repo.saveFlag("netflix", false);
        assertFalse(repo.isFlagged("Netflix"));
        assertTrue(repo.flaggedMerchants().isEmpty());
    }

    @Test
    void clearingAFlagThatWasNeverSetIsHarmless() {
        repo.saveFlag("nothing", false);
        assertTrue(repo.flaggedMerchants().isEmpty());
    }
}
