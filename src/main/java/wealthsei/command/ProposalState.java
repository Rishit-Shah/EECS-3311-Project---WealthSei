package wealthsei.command;

/**
 * Where a proposal is in its life (State pattern). Each state says which moves are legal; an
 * illegal move throws {@link IllegalStateTransitionException}.
 */
public interface ProposalState {

    void approve(Proposal proposal, CommandHistory<Command> history);

    void reject(Proposal proposal);

    void expire(Proposal proposal);

    /** Reverses an applied proposal; only the applied state allows it. */
    default void undo(Proposal proposal, CommandHistory<Command> history) {
        throw new IllegalStateTransitionException("Only an applied proposal can be undone; this one is " + name());
    }

    String name();

    /** The standard refusal for a move this state does not allow. */
    static IllegalStateTransitionException refuse(String move, ProposalState state) {
        return new IllegalStateTransitionException("Cannot " + move + " a proposal that is " + state.name()
                + ". Only pending proposals can be approved, rejected or expired.");
    }
}
