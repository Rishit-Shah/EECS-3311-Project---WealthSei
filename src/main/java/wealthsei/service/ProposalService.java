package wealthsei.service;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import wealthsei.agent.ProposalDraft;
import wealthsei.command.Command;
import wealthsei.command.CommandFactory;
import wealthsei.command.CommandHistory;
import wealthsei.command.IllegalStateTransitionException;
import wealthsei.command.Proposal;
import wealthsei.domain.Categories;
import wealthsei.domain.Category;
import wealthsei.repository.ProposalRepository;

/**
 * The approval queue. The agent never changes budgets or goals itself: its suggestions become
 * pending proposals here, and only an explicit approval runs the change (through a command
 * history, so it can be undone). Suggestions that touch a protected category are discarded.
 */
public class ProposalService {

    private final ProposalRepository repo;
    private final CommandFactory factory;
    private final Supplier<String> ids;
    private final Supplier<Instant> clock;
    private final ZoneId zone;
    private final CommandHistory<Command> history = new CommandHistory<>();
    private final Set<Category> protectedCategories = new LinkedHashSet<>(Set.of(Categories.GROCERIES));
    private List<String> lastDiscarded = List.of();

    public ProposalService(ProposalRepository repo, CommandFactory factory) {
        this(repo, factory, () -> UUID.randomUUID().toString().substring(0, 8), Instant::now, ZoneId.systemDefault());
    }

    public ProposalService(ProposalRepository repo, CommandFactory factory, Supplier<String> ids,
                           Supplier<Instant> clock, ZoneId zone) {
        this.repo = Objects.requireNonNull(repo, "repo");
        this.factory = Objects.requireNonNull(factory, "factory");
        this.ids = Objects.requireNonNull(ids, "ids");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    /**
     * Stores a pending proposal for each draft that passes the guardrails and can be turned into a
     * change. The others are discarded; {@link #lastDiscarded()} says why.
     */
    public List<Proposal> createFromDrafts(List<ProposalDraft> drafts, String traceId) {
        List<Proposal> created = new ArrayList<>();
        List<String> discarded = new ArrayList<>();
        for (ProposalDraft draft : drafts) {
            if (!passesGuardrails(draft)) {
                discarded.add("'" + draft.title() + "' touches a protected category");
                continue;
            }
            try {
                Command command = factory.create(draft);
                Proposal proposal = new Proposal(ids.get(), draft.type(), draft.title(), draft.rationale(), command,
                        traceId, clock.get());
                repo.save(proposal);
                created.add(proposal);
            } catch (ValidationException | IllegalArgumentException e) {
                discarded.add("'" + draft.title() + "': " + e.getMessage());
            }
        }
        lastDiscarded = List.copyOf(discarded);
        return created;
    }

    /** Why the drafts of the latest {@link #createFromDrafts} call that were not stored were left out. */
    public List<String> lastDiscarded() {
        return lastDiscarded;
    }

    public List<Proposal> listPending() {
        return repo.findPending();
    }

    public List<Proposal> listAll() {
        return repo.findAll();
    }

    /** Finds a proposal by its id or by the start of its id when that is unambiguous. */
    public Proposal find(String idOrPrefix) {
        Optional<Proposal> exact = repo.findById(idOrPrefix);
        if (exact.isPresent()) {
            return exact.get();
        }
        List<Proposal> matches = repo.findAll().stream().filter(p -> p.id().startsWith(idOrPrefix)).toList();
        if (matches.size() == 1) {
            return matches.get(0);
        }
        throw new ValidationException(matches.isEmpty() ? "No proposal with id '" + idOrPrefix + "'"
                : "'" + idOrPrefix + "' matches more than one proposal; type more of the id");
    }

    /**
     * Approves (runs the change) or rejects a pending proposal.
     *
     * @throws IllegalStateTransitionException if it is no longer pending
     * @throws ValidationException             if the change could not be made; the proposal stays pending
     */
    public Proposal decide(String idOrPrefix, boolean approve) {
        Proposal proposal = find(idOrPrefix);
        if (approve) {
            try {
                proposal.approve(history);
            } catch (IllegalStateTransitionException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new ValidationException("Could not apply '" + proposal.title() + "': " + e.getMessage()
                        + " (it is still pending)");
            }
        } else {
            proposal.reject();
        }
        repo.save(proposal);
        return proposal;
    }

    /** Reverses the most recently applied proposal, if any. */
    public Optional<Proposal> undoLast() {
        Optional<Command> undone = history.undo();
        if (undone.isEmpty()) {
            return Optional.empty();
        }
        Proposal proposal = repo.findAll().stream().filter(p -> p.command() == undone.get()).findFirst().orElseThrow();
        proposal.setState(new wealthsei.command.UndoneState());
        repo.save(proposal);
        return Optional.of(proposal);
    }

    /** Expires pending proposals that were made in an earlier month; returns how many. */
    public int expireOld(YearMonth current) {
        int count = 0;
        for (Proposal p : repo.findPending()) {
            if (YearMonth.from(p.createdAt().atZone(zone)).isBefore(current)) {
                p.expire();
                repo.save(p);
                count++;
            }
        }
        return count;
    }

    public void setProtectedCategory(Category category, boolean isProtected) {
        if (isProtected) {
            protectedCategories.add(category);
        } else {
            protectedCategories.remove(category);
        }
    }

    public Set<Category> protectedCategories() {
        return Set.copyOf(protectedCategories);
    }

    private boolean passesGuardrails(ProposalDraft draft) {
        return draft.params().get("category")
                .flatMap(Categories::find)
                .map(c -> !protectedCategories.contains(c))
                .orElse(true);
    }
}
