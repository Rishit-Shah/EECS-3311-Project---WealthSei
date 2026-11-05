package wealthsei.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import wealthsei.agent.TransactionQueryTool;
import wealthsei.domain.GoalRequest;
import wealthsei.domain.Money;
import wealthsei.facade.WealthSeiFacade;
import wealthsei.facade.WealthSeiFactory;
import wealthsei.llm.LLMException;
import wealthsei.llm.LLMResponse;
import wealthsei.llm.LlmConfig;
import wealthsei.llm.MockLLMProvider;
import wealthsei.llm.ToolArgs;
import wealthsei.llm.ToolCall;

class ReviewServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);
    private static final YearMonth OCT = YearMonth.of(2026, 10);

    private final MockLLMProvider model = new MockLLMProvider();
    private final WealthSeiFacade facade = WealthSeiFactory.inMemory(model, LlmConfig.none(), () -> TODAY);

    ReviewServiceTest() throws IOException {
        Path csv = Files.createTempFile("review", ".csv");
        Files.writeString(csv, "Date,Description,Amount\\n".replace("\\n", "\n")
                + "2026-09-05,Spotify,-10\n2026-09-12,Pizza Place,-250\n"
                + "2026-10-05,Spotify,-10\n2026-10-12,Pizza Place,-300\n2026-10-14,Whole Foods,-80\n2026-10-20,Employer,3000\n");
        csv.toFile().deleteOnExit();
        facade.importTransactions(csv, TODAY);
    }

    private void aiReview(String text) {
        model.thenReply(LLMResponse.toolCall(new ToolCall("tu_t", "transactions", ToolArgs.of("month", "2026-10"))));
        model.thenReply(LLMResponse.text(text));
    }

    @Test
    void theAiWritesTheNarrativeAndItIsSaved() {
        aiReview("What happened: you spent $390.00 in 2026-10.\nWhat went well: nothing broke.\nWhat to fix: not much.");
        ReviewResult r = facade.generateMonthlyReview(OCT);
        assertFalse(r.metricsOnly());
        assertFalse(r.replacedEarlier());
        assertTrue(r.review().narrative().startsWith("What happened"));
        assertFalse(r.review().traceId().isEmpty());
        assertEquals(r.review(), facade.getMonthlyReview(OCT).orElseThrow());
        assertTrue(model.requests().get(0).systemPrompt().contains("What went well:"));
    }

    @Test
    void withoutTheModelTheReviewIsAFixedSummaryOfTheNumbers() {
        model.thenFail(new LLMException("down", false));
        ReviewResult r = facade.generateMonthlyReview(OCT);
        assertTrue(r.metricsOnly());
        assertTrue(r.note().contains("not available"));
        String text = r.review().narrative();
        assertTrue(text.contains("What happened: in October 2026 you spent 390.00 across 3 purchases and received 3000.00"), text);
        assertTrue(text.contains("What to fix:"));
        assertEquals(r.review(), facade.getMonthlyReview(OCT).orElseThrow());
    }

    @Test
    void aMonthWithNoTransactionsOrInTheFutureIsRefused() {
        assertThrows(ValidationException.class, () -> facade.generateMonthlyReview(YearMonth.of(2026, 7)));
        assertThrows(ValidationException.class, () -> facade.generateMonthlyReview(YearMonth.of(2027, 1)));
        assertTrue(model.requests().isEmpty());
    }

    @Test
    void generatingAgainReplacesTheEarlierReview() {
        model.thenFail(new LLMException("down", false));
        facade.generateMonthlyReview(OCT);
        model.thenFail(new LLMException("down", false));
        ReviewResult again = facade.generateMonthlyReview(OCT);
        assertTrue(again.replacedEarlier());
    }

    @Test
    void aGoalThatIsBehindBecomesOneFixAndIsNotSuggestedTwice() {
        facade.addGoal(new GoalRequest("Laptop", Money.of("1200"), LocalDate.of(2026, 12, 1), Money.ZERO), TODAY);
        model.thenFail(new LLMException("down", false));
        ReviewResult first = facade.generateMonthlyReview(OCT);
        assertEquals(1, first.proposals().size());
        assertTrue(first.proposals().get(0).title().contains("Laptop"));
        assertEquals(first.proposals().get(0).id(), first.review().proposalIds().get(0));
        model.thenFail(new LLMException("down", false));
        ReviewResult second = facade.generateMonthlyReview(OCT);
        assertTrue(second.proposals().isEmpty());
        assertEquals(1, second.alreadyWaiting());
        assertEquals(1, facade.listPendingProposals(TODAY).size());
    }

    @Test
    void theNextReviewIsToldWhatTheLastOneSaid() {
        aiReview("What happened: you spent $390.00.\nWhat went well: ok.\nWhat to fix: nothing.");
        facade.generateMonthlyReview(OCT);
        model.thenReply(LLMResponse.toolCall(new ToolCall("tu_t", "transactions", ToolArgs.of("month", "2026-09"))));
        model.thenReply(LLMResponse.text("What happened: you spent $260.00.\nWhat went well: less spending.\nWhat to fix: nothing."));
        facade.generateMonthlyReview(YearMonth.of(2026, 9));
        String prompt = model.requests().get(2).systemPrompt();
        assertTrue(prompt.contains("Previous review summary: 2026-10: "), prompt);
    }

    @Test
    void anUnverifiableNarrativeFallsBackToTheNumbers() {
        model.thenReply(LLMResponse.text("What happened: you spent $999.00."));
        model.thenReply(LLMResponse.text("What happened: you spent $999.00."));
        ReviewResult r = facade.generateMonthlyReview(OCT);
        assertTrue(r.metricsOnly());
        assertTrue(r.review().narrative().contains("390.00"));
    }
}
