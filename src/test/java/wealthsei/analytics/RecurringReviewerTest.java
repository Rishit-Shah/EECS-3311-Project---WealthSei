package wealthsei.analytics;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Frequency;
import wealthsei.domain.Money;
import wealthsei.domain.RecurringPayment;
import wealthsei.llm.LLMException;
import wealthsei.llm.LLMResponse;
import wealthsei.llm.MockLLMProvider;
import wealthsei.llm.UnavailableLLMProvider;

class RecurringReviewerTest {

    private static final String FALLBACK = "template summary";

    private static RecurringPayment spotify() {
        return new RecurringPayment("Spotify", Money.of("11.99"), Frequency.MONTHLY, LocalDate.of(2026, 11, 20),
                0.95, false, Money.of("2.00"), 4);
    }

    private static List<RecurringPayment> payments() {
        return List.of(spotify());
    }

    @Test
    void usesTheModelsWordingWhenItOnlyUsesGivenFigures() {
        MockLLMProvider model = new MockLLMProvider(LLMResponse.text(
                "You have 1 recurring payment costing $11.99 a month. Spotify went up by $2.00, so look at it first."));
        RecurringReview r = new RecurringReviewer(model, "haiku").review(payments(), Money.of("11.99"), FALLBACK);
        assertTrue(r.byAi());
        assertTrue(r.text().contains("Spotify"));
        assertEquals("haiku", model.requests().get(0).model());
    }

    @Test
    void aFigureThatIsNotInTheDataFallsBackToTheTemplate() {
        MockLLMProvider model = new MockLLMProvider(LLMResponse.text("Spotify costs $19.99 a month."));
        RecurringReview r = new RecurringReviewer(model, "haiku").review(payments(), Money.of("11.99"), FALLBACK);
        assertFalse(r.byAi());
        assertEquals(FALLBACK, r.text());
        assertTrue(r.note().contains("not in your data"));
    }

    @Test
    void aFailingModelFallsBackToTheTemplate() {
        MockLLMProvider model = new MockLLMProvider().thenFail(new LLMException("down", true));
        RecurringReview r = new RecurringReviewer(model, "haiku").review(payments(), Money.of("11.99"), FALLBACK);
        assertFalse(r.byAi());
        assertEquals(FALLBACK, r.text());
    }

    @Test
    void withoutAKeyTheTemplateIsReturnedWithTheReason() {
        RecurringReview r = new RecurringReviewer(new UnavailableLLMProvider(), "haiku")
                .review(payments(), Money.of("11.99"), FALLBACK);
        assertFalse(r.byAi());
        assertTrue(r.note().contains("AI features are off"));
    }

    @Test
    void nothingIsSentWhenThereAreNoRecurringPayments() {
        MockLLMProvider model = new MockLLMProvider(LLMResponse.text("unused"));
        RecurringReview r = new RecurringReviewer(model, "haiku").review(List.of(), Money.ZERO, FALLBACK);
        assertFalse(r.byAi());
        assertTrue(model.requests().isEmpty());
    }

    @Test
    void theSameFactsAreAnsweredOnceAndRemembered() {
        MockLLMProvider model = new MockLLMProvider(LLMResponse.text("Spotify is $11.99 a month."));
        RecurringReviewer reviewer = new RecurringReviewer(model, "haiku");
        reviewer.review(payments(), Money.of("11.99"), FALLBACK);
        RecurringReview again = reviewer.review(payments(), Money.of("11.99"), FALLBACK);
        assertTrue(again.byAi());
        assertEquals(1, model.requests().size());
    }

    @Test
    void theRequestHoldsOnlyTheComputedFacts() {
        MockLLMProvider model = new MockLLMProvider(LLMResponse.text("ok"));
        new RecurringReviewer(model, "haiku").review(payments(), Money.of("11.99"), FALLBACK);
        String sent = model.requests().get(0).messages().get(0).text();
        assertTrue(sent.contains("Spotify") && sent.contains("11.99") && sent.contains("price up"));
        assertFalse(sent.contains("4111"));
    }

    @Test
    void numberCheckIgnoresCommasAndFullStops() {
        assertTrue(RecurringReviewer.numbersComeFrom("Total is $1,200.50.", "Monthly total: $1200.50\nCount: 3"));
        assertFalse(RecurringReviewer.numbersComeFrom("About 7 payments.", "Count: 3"));
    }
}
