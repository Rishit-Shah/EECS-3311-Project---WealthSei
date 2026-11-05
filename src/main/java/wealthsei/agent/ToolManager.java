package wealthsei.agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import wealthsei.llm.ToolCall;
import wealthsei.llm.ToolSpec;

/** Holds the tools, tells the model about them and runs the ones it picks, checking arguments first. */
public class ToolManager {

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public void register(Tool tool) {
        Objects.requireNonNull(tool, "tool");
        String name = tool.spec().name();
        if (tools.putIfAbsent(name, tool) != null) {
            throw new IllegalArgumentException("A tool named '" + name + "' is already registered");
        }
    }

    public List<ToolSpec> listSpecs() {
        List<ToolSpec> specs = new ArrayList<>();
        tools.values().forEach(t -> specs.add(t.spec()));
        return specs;
    }

    public boolean has(String name) {
        return tools.containsKey(name);
    }

    /** Never throws: an unknown tool, bad arguments or a failing tool all come back as a failed result. */
    public ToolResult execute(ToolCall call) {
        Tool tool = tools.get(call.toolName());
        if (tool == null) {
            return ToolResult.failure("There is no tool called '" + call.toolName() + "'");
        }
        try {
            tool.spec().validate(call.args());
            return tool.run(call.args());
        } catch (IllegalArgumentException | wealthsei.service.ValidationException e) {
            return ToolResult.failure(e.getMessage());
        } catch (RuntimeException e) {
            return ToolResult.failure("The tool '" + call.toolName() + "' failed: " + e.getMessage());
        }
    }
}
