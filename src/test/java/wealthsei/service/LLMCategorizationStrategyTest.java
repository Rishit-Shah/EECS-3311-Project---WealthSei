package wealthsei.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Categories;
import wealthsei.domain.Category;
import wealthsei.domain.Money;
import wealthsei.domain.Transaction;
import wealthsei.llm.LLMException;
import wealthsei.llm.LLMResponse;
import wealthsei.llm.MockLLMProvider;

class LLMCategorizationStrategyTest {

    private static Transaction tx(String merchant, boolean income) {
        return new Transaction("t", LocalDate.of(2026, 10, 1), Money.of("12.34"), merchant,
                merchant + " TORONTO CAN 4111", income, Category.UNCATEGORIZED);
    }

    @Test
    void usesTheModelsAnswerWhenItIsAValidCategory() {
        MockLLMProvider model = new MockLLMProvider(LLMResponse.text("dining"));
        var s = new LLMCategorizationStrategy(model, "haiku");
        assertEquals(Categories.DINING, s.categorize(tx("Rfbt The Tenor", false)).orElseThrow());
    }

    @Test
    void onlyTheMerchantNameIsSent() {
        MockLLMProvider model = new MockLLMProvider(LLMResponse.text("dining"));
        new LLMCategorizationStrategy(model, "haiku").categorize(tx("Rfbt The Tenor", false));
        String sent = model.requests().get(0).messages().get(0).text();
        assertEquals("Merchant: Rfbt The Tenor", sent);
        assertFalse(sent.contains("12.34"));
        assertFalse(sent.contains("4111"));
        assertEquals("haiku", model.requests().get(0).model());
    }

    @Test
    void unknownOrInventedAnswersAreIgnored() {
        MockLLMProvider model = new MockLLMProvider(LLMResponse.text("unknown"), LLMResponse.text("crypto"),
                LLMResponse.text("income"), LLMResponse.text("Dining is my guess because it sounds like food"));
        var s = new LLMCategorizationStrategy(model, "m");
        assertTrue(s.categorize(tx("A", false)).isEmpty());
        assertTrue(s.categorize(tx("B", false)).isEmpty());
        assertTrue(s.categorize(tx("C", false)).isEmpty());
        assertTrue(s.categorize(tx("D", false)).isEmpty());
    }

    @Test
    void quotesCapitalsAndFullStopsAreTolerated() {
        MockLLMProvider model = new MockLLMProvider(LLMResponse.text(" \"Shopping\". "));
        var s = new LLMCategorizationStrategy(model, "m");
        assertEquals(Categories.SHOPPING, s.categorize(tx("Zzz", false)).orElseThrow());
    }

    @Test
    void eachMerchantIsAskedAboutOnlyOnce() {
        MockLLMProvider model = new MockLLMProvider(LLMResponse.text("dining"));
        var s = new LLMCategorizationStrategy(model, "m");
        s.categorize(tx("Rfbt The Tenor", false));
        s.categorize(tx("RFBT THE TENOR", false));
        assertEquals(1, model.requests().size());
    }

    @Test
    void incomeIsNeverSentToTheModel() {
        MockLLMProvider model = new MockLLMProvider(LLMResponse.text("dining"));
        assertTrue(new LLMCategorizationStrategy(model, "m").categorize(tx("Payroll", true)).isEmpty());
        assertTrue(model.requests().isEmpty());
    }

    @Test
    void afterTwoFailuresInARowItStopsAsking() {
        MockLLMProvider model = new MockLLMProvider()
                .thenFail(new LLMException("down", true)).thenFail(new LLMException("down", true))
                .thenReply(LLMResponse.text("dining"));
        var s = new LLMCategorizationStrategy(model, "m");
        assertTrue(s.categorize(tx("A", false)).isEmpty());
        assertTrue(s.categorize(tx("B", false)).isEmpty());
        assertTrue(s.categorize(tx("C", false)).isEmpty());
        assertEquals(2, model.requests().size());
    }

    @Test
    void aFailureDoesNotPoisonTheCacheAndASuccessResetsTheCount() {
        MockLLMProvider model = new MockLLMProvider()
                .thenFail(new LLMException("blip", true)).thenReply(LLMResponse.text("transport"));
        var s = new LLMCategorizationStrategy(model, "m");
        assertTrue(s.categorize(tx("A", false)).isEmpty());
        assertEquals(Categories.TRANSPORT, s.categorize(tx("A", false)).orElseThrow());
    }
}
