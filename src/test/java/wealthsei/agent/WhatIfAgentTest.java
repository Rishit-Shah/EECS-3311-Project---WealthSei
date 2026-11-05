package wealthsei.agent;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import wealthsei.analytics.ScenarioResult;
import wealthsei.facade.WealthSeiFacade;
import wealthsei.facade.WealthSeiFactory;
import wealthsei.llm.LLMException;
import wealthsei.llm.LLMResponse;
import wealthsei.llm.LlmConfig;
import wealthsei.llm.Message;
import wealthsei.llm.MockLLMProvider;
import wealthsei.llm.ToolArgs;
import wealthsei.llm.ToolCall;
import wealthsei.presentation.cli.CliApp;
import wealthsei.service.ValidationException;

class WhatIfAgentTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);

    private final MockLLMProvider model = new MockLLMProvider();
    private final WealthSeiFacade facade = WealthSeiFactory.inMemory(model, LlmConfig.none(), () -> TODAY);

    WhatIfAgentTest() throws IOException {
        CliApp app = new CliApp(facade, new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8), () -> TODAY);
        Path csv = Files.createTempFile("whatif", ".csv");
        StringBuilder rows = new StringBuilder("Date,Description,Amount\n");
        for (String month : new String[] {"2026-08", "2026-09", "2026-10"}) {
            rows.append(month).append("-05,Spotify,-10\n").append(month).append("-12,Pizza Place,-300\n");
        }
        Files.writeString(csv, rows.toString());
        csv.toFile().deleteOnExit();
        facade.importTransactions(csv, TODAY);
        app.execute("plan balance 3000");
        app.execute("plan income Job 3000 monthly 2026-11-15");
    }

    private static ToolCall scenario(String... args) {
        return new ToolCall("tu_s", ScenarioTool.NAME, ToolArgs.of(args));
    }

    @Test
    void theToolReturnsBaselineAndScenarioFigures() {
        model.thenReply(LLMResponse.toolCall(scenario("cancel", "Spotify", "caps", "dining=150")));
        model.thenReply(LLMResponse.text("Cancelling Spotify and capping dining would add $160.00 a month."));
        AgentResult r = facade.runWhatIf("What if I cancel Spotify and cap dining at 150?");
        assertEquals(AgentStatus.COMPLETED, r.status());
        assertTrue(r.grounding().grounded());
        assertNull(r.verdict());
        assertTrue(model.requests().get(0).systemPrompt().contains("what-if"));
        Message toolResult = model.requests().get(1).messages().get(model.requests().get(1).messages().size() - 1);
        assertTrue(toolResult.text().contains("\"monthlySavingsChange\":160.00"), toolResult.text());
        assertTrue(toolResult.text().contains("Cancel Spotify"));
    }

    @Test
    void anUnknownSubscriptionComesBackAsNotFound() {
        model.thenReply(LLMResponse.toolCall(scenario("cancel", "Hulu")));
        model.thenReply(LLMResponse.text("I could not find Hulu. Your recurring payments are listed above."));
        AgentResult r = facade.runWhatIf("cancel Hulu");
        assertEquals(AgentStatus.TOOL_FAILURE, r.status());
        Message toolResult = model.requests().get(1).messages().get(model.requests().get(1).messages().size() - 1);
        assertTrue(toolResult.text().contains("No recurring payment or bill called 'Hulu'"));
        assertTrue(toolResult.text().contains("Spotify"));
    }

    @Test
    void tooManyChangesAndBadValuesAreRejectedBeforeSimulating() {
        model.thenReply(LLMResponse.toolCall(scenario("cancel", "a,b,c", "caps", "dining=1,shopping=1,health=1")));
        model.thenReply(LLMResponse.toolCall(scenario("income_increase", "-50")));
        model.thenReply(LLMResponse.text("Please split that into smaller scenarios."));
        AgentResult r = facade.runWhatIf("do everything");
        assertEquals(AgentStatus.TOOL_FAILURE, r.status());
        AgentTrace trace = facade.getTrace(r.traceId()).orElseThrow();
        assertEquals(2, trace.steps().stream().filter(s -> s.kind().equals(TraceStep.TOOL_CALL)
                && s.resultSummary().startsWith("ERROR")).count());
    }

    @Test
    void aVagueRequestGetsAClarifyingQuestionWithoutAnyTool() {
        model.thenReply(LLMResponse.text("Which expenses would you like to cut, and by how much?"));
        AgentResult r = facade.runWhatIf("cut my expenses");
        assertEquals(AgentStatus.COMPLETED, r.status());
        assertTrue(r.answer().endsWith("?"));
        assertEquals(1, model.requests().size());
        assertTrue(facade.getTrace(r.traceId()).orElseThrow().steps().stream().noneMatch(s -> s.kind().equals(TraceStep.TOOL_CALL)));
    }

    @Test
    void aFigureTheToolDidNotProduceIsCaught() {
        model.thenReply(LLMResponse.toolCall(scenario("income_increase", "200")));
        model.thenReply(LLMResponse.text("You would gain $777.77 a month."));
        model.thenReply(LLMResponse.text("You would gain $200.00 a month."));
        AgentResult r = facade.runWhatIf("what if I get a 200 raise");
        assertEquals(AgentStatus.COMPLETED, r.status());
        assertTrue(r.answer().contains("200.00"));
    }

    @Test
    void withoutTheModelTheUserIsToldHowToGiveChangesDirectly() {
        model.thenFail(new LLMException("AI features are off", false));
        AgentResult r = facade.runWhatIf("cancel Spotify");
        assertEquals(AgentStatus.MODEL_UNAVAILABLE, r.status());
        assertTrue(r.answer().contains("whatif cancel=Spotify"));
    }

    @Test
    void aBlankDescriptionNeverReachesTheAgent() {
        assertThrows(ValidationException.class, () -> facade.runWhatIf("  "));
        assertTrue(model.requests().isEmpty());
    }

    @Test
    void theStructuredPathNeedsNoModel() {
        ScenarioResult r = facade.simulateWhatIf(Map.of("cancel", "Spotify", "caps", "dining=150"), TODAY);
        assertEquals(wealthsei.domain.Money.of("160.00"), r.monthlySavingsDelta());
        assertTrue(model.requests().isEmpty());
        assertThrows(ValidationException.class, () -> facade.simulateWhatIf(Map.of(), TODAY));
    }

    @Test
    void anAiWhatIfCarriesItsChangesAsDraftsThatChangeNothingByThemselves() {
        facade.setBudgetLimit(java.time.YearMonth.of(2026, 11), wealthsei.domain.Categories.DINING, wealthsei.domain.Money.of("400"));
        model.thenReply(LLMResponse.toolCall(scenario("cancel", "Spotify", "caps", "dining=150")));
        model.thenReply(LLMResponse.text("Cancelling Spotify and capping dining would add $160.00 a month."));
        AgentResult r = facade.runWhatIf("What if I cancel Spotify and cap dining at 150?");
        assertEquals(2, r.drafts().size());
        assertTrue(facade.listPendingProposals(TODAY).isEmpty());
        var created = facade.createProposals(r.drafts(), r.traceId());
        assertEquals(2, created.size());
        assertEquals(r.traceId(), created.get(0).traceId());
        assertEquals(2, facade.listPendingProposals(TODAY).size());
    }

    @Test
    void proposalsFromAnEarlierMonthExpireAndCannotBeApproved() {
        facade.setBudgetLimit(java.time.YearMonth.of(2026, 11), wealthsei.domain.Categories.DINING, wealthsei.domain.Money.of("400"));
        var drafts = facade.whatIfDrafts(java.util.Map.of("caps", "dining=150"), TODAY);
        var created = facade.createProposals(drafts, "");
        assertEquals(1, facade.listPendingProposals(TODAY).size());
        assertTrue(facade.listPendingProposals(LocalDate.of(2027, 1, 5)).isEmpty());
        assertEquals("Expired", facade.getProposal(created.get(0).id()).statusName());
        assertThrows(wealthsei.command.IllegalStateTransitionException.class,
                () -> facade.decideProposal(created.get(0).id(), true));
    }

    @Test
    void aDraftWithNoMachineReadableChangeIsLeftOutWithAReason() {
        var created = facade.createProposals(java.util.List.of(new ProposalDraft("Spend less", "Just a thought")), "");
        assertTrue(created.isEmpty());
        assertEquals(1, facade.lastDiscardedDrafts().size());
        assertTrue(facade.lastDiscardedDrafts().get(0).contains("only a suggestion"));
    }

    @Test
    void aPaymentThatRepeatsEveryMonthIsCountedAsRecurringNotAsEverydaySpending() {
        // Spotify (10) and Pizza Place (300) both repeat monthly, so they are recurring payments
        var r = facade.simulateWhatIf(java.util.Map.of("caps", "dining=150"), TODAY);
        assertEquals(wealthsei.domain.Money.ZERO, r.typicalMonthlySpending());
        assertEquals(wealthsei.domain.Money.of("2690.00"), r.baselineMonthlySavings());
        assertEquals(wealthsei.domain.Money.of("2840.00"), r.scenarioMonthlySavings());
    }

    @Test
    void irregularSpendingFromPastFullMonthsIsCountedAsAMonthlyAverage() throws IOException {
        WealthSeiFacade fresh = WealthSeiFactory.inMemory(model, LlmConfig.none(), () -> TODAY);
        Path csv = Files.createTempFile("irregular", ".csv");
        Files.writeString(csv, "Date,Description,Amount\n2026-08-03,Cafe Aaa,-100\n2026-09-14,Bistro Bbb,-200\n"
                + "2026-10-21,Diner Ccc,-300\n2026-11-02,Snack Ddd,-50\n");
        csv.toFile().deleteOnExit();
        fresh.importTransactions(csv, TODAY);
        CliApp app = new CliApp(fresh, new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8), () -> TODAY);
        app.execute("plan balance 3000");
        app.execute("plan income Job 3000 monthly 2026-11-15");
        var r = fresh.simulateWhatIf(java.util.Map.of("income_increase", "100"), TODAY);
        assertEquals(wealthsei.domain.Money.of("200.00"), r.typicalMonthlySpending());
        assertEquals(wealthsei.domain.Money.of("2800.00"), r.baselineMonthlySavings());
        assertEquals(wealthsei.domain.Money.of("2900.00"), r.scenarioMonthlySavings());
    }

    @Test
    void withoutAFullMonthOfHistoryNoEverydaySpendingIsInvented() throws IOException {
        WealthSeiFacade fresh = WealthSeiFactory.inMemory(model, LlmConfig.none(), () -> TODAY);
        Path csv = Files.createTempFile("thismonth", ".csv");
        Files.writeString(csv, "Date,Description,Amount\n2026-11-02,Pizza Place,-300\n");
        csv.toFile().deleteOnExit();
        fresh.importTransactions(csv, TODAY);
        CliApp app = new CliApp(fresh, new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8), () -> TODAY);
        app.execute("plan balance 3000");
        app.execute("plan income Job 3000 monthly 2026-11-15");
        var r = fresh.simulateWhatIf(java.util.Map.of("income_increase", "100"), TODAY);
        assertEquals(wealthsei.domain.Money.ZERO, r.typicalMonthlySpending());
        assertEquals(wealthsei.domain.Money.of("3000.00"), r.baselineMonthlySavings());
    }
}
