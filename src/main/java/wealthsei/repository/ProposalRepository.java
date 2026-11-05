package wealthsei.repository;

import java.util.List;
import java.util.Optional;
import wealthsei.command.Proposal;

/** Storage for proposals. */
public interface ProposalRepository {

    void save(Proposal proposal);

    Optional<Proposal> findById(String proposalId);

    /** Proposals still waiting for a decision, oldest first. */
    List<Proposal> findPending();

    /** Every proposal, oldest first. */
    List<Proposal> findAll();
}
