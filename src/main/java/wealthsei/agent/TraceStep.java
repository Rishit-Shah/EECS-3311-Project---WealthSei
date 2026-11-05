package wealthsei.agent;

import java.util.Objects;

/**
 * One line of the "how did you get this?" record.
 *
 * @param index         position, from 1
 * @param kind          TOOL_CALL, ANSWER, GROUNDING, GUARDRAIL or ERROR
 * @param toolName      the tool, for a tool call; otherwise empty
 * @param argsJson      the arguments, for a tool call; otherwise empty
 * @param resultSummary what came back, or what happened
 */
public record TraceStep(int index, String kind, String toolName, String argsJson, String resultSummary) {

    public static final String TOOL_CALL = "TOOL_CALL";
    public static final String ANSWER = "ANSWER";
    public static final String GROUNDING = "GROUNDING";
    public static final String GUARDRAIL = "GUARDRAIL";
    public static final String ERROR = "ERROR";

    public TraceStep {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(toolName, "toolName");
        Objects.requireNonNull(argsJson, "argsJson");
        Objects.requireNonNull(resultSummary, "resultSummary");
    }
}
