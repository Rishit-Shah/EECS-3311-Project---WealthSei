package wealthsei.command;

/** The change was made. It can be undone, which moves the proposal to the undone state. */
public class AppliedState implements ProposalState {

    @Override
    public void approve(Proposal proposal, CommandHistory<Command> history) {
        throw ProposalState.refuse("approve", this);
    }

    @Override
    public void reject(Proposal proposal) {
        throw ProposalState.refuse("reject", this);
    }

    @Override
    public void expire(Proposal proposal) {
        throw ProposalState.refuse("expire", this);
    }

    @Override
    public void undo(Proposal proposal, CommandHistory<Command> history) {
        proposal.setState(new UndoneState());
    }

    @Override
    public String name() {
        return "Applied";
    }
}
