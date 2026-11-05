package wealthsei.agent;

/** How an agent run ended. */
public enum AgentStatus {
    /** Finished normally and every figure is backed by a tool result. */
    COMPLETED,
    /** Finished, but at least one tool failed, so the answer is qualified. */
    TOOL_FAILURE,
    /** Ran out of steps; the answer is partial. */
    STEP_LIMIT,
    /** The model could not be used; the answer comes from fixed rules and templates. */
    MODEL_UNAVAILABLE,
    /** The answer still contained figures no tool produced. */
    UNGROUNDED
}
