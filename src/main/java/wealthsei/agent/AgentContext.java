package wealthsei.agent;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import wealthsei.llm.Message;
import wealthsei.llm.ToolCall;
import wealthsei.llm.ToolSpec;

/** Everything the planner needs for one run: the task, the conversation so far and what the tools have returned. */
public class AgentContext {

    /** A tool call and what it returned. */
    public record Observation(ToolCall call, ToolResult result) { }

    private final AgentTask task;
    private final LocalDate today;
    private final List<Message> history = new ArrayList<>();
    private final List<Observation> observations = new ArrayList<>();
    private List<ToolSpec> toolSpecs = List.of();
    private final List<Message> prior = new ArrayList<>();
    private String profileNote = "";
    private int tokensUsed;

    public AgentContext(AgentTask task, LocalDate today) {
        this.task = Objects.requireNonNull(task, "task");
        this.today = Objects.requireNonNull(today, "today");
        history.add(Message.user(task.userInput()));
    }

    public AgentTask task() {
        return task;
    }

    public LocalDate today() {
        return today;
    }

    public List<Message> history() {
        return List.copyOf(history);
    }

    public List<ToolSpec> toolSpecs() {
        return toolSpecs;
    }

    public void setToolSpecs(List<ToolSpec> specs) {
        this.toolSpecs = List.copyOf(specs);
    }

    public List<Observation> observations() {
        return List.copyOf(observations);
    }

    public List<ToolResult> results() {
        return observations.stream().map(Observation::result).toList();
    }

    /** Records a tool call and its result, in the form the model expects to see them. */
    public void addObservation(ToolCall call, ToolResult result) {
        observations.add(new Observation(call, result));
        history.add(Message.assistant("", List.of(call)));
        history.add(Message.toolResult(call.id(), result.forModel()));
    }

    /** Adds the remembered conversation before the current question, and the person's preferences. */
    public void useMemory(List<Message> recent, String note) {
        prior.addAll(recent);
        history.addAll(0, recent);
        profileNote = note == null ? "" : note;
    }

    /** Messages remembered from earlier turns (empty when memory is not used). */
    public List<Message> priorMessages() {
        return List.copyOf(prior);
    }

    public String profileNote() {
        return profileNote;
    }

    public void addAssistantText(String text) {
        history.add(Message.assistant(text, List.of()));
    }

    public void addUserMessage(String text) {
        history.add(Message.user(text));
    }

    public void addTokens(int tokens) {
        tokensUsed += tokens;
    }

    public int tokensUsed() {
        return tokensUsed;
    }
}
