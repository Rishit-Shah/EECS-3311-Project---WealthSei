package wealthsei.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Categories;
import wealthsei.domain.Category;
import wealthsei.domain.Money;
import wealthsei.domain.Transaction;
import wealthsei.repository.InMemoryRuleRepository;

class LearnedRuleStrategyTest {

    private final InMemoryRuleRepository rules = new InMemoryRuleRepository();
    private final LearnedRuleStrategy strategy = new LearnedRuleStrategy(rules);

    private static Transaction tx(String merchant, boolean income) {
        return new Transaction("t1", LocalDate.of(2026, 10, 1), Money.of("5"), merchant, "", income,
                Category.UNCATEGORIZED);
    }

    @Test
    void usesTheLearnedCategoryForAKnownMerchant() {
        rules.save("Starbucks", Categories.ENTERTAINMENT);
        assertEquals(Categories.ENTERTAINMENT, strategy.categorize(tx("STARBUCKS", false)).orElseThrow());
    }

    @Test
    void unknownMerchantAndIncomeAreLeftToOtherStrategies() {
        rules.save("employer", Categories.SHOPPING);
        assertTrue(strategy.categorize(tx("somewhere", false)).isEmpty());
        assertTrue(strategy.categorize(tx("employer", true)).isEmpty());
    }

    @Test
    void aLearnedRuleBeatsTheBuiltInKeywordRules() {
        rules.save("starbucks", Categories.ENTERTAINMENT);
        Categorizer categorizer = new Categorizer(List.of(strategy, KeywordRuleStrategy.withDefaults()));
        assertEquals(Categories.ENTERTAINMENT, categorizer.categorize(tx("Starbucks", false)));
        assertEquals(Categories.DINING, new Categorizer(List.of(KeywordRuleStrategy.withDefaults()))
                .categorize(tx("Starbucks", false)));
    }

    @Test
    void learnThenForgetChangesFutureAnswers() {
        strategy.learn("Cafe X", Categories.DINING);
        assertEquals(Categories.DINING, strategy.categorize(tx("cafe x", false)).orElseThrow());
        assertEquals(Categories.DINING, strategy.ruleFor("CAFE X").orElseThrow());
        strategy.forget("CAFE X");
        assertTrue(strategy.categorize(tx("cafe x", false)).isEmpty());
        assertTrue(strategy.ruleFor("cafe x").isEmpty());
    }
}
