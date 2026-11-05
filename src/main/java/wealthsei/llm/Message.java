package wealthsei.llm;

import java.util.List;
import java.util.Objects;

/**
 * One turn of the conversation with the model.
 *
 * @param role       who is speaking
 * @param text       the words (for a tool result, the result data)
 * @param toolCalls  tool calls the assistant made in this turn (assistant messages only)
 * @param toolCallId which call a tool result answers (tool-result messages only)
 */
public record Message(Role role, String text, List<ToolCall> toolCalls, String toolCallId) {

    public enum Role { USER, ASSISTANT, TOOL_RESULT }

    public Message {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(text, "text");
        toolCalls = List.copyOf(Objects.requireNonNull(toolCalls, "toolCalls"));
    }

    public static Message user(String text) {
        return new Message(Role.USER, text, List.of(), null);
    }

    public static Message assistant(String text, List<ToolCall> calls) {
        return new Message(Role.ASSISTANT, text, calls, null);
    }

    public static Message toolResult(String toolCallId, String data) {
        return new Message(Role.TOOL_RESULT, data, List.of(), Objects.requireNonNull(toolCallId, "toolCallId"));
    }
}
