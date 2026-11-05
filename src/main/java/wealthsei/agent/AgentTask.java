package wealthsei.agent;

import java.util.Objects;

/**
 * A job for the agent.
 *
 * @param type      what kind of job
 * @param userInput the user's own words
 * @param params    structured inputs (item, price, date ...)
 */
public record AgentTask(TaskType type, String userInput, TaskParams params) {

    public AgentTask {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(userInput, "userInput");
        Objects.requireNonNull(params, "params");
        if (userInput.isBlank()) {
            throw new IllegalArgumentException("The task needs some input");
        }
    }
}
