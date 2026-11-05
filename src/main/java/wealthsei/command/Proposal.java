package wealthsei.command;

import java.time.Instant;
import java.util.Objects;

/**
 * A change the agent suggests. Nothing happens until the user approves it; what each move does
 * depends on the current {@link ProposalState}.
 */
public class Proposal {

    private final String id;
    private final ProposalType type;
    private final String title;
    private final String rationale;
    private final Command command;
    private final String traceId;
    private final Instant createdAt;
    private ProposalState state = new PendingState();

    public Proposal(String id, ProposalType type, String title, String rationale, Command command,
                    String traceId, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.title = Objects.requireNonNull(title, "title");
        this.rationale = Objects.requireNonNull(rationale, "rationale");
        this.command = Objects.requireNonNull(command, "command");
        this.traceId = traceId == null ? "" : traceId;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public String id() {
        return id;
    }

    public ProposalType type() {
        return type;
    }

    public String title() {
        return title;
    }

    public String rationale() {
        return rationale;
    }

    public Command command() {
        return command;
    }

    /** The agent run that produced this proposal, or empty text when it did not come from one. */
    public String traceId() {
        return traceId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public void approve(CommandHistory<Command> history) {
        state.approve(this, history);
    }

    public void reject() {
        state.reject(this);
    }

    public void expire() {
        state.expire(this);
    }

    public void undo(CommandHistory<Command> history) {
        state.undo(this, history);
    }

    public void setState(ProposalState next) {
        this.state = Objects.requireNonNull(next, "next");
    }

    public String statusName() {
        return state.name();
    }

    public boolean isPending() {
        return state instanceof PendingState;
    }
}
