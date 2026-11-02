package wealthsei.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Categories;
import wealthsei.domain.Category;
import wealthsei.domain.Money;
import wealthsei.domain.Transaction;

class CategorizerTest {

    private static Transaction spend(String merchant) {
        return new Transaction("t", LocalDate.of(2026, 10, 1), Money.of("10"), merchant, merchant.toUpperCase(),
                false, Category.UNCATEGORIZED);
    }

    @Test
    void keywordsMatchWholeWordsIgnoringCase() {
        KeywordRuleStrategy s = KeywordRuleStrategy.withDefaults();
        assertEquals(Categories.GROCERIES, s.categorize(spend("Loblaws Toronto")).orElseThrow());
        assertEquals(Categories.DINING, s.categorize(spend("Tim Hortons")).orElseThrow());
        assertEquals(Categories.TRANSPORT, s.categorize(spend("Presto Fare")).orElseThrow());
        assertEquals(Categories.SUBSCRIPTIONS, s.categorize(spend("Netflix")).orElseThrow());
    }

    @Test
    void doesNotMatchInsideLongerWords() {
        KeywordRuleStrategy s = KeywordRuleStrategy.withDefaults();
        assertTrue(s.categorize(spend("Metropolis Books")).isEmpty());
        assertTrue(s.categorize(spend("Bellevue Cafe")).isEmpty());
    }

    @Test
    void earlierKeywordsWinSoUberEatsIsNotTransport() {
        KeywordRuleStrategy s = KeywordRuleStrategy.withDefaults();
        assertEquals(Categories.DINING, s.categorize(spend("Uber Eats")).orElseThrow());
        assertEquals(Categories.TRANSPORT, s.categorize(spend("Uber Trip")).orElseThrow());
    }

    @Test
    void incomeIsAlwaysIncome() {
        Transaction pay = new Transaction("p", LocalDate.of(2026, 10, 1), Money.of("100"), "Acme", "ACME",
                true, Category.UNCATEGORIZED);
        assertEquals(Categories.INCOME, KeywordRuleStrategy.withDefaults().categorize(pay).orElseThrow());
    }

    @Test
    void unknownMerchantGivesNoAnswer() {
        assertTrue(KeywordRuleStrategy.withDefaults().categorize(spend("Zxqv Widgets")).isEmpty());
    }

    @Test
    void customKeywordsWork() {
        KeywordRuleStrategy s = new KeywordRuleStrategy(Map.of("zxqv", Categories.SHOPPING));
        assertEquals(Categories.SHOPPING, s.categorize(spend("Zxqv Widgets")).orElseThrow());
    }

    @Test
    void categorizerUsesTheFirstStrategyThatAnswers() {
        CategorizationStrategy first = tx -> Optional.empty();
        CategorizationStrategy second = tx -> Optional.of(Categories.HEALTH);
        CategorizationStrategy third = tx -> Optional.of(Categories.RENT);
        assertEquals(Categories.HEALTH, new Categorizer(List.of(first, second, third)).categorize(spend("x")));
    }

    @Test
    void categorizerFallsBackToUncategorized() {
        assertEquals(Category.UNCATEGORIZED, new Categorizer(List.of(tx -> Optional.empty())).categorize(spend("x")));
        assertEquals(Category.UNCATEGORIZED, new Categorizer(List.of()).categorize(spend("x")));
    }
}
