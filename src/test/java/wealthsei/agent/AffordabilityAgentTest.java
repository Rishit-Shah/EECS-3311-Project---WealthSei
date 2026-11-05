package wealthsei.agent;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Money;
import wealthsei.domain.Verdict;
import wealthsei.facade.WealthSeiFacade;
import wealthsei.facade.WealthSeiFactory;
import wealthsei.llm.LLMException;
import wealthsei.llm.LLMResponse;
import wealthsei.llm.LlmConfig;
import wealthsei.llm.Message;
import wealthsei.llm.MockLLMProvider;
import wealthsei.llm.ToolArgs;
import wealthsei.llm.ToolCall;
import wealthsei.service.ValidationException;

/** The F08 agent end to end, with a scripted model standing in for Claude. */
class AffordabilityAgentTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);

    private final MockLLMProvider model = new MockLLMProvider();
    private final WealthSeiFacade facade = WealthSeiFactory.inMemory(model, LlmConfig.none(), () -> TODAY);

    /** Tiny helper so tests can type CLI commands to set up a plan. */
    private final class Setup {
        void run(String... lines) {
            wealthsei.presentation.cli.CliApp app = new wealthsei.presentation.cli.CliApp(facade,
                    new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8), () -> TODAY);
            for (String line : lines) {
                app.execute(line);
            }
        }
    }

    private void plan(String balance) {
        new Setup().run("plan balance " + balance, "plan income Job 3000 monthly 2026-11-15",
                "plan bill Rent 1200 monthly 2026-12-01");
    }

    private static ToolCall call(String name, String... args) {
        return new ToolCall("tu_" + name, name, ToolArgs.of(args));
    }

    private AgentResult ask(String item, String price) {
        return facade.canIAfford(item, Money.of(price), null);
    }

    @Test
    void groundedAnswerCompletesAndKeepsTheModelsVerdict() {
        plan("3000");
        model.thenReply(LLMResponse.text("VERDICT: Yes\nThe $600 laptop leaves $2400.00 at its lowest, above your $200 buffer."));
        AgentResult r = ask("laptop", "600");
        assertEquals(AgentStatus.COMPLETED, r.status());
        assertEquals(Verdict.YES, r.verdict());
        assertFalse(r.answer().contains("VERDICT"));
        assertTrue(r.grounding().grounded());
        assertEquals(1, model.requests().size());
    }

    @Test
    void theFirstToolResultIsAlreadyInTheModelsContext() {
        plan("3000");
        model.thenReply(LLMResponse.text("VERDICT: Yes\nFine."));
        ask("laptop", "600");
        List<Message> messages = model.requests().get(0).messages();
        assertEquals(Message.Role.USER, messages.get(0).role());
        assertEquals(Message.Role.ASSISTANT, messages.get(1).role());
        assertEquals("forecast", messages.get(1).toolCalls().get(0).toolName());
        assertEquals(Message.Role.TOOL_RESULT, messages.get(2).role());
        assertTrue(messages.get(2).text().contains("suggestedVerdict"));
        assertEquals(6, model.requests().get(0).toolSpecs().size());
        assertTrue(model.requests().get(0).systemPrompt().contains("VERDICT"));
    }

    @Test
    void theModelCanCallMoreToolsAndSeeTheirResults() {
        plan("3000");
        new Setup().run("budget set 2026-11 shopping=300");
        model.thenReply(LLMResponse.toolCall(call("budget_status", "month", "2026-11")));
        model.thenReply(LLMResponse.text("VERDICT: Yes\nShopping has a $300.00 limit with $0.00 spent."));
        AgentResult r = ask("laptop", "600");
        assertEquals(AgentStatus.COMPLETED, r.status());
        assertEquals(2, model.requests().size());
        List<Message> second = model.requests().get(1).messages();
        Message toolResult = second.get(second.size() - 1);
        assertEquals(Message.Role.TOOL_RESULT, toolResult.role());
        assertTrue(toolResult.text().contains("\"limit\":300.00"));
        AgentTrace trace = facade.getTrace(r.traceId()).orElseThrow();
        assertEquals(List.of("forecast", "budget_status"), trace.steps().stream()
                .filter(s -> s.kind().equals(TraceStep.TOOL_CALL)).map(TraceStep::toolName).toList());
    }

    @Test
    void aMonthWithoutABudgetIsANormalResultNotAFailure() {
        plan("3000");
        model.thenReply(LLMResponse.toolCall(call("budget_status", "month", "2026-11")));
        model.thenReply(LLMResponse.text("VERDICT: Yes\nNo budget is set for November, so I only checked your balance."));
        AgentResult r = ask("laptop", "600");
        assertEquals(AgentStatus.COMPLETED, r.status());
        List<Message> second = model.requests().get(1).messages();
        String result = second.get(second.size() - 1).text();
        assertTrue(result.contains("No budget has been set"));
        assertTrue(result.contains("\"budgetSet\":false"));
    }

    @Test
    void aFailingToolQualifiesTheAnswer() {
        plan("3000");
        model.thenReply(LLMResponse.toolCall(call("budget_status", "month", "soon")));
        model.thenReply(LLMResponse.text("VERDICT: Yes\nI could not see a budget, so I only checked your balance."));
        AgentResult r = ask("laptop", "600");
        assertEquals(AgentStatus.TOOL_FAILURE, r.status());
        List<Message> second = model.requests().get(1).messages();
        assertTrue(second.get(second.size() - 1).text().contains("must look like"));
    }

    @Test
    void unknownToolsAndBadArgumentsAreReportedToTheModel() {
        plan("3000");
        model.thenReply(LLMResponse.toolCall(call("hack_bank")));
        model.thenReply(LLMResponse.toolCall(call("forecast", "days", "many")));
        model.thenReply(LLMResponse.text("VERDICT: Yes\nOk."));
        AgentResult r = ask("laptop", "600");
        assertEquals(AgentStatus.TOOL_FAILURE, r.status());
        assertTrue(model.requests().get(1).messages().stream().anyMatch(m -> m.text().contains("no tool called")));
    }

    @Test
    void anInventedFigureTriggersOneCorrection() {
        plan("3000");
        model.thenReply(LLMResponse.text("VERDICT: Yes\nYou would have $9999.99 left."));
        model.thenReply(LLMResponse.text("VERDICT: Yes\nYou would have $2400.00 left."));
        AgentResult r = ask("laptop", "600");
        assertEquals(AgentStatus.COMPLETED, r.status());
        assertTrue(r.answer().contains("2400.00"));
        assertEquals(2, model.requests().size());
        List<Message> second = model.requests().get(1).messages();
        assertTrue(second.get(second.size() - 1).text().contains("$9999.99"));
    }

    @Test
    void figuresThatStayUngroundedAreFlagged() {
        plan("3000");
        model.thenReply(LLMResponse.text("VERDICT: Yes\nYou would have $9999.99 left."));
        model.thenReply(LLMResponse.text("VERDICT: Yes\nYou would still have $9999.99 left."));
        AgentResult r = ask("laptop", "600");
        assertEquals(AgentStatus.UNGROUNDED, r.status());
        assertFalse(r.grounding().grounded());
        assertTrue(r.answer().contains("could not be verified"));
    }

    @Test
    void theModelCannotBeMoreOptimisticThanTheRules() {
        plan("300");
        model.thenReply(LLMResponse.text("VERDICT: Yes\nGo ahead, it is fine."));
        AgentResult r = ask("laptop", "600");
        assertEquals(Verdict.NO, r.verdict());
        assertTrue(r.answer().contains("budget rules"));
        AgentTrace trace = facade.getTrace(r.traceId()).orElseThrow();
        assertTrue(trace.steps().stream().anyMatch(s -> s.kind().equals(TraceStep.GUARDRAIL)));
    }

    @Test
    void aMoreCautiousModelVerdictIsKept() {
        plan("3000");
        model.thenReply(LLMResponse.text("VERDICT: Wait\nIt is affordable but you may prefer to wait for a sale."));
        assertEquals(Verdict.WAIT, ask("laptop", "600").verdict());
    }

    @Test
    void aMissingVerdictLineFallsBackToTheRules() {
        plan("700");
        model.thenReply(LLMResponse.text("It is tight."));
        AgentResult r = ask("laptop", "600");
        assertEquals(Verdict.WAIT, r.verdict());
        assertTrue(r.answer().contains("no verdict"));
    }

    @Test
    void withoutTheModelTheAnswerComesFromTheRules() {
        plan("3000");
        model.thenFail(new LLMException("AI features are off", false));
        AgentResult r = ask("laptop", "600");
        assertEquals(AgentStatus.MODEL_UNAVAILABLE, r.status());
        assertEquals(Verdict.YES, r.verdict());
        assertTrue(r.answer().contains("Buying laptop for $600.00"));
        assertTrue(r.answer().contains("AI features are off"));
        assertTrue(r.answer().startsWith("Your balance stays above"));
    }

    @Test
    void negativeFiguresReadAsMinusDollars() {
        plan("300");
        model.thenFail(new LLMException("off", false));
        AgentResult r = ask("laptop", "600");
        assertTrue(r.answer().contains("to -$300.00"));
        assertFalse(r.answer().contains("$-"));
    }

    @Test
    void anEmptyReplyIsRepairedOnceThenGivenUpOn() {
        plan("3000");
        model.thenReply(LLMResponse.text("  ")).thenReply(LLMResponse.text(""));
        AgentResult r = ask("laptop", "600");
        assertEquals(AgentStatus.MODEL_UNAVAILABLE, r.status());
        assertEquals(2, model.requests().size());
        assertTrue(model.requests().get(1).messages().get(model.requests().get(1).messages().size() - 1).text().contains("empty or unusable"));
    }

    @Test
    void anEmptyReplyThatIsRepairedWorks() {
        plan("3000");
        model.thenReply(LLMResponse.text("")).thenReply(LLMResponse.text("VERDICT: Yes\nFine."));
        assertEquals(AgentStatus.COMPLETED, ask("laptop", "600").status());
    }

    @Test
    void runningOutOfStepsGivesAPartialAnswer() {
        plan("3000");
        for (int i = 0; i < 6; i++) {
            model.thenReply(LLMResponse.toolCall(new ToolCall("tu_" + i, "goals", ToolArgs.of())));
        }
        AgentResult r = ask("laptop", "600");
        assertEquals(AgentStatus.STEP_LIMIT, r.status());
        assertTrue(r.answer().contains("partial"));
        assertEquals(Verdict.YES, r.verdict());
    }

    @Test
    void withoutAPlanTheAgentSaysSoAndNeverCallsTheModel() {
        AgentResult r = ask("laptop", "600");
        assertEquals(AgentStatus.TOOL_FAILURE, r.status());
        assertTrue(r.answer().contains("plan balance"));
        assertTrue(model.requests().isEmpty());
        assertNull(r.verdict());
    }

    @Test
    void badInputNeverReachesTheAgent() {
        assertThrows(ValidationException.class, () -> facade.canIAfford(" ", Money.of("5"), null));
        assertThrows(ValidationException.class, () -> facade.canIAfford("tv", Money.ZERO, null));
        assertTrue(model.requests().isEmpty());
    }

    @Test
    void aPastDateIsRefused() {
        plan("3000");
        AgentResult r = facade.canIAfford("tv", Money.of("500"), TODAY.minusDays(1));
        assertEquals(AgentStatus.TOOL_FAILURE, r.status());
        assertTrue(r.answer().contains("past"));
    }

    @Test
    void aFuturePurchaseDateIsUsed() {
        plan("3000");
        model.thenReply(LLMResponse.text("VERDICT: Yes\nFine."));
        AgentResult r = facade.canIAfford("tv", Money.of("500"), LocalDate.of(2026, 12, 10));
        assertEquals(Verdict.YES, r.verdict());
        AgentTrace trace = facade.getTrace(r.traceId()).orElseThrow();
        assertTrue(trace.steps().get(0).resultSummary().contains("2026-12-10"));
    }

    @Test
    void theTraceCanBeFoundAgain() {
        plan("3000");
        model.thenReply(LLMResponse.text("VERDICT: Yes\nFine."));
        AgentResult r = ask("laptop", "600");
        assertEquals(r.traceId(), facade.latestTrace().orElseThrow().id());
        assertEquals(TaskType.AFFORDABILITY, facade.getTrace(r.traceId()).orElseThrow().taskType());
        assertTrue(facade.getTrace("nope").isEmpty());
    }

    @Test
    void tokensAreCounted() {
        plan("3000");
        model.thenReply(new LLMResponse("VERDICT: Yes\nFine.", List.of(), 321));
        assertEquals(321, ask("laptop", "600").tokensUsed());
    }

    @Test
    void transactionAndGoalToolsReturnRealFigures() throws IOException {
        plan("3000");
        Path csv = Files.createTempFile("agent", ".csv");
        Files.writeString(csv, "Date,Description,Amount\n2026-10-03,Pizza Place,-40\n2026-10-10,Pizza Place,-25.50\n");
        csv.toFile().deleteOnExit();
        facade.importTransactions(csv, TODAY);
        new Setup().run("goal add \"Trip\" 1200 2027-11-04");
        model.thenReply(LLMResponse.toolCall(call("transactions", "month", "2026-10", "merchant", "pizza")));
        model.thenReply(LLMResponse.toolCall(call("goals")));
        model.thenReply(LLMResponse.text("VERDICT: Yes\nYou spent $65.50 at Pizza Place; the Trip goal needs $100.00 a month."));
        AgentResult r = ask("laptop", "600");
        assertEquals(AgentStatus.COMPLETED, r.status());
        assertTrue(r.grounding().grounded());
    }
}
