package wealthsei.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Everything one agent run did, in order, so the user can see how an answer was reached. */
public class AgentTrace {

    private final String id;
    private final TaskType taskType;
    private final String question;
    private final List<TraceStep> steps = new ArrayList<>();

    public AgentTrace(String id, TaskType taskType, String question) {
        this.id = Objects.requireNonNull(id, "id");
        this.taskType = Objects.requireNonNull(taskType, "taskType");
        this.question = Objects.requireNonNull(question, "question");
    }

    public String id() {
        return id;
    }

    public TaskType taskType() {
        return taskType;
    }

    public String question() {
        return question;
    }

    public List<TraceStep> steps() {
        return List.copyOf(steps);
    }

    /** Adds a step; the index is set here so steps are always numbered 1, 2, 3 ... */
    public void addStep(String kind, String toolName, String argsJson, String resultSummary) {
        steps.add(new TraceStep(steps.size() + 1, kind, toolName, argsJson, resultSummary));
    }

    public void addStep(TraceStep step) {
        addStep(step.kind(), step.toolName(), step.argsJson(), step.resultSummary());
    }
}
