package wealthsei.repository;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import wealthsei.domain.Categories;

class InMemoryRuleRepositoryTest {

    private final InMemoryRuleRepository repo = new InMemoryRuleRepository();

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
        assertEquals(1, repo.findAll().size());
        assertEquals(Categories.ENTERTAINMENT, repo.find("cafe").orElseThrow());
        repo.delete("CAFE");
        assertTrue(repo.find("cafe").isEmpty());
    }

    @Test
    void findAllListsRulesInTheOrderTheyWereSaved() {
        repo.save("Zed", wealthsei.domain.Categories.SHOPPING);
        repo.save("Alpha", wealthsei.domain.Categories.DINING);
        assertEquals(java.util.List.of("zed", "alpha"),
                repo.findAll().stream().map(wealthsei.domain.LearnedRule::merchant).toList());
    }
}
