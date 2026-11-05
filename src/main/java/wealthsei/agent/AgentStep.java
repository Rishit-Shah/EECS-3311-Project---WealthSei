package wealthsei.agent;

/** What the model wants next: run a tool ({@link ToolCallStep}), or give the final answer ({@link FinalAnswerStep}). */
public sealed interface AgentStep permits ToolCallStep, FinalAnswerStep {
}
