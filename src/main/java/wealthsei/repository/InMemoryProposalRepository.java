package wealthsei.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import wealthsei.command.Proposal;

/** Keeps proposals for the length of the session. */
public class InMemoryProposalRepository implements ProposalRepository {

    private final Map<String, Proposal> byId = new LinkedHashMap<>();

    @Override
    public void save(Proposal proposal) {
        Objects.requireNonNull(proposal, "proposal");
        byId.put(proposal.id(), proposal);
    }

    @Override
    public Optional<Proposal> findById(String proposalId) {
        return Optional.ofNullable(byId.get(proposalId));
    }

    @Override
    public List<Proposal> findPending() {
        return byId.values().stream().filter(Proposal::isPending).toList();
    }

    @Override
    public List<Proposal> findAll() {
        return new ArrayList<>(byId.values());
    }
}
