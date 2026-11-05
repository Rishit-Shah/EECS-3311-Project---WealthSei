package wealthsei.agent;

import java.util.Map;
import java.util.Objects;
import wealthsei.llm.Json;

/**
 * What a tool returned: data as JSON text when it worked, or an error message when it did not.
 */
public record ToolResult(boolean success, String dataJson, String error) {

    public ToolResult {
        Objects.requireNonNull(dataJson, "dataJson");
        Objects.requireNonNull(error, "error");
    }

    public static ToolResult ok(Map<String, Object> data) {
        return new ToolResult(true, Json.write(data), "");
    }

    public static ToolResult failure(String message) {
        return new ToolResult(false, "", message);
    }

    /** What is sent back to the model: the data, or the error as JSON so it can explain what was missing. */
    public String forModel() {
        return success ? dataJson : Json.write(Map.of("error", error));
    }

    /** A short form for the trace. */
    public String summary() {
        String text = success ? dataJson : "ERROR: " + error;
        return text.length() <= 300 ? text : text.substring(0, 297) + "...";
    }
}
