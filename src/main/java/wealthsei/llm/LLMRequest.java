package wealthsei.llm;

import java.util.List;
import java.util.Objects;

/** Everything sent to the model for one reply. */
public record LLMRequest(String systemPrompt, List<Message> messages, List<ToolSpec> toolSpecs, String model) {

    public LLMRequest {
        Objects.requireNonNull(systemPrompt, "systemPrompt");
        messages = List.copyOf(Objects.requireNonNull(messages, "messages"));
        toolSpecs = List.copyOf(Objects.requireNonNull(toolSpecs, "toolSpecs"));
        Objects.requireNonNull(model, "model");
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("A request needs at least one message");
        }
    }

    /** A plain question with no tools. */
    public static LLMRequest simple(String systemPrompt, String userText, String model) {
        return new LLMRequest(systemPrompt, List.of(Message.user(userText)), List.of(), model);
    }
}
