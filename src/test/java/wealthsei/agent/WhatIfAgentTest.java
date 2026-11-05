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
}
