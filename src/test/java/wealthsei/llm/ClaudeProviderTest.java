package wealthsei.llm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClaudeProviderTest {

    private static final ToolSpec SPEC = new ToolSpec("budget_status", "Budget for a month",
            List.of(new ParamSpec("month", ParamType.STRING, "YYYY-MM", false)));

    /** A transport that records what it was sent and returns a canned reply. */
    private static final class FakeTransport implements HttpTransport {
        final List<String> sent = new ArrayList<>();
        final String reply;

        FakeTransport(String reply) {
            this.reply = reply;
        }

        @Override
        public String post(String jsonBody) {
            sent.add(jsonBody);
            return reply;
        }
    }

    @Test
    void sendsModelSystemMessagesAndTools() {
        FakeTransport t = new FakeTransport("{\"content\":[{\"type\":\"text\",\"text\":\"Hi\"}],\"usage\":{\"input_tokens\":10,\"output_tokens\":4}}");
        LLMResponse r = new ClaudeProvider(t).complete(new LLMRequest("Be brief.",
                List.of(Message.user("Hello")), List.of(SPEC), "claude-sonnet-5-5"));
        assertEquals("Hi", r.text());
        assertEquals(14, r.tokensUsed());
        Map<String, Object> body = Json.parseObject(t.sent.get(0));
        assertEquals("claude-sonnet-5-5", body.get("model"));
        assertEquals("Be brief.", body.get("system"));
        assertNotNull(body.get("max_tokens"));
        Map<?, ?> tool = (Map<?, ?>) ((List<?>) body.get("tools")).get(0);
        assertEquals("budget_status", tool.get("name"));
        assertEquals("object", ((Map<?, ?>) tool.get("input_schema")).get("type"));
        assertEquals(Boolean.TRUE, ((Map<?, ?>) body.get("tool_choice")).get("disable_parallel_tool_use"));
    }

    @Test
    void noToolsMeansNoToolFields() {
        FakeTransport t = new FakeTransport("{\"content\":[{\"type\":\"text\",\"text\":\"ok\"}]}");
        new ClaudeProvider(t).complete(LLMRequest.simple("", "Hello", "m"));
        Map<String, Object> body = Json.parseObject(t.sent.get(0));
        assertFalse(body.containsKey("tools"));
        assertFalse(body.containsKey("tool_choice"));
        assertFalse(body.containsKey("system"));
    }

    @Test
    void toolUseIsReadFromTheReply() {
        FakeTransport t = new FakeTransport("{\"content\":[{\"type\":\"text\",\"text\":\"Checking\"},"
                + "{\"type\":\"tool_use\",\"id\":\"tu_1\",\"name\":\"budget_status\",\"input\":{\"month\":\"2026-11\"}}]}");
        LLMResponse r = new ClaudeProvider(t).complete(LLMRequest.simple("", "q", "m"));
        assertEquals("Checking", r.text());
        assertEquals(1, r.toolCalls().size());
        ToolCall call = r.toolCalls().get(0);
        assertEquals("tu_1", call.id());
        assertEquals("budget_status", call.toolName());
        assertEquals("2026-11", call.args().requireText("month"));
        assertEquals(0, r.tokensUsed());
    }

    @Test
    void toolRoundTripKeepsIdsAndGroupsResults() {
        ToolCall call = new ToolCall("tu_1", "budget_status", ToolArgs.of("month", "2026-11"));
        LLMRequest request = new LLMRequest("s", List.of(Message.user("Can I afford it?"),
                Message.assistant("Let me look.", List.of(call)), Message.toolResult("tu_1", "{\"ok\":true}")),
                List.of(SPEC), "m");
        Map<String, Object> body = ClaudeProvider.toBody(request);
        List<?> messages = (List<?>) body.get("messages");
        assertEquals(3, messages.size());
        Map<?, ?> assistant = (Map<?, ?>) messages.get(1);
        List<?> blocks = (List<?>) assistant.get("content");
        assertEquals("text", ((Map<?, ?>) blocks.get(0)).get("type"));
        Map<?, ?> use = (Map<?, ?>) blocks.get(1);
        assertEquals("tool_use", use.get("type"));
        assertEquals("tu_1", use.get("id"));
        assertEquals(Map.of("month", "2026-11"), use.get("input"));
        Map<?, ?> results = (Map<?, ?>) messages.get(2);
        assertEquals("user", results.get("role"));
        Map<?, ?> result = (Map<?, ?>) ((List<?>) results.get("content")).get(0);
        assertEquals("tool_result", result.get("type"));
        assertEquals("tu_1", result.get("tool_use_id"));
        assertEquals("{\"ok\":true}", result.get("content"));
    }

    @Test
    void serviceErrorsBecomeExceptions() {
        LLMException e = assertThrows(LLMException.class, () -> new ClaudeProvider(
                new FakeTransport("{\"type\":\"error\",\"error\":{\"type\":\"x\",\"message\":\"credit balance is too low\"}}"))
                .complete(LLMRequest.simple("", "q", "m")));
        assertTrue(e.getMessage().contains("credit balance is too low"));
        assertFalse(e.isRetryable());
    }

    @Test
    void unreadableOrEmptyRepliesBecomeExceptions() {
        assertThrows(LLMException.class, () -> new ClaudeProvider(new FakeTransport("not json")).complete(LLMRequest.simple("", "q", "m")));
        assertThrows(LLMException.class, () -> new ClaudeProvider(new FakeTransport("{}")).complete(LLMRequest.simple("", "q", "m")));
    }

    @Test
    void requestNeedsAMessage() {
        assertThrows(IllegalArgumentException.class, () -> new LLMRequest("s", List.of(), List.of(), "m"));
    }
}
