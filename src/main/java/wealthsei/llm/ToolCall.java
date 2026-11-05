package wealthsei.llm;

import java.util.Objects;

/**
 * The model asking to run a tool.
 *
 * @param id       the model's id for this call; the result must be sent back under the same id
 * @param toolName which tool
 * @param args     the arguments
 */
public record ToolCall(String id, String toolName, ToolArgs args) {

    public ToolCall {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(toolName, "toolName");
        Objects.requireNonNull(args, "args");
    }
}
