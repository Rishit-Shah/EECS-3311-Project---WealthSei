package wealthsei.agent;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Categories;
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

class ChatMemoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);

    private final MockLLMProvider model = new MockLLMProvider();
    private final WealthSeiFacade facade = WealthSeiFactory.inMemory(model, LlmConfig.none(), () -> TODAY);

    ChatMemoryTest() throws IOException {
        Path csv = Files.createTempFile("chat", ".csv");
        Files.writeString(csv, "Date,Description,Amount\n2026-10-05,Spotify,-10\n2026-10-12,Pizza Place,-300\n2026-10-14,Whole Foods,-80\n");
        csv.toFile().deleteOnExit();
        facade.importTransactions(csv, TODAY);
    }

    private static ToolCall lookup(String month, String category) {
        return new ToolCall("tu_1", "transactions", ToolArgs.of("month", month, "category", category));
    }

    @Test
    void aFollowUpSeesTheEarlierQuestionAndAnswer() {
        model.thenReply(LLMResponse.toolCall(lookup("2026-10", "dining")));
        model.thenReply(LLMResponse.text("You spent $300.00 on dining in 2026-10."));
        AgentResult first = facade.ask("how much did I spend on dining in 2026-10?");
        assertEquals(AgentStatus.COMPLETED, first.status());

        model.thenReply(LLMResponse.text("That is $300.00, as I said."));
        AgentResult second = facade.ask("and what was that again?");
        assertEquals(AgentStatus.COMPLETED, second.status());
        var request = model.requests().get(model.requests().size() - 1);
        assertEquals(3, request.messages().size());
        assertEquals(Message.Role.USER, request.messages().get(0).role());
        assertTrue(request.messages().get(0).text().contains("dining"));
        assertTrue(request.messages().get(1).text().contains("300"));
    }

    @Test
    void offTopicQuestionsGetNoToolCallsAndTheSystemPromptSaysSo() {
        model.thenReply(LLMResponse.text("I can only help with your finances. Try asking how much you spent on dining."));
        AgentResult r = facade.ask("what is the capital of France?");
        assertEquals(AgentStatus.COMPLETED, r.status());
        String prompt = model.requests().get(0).systemPrompt();
        assertTrue(prompt.contains("not about their money"));
        assertTrue(prompt.contains("Protected categories"));
    }

    @Test
    void forgettingStartsAFreshConversation() {
        model.thenReply(LLMResponse.text("Hello."));
        facade.ask("hi");
        facade.clearConversation();
        model.thenReply(LLMResponse.text("Hello again."));
        facade.ask("hi again");
        assertEquals(1, model.requests().get(1).messages().size());
    }

    @Test
    void blankQuestionsAreRefusedWithoutCallingTheModel() {
        assertThrows(ValidationException.class, () -> facade.ask("   "));
        assertTrue(model.requests().isEmpty());
    }

    @Test
    void whenTheModelIsDownTheAnswerPointsToCommandsAndNothingIsRemembered() {
        model.thenFail(new LLMException("down", false));
        AgentResult r = facade.ask("how am I doing?");
        assertEquals(AgentStatus.MODEL_UNAVAILABLE, r.status());
        assertTrue(r.answer().contains("transactions"));
        model.thenReply(LLMResponse.text("Fine."));
        facade.ask("hello");
        assertEquals(1, model.requests().get(model.requests().size() - 1).messages().size());
    }

    @Test
    void theConversationWindowDropsTheOldestMessages() {
        ConversationMemory memory = new ConversationMemory(4);
        for (int i = 1; i <= 4; i++) {
            memory.append(Message.user("q" + i));
            memory.append(Message.assistant("a" + i, List.of()));
        }
        List<Message> recent = memory.recent();
        assertEquals(4, recent.size());
        assertEquals("q3", recent.get(0).text());
        assertThrows(IllegalArgumentException.class, () -> new ConversationMemory(1));
    }

    @Test
    void theWindowAlwaysStartsWithAQuestion() {
        ConversationMemory memory = new ConversationMemory(3);
        memory.append(Message.user("q1"));
        memory.append(Message.assistant("a1", List.of()));
        memory.append(Message.user("q2"));
        memory.append(Message.assistant("a2", List.of()));
        assertEquals(Message.Role.USER, memory.recent().get(0).role());
        assertEquals("q2", memory.recent().get(0).text());
    }

    @Test
    void protectedCategoriesAreSavedAndLoadedAgain() throws IOException {
        Path dir = Files.createTempDirectory("mem");
        WealthSeiFacade one = WealthSeiFactory.persistent(dir);
        assertTrue(one.protectedCategories().contains(Categories.GROCERIES));
        one.setProtectedCategory(Categories.DINING, true);
        one.setProtectedCategory(Categories.GROCERIES, false);
        WealthSeiFacade two = WealthSeiFactory.persistent(dir);
        assertTrue(two.protectedCategories().contains(Categories.DINING));
        assertFalse(two.protectedCategories().contains(Categories.GROCERIES));
    }

    @Test
    void theInsightToolReportsHabitsAndAScoreNote() {
        InsightTool tool = new InsightTool(null, null, () -> TODAY);
        assertEquals("insights", tool.spec().name());
        model.thenReply(LLMResponse.toolCall(new ToolCall("tu_i", InsightTool.NAME, ToolArgs.of("month", "2026-10"))));
        model.thenReply(LLMResponse.text("Here are your habits."));
        AgentResult r = facade.ask("what are my spending habits in 2026-10?");
        assertEquals(AgentStatus.COMPLETED, r.status());
        assertTrue(facade.latestTrace().isPresent());
    }
}
