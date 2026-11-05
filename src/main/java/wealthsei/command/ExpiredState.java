package wealthsei.command;

/** Final state: nothing more can happen to the proposal. */
public class ExpiredState implements ProposalState {

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
    public String name() {
        return "Expired";
    }
}
