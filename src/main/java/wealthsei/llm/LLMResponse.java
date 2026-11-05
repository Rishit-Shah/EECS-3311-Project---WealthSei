package wealthsei.llm;

import java.util.List;
import java.util.Objects;

/**
 * The model's reply: some text, tool calls, or both.
 *
 * @param text       what it said (may be empty when it only asked for tools)
 * @param toolCalls  the tools it wants run
 * @param tokensUsed input plus output tokens, for cost tracking
 */
public record LLMResponse(String text, List<ToolCall> toolCalls, int tokensUsed) {

    public LLMResponse {
        Objects.requireNonNull(text, "text");
        toolCalls = List.copyOf(Objects.requireNonNull(toolCalls, "toolCalls"));
    }

    public static LLMResponse text(String text) {
        return new LLMResponse(text, List.of(), 0);
    }

    public static LLMResponse toolCall(ToolCall call) {
        return new LLMResponse("", List.of(call), 0);
    }
}
