package wealthsei.command;

/** Waiting for the user. Approving runs the command; if it throws, the proposal stays pending. */
public class PendingState implements ProposalState {

    @Override
    public void approve(Proposal proposal, CommandHistory<Command> history) {
        history.execute(proposal.command());
        proposal.setState(new AppliedState());
    }

    @Override
    public void reject(Proposal proposal) {
        proposal.setState(new RejectedState());
    }

    @Override
    public void expire(Proposal proposal) {
        proposal.setState(new ExpiredState());
    }

    @Override
    public String name() {
        return "Pending";
    }
}
