package wealthsei.repository;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import wealthsei.domain.Categories;

class InMemoryLearnedRuleRepositoryTest {

    private final InMemoryLearnedRuleRepository repo = new InMemoryLearnedRuleRepository();

    @Test
    void findIgnoresCaseAndSurroundingSpaces() {
        repo.save("  Zxqv Widgets ", Categories.SHOPPING);
        assertEquals(Categories.SHOPPING, repo.find("ZXQV WIDGETS").orElseThrow());
    }

    @Test
    void unknownMerchantIsEmpty() {
        assertTrue(repo.find("nobody").isEmpty());
    }

    @Test
    void saveReplacesAndRemoveDeletes() {
        repo.save("cafe", Categories.DINING);
        repo.save("Cafe", Categories.ENTERTAINMENT);
        assertEquals(1, repo.all().size());
        assertEquals(Categories.ENTERTAINMENT, repo.find("cafe").orElseThrow());
        repo.remove("CAFE");
        assertTrue(repo.find("cafe").isEmpty());
    }
}
