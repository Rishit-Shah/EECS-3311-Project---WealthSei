package wealthsei.agent;

import java.util.Objects;
import wealthsei.llm.ToolCall;

/** The model asked for a tool. */
public record ToolCallStep(ToolCall call) implements AgentStep {

    public ToolCallStep {
        Objects.requireNonNull(call, "call");
    }
}
