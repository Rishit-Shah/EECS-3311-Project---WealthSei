package wealthsei.command;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ProposalStateTest {

    /** A command that records what was done to it, and can be told to fail. */
    private static final class Probe implements Command {
        int executed;
        int undone;
        boolean fail;

        @Override
        public void execute() {
            if (fail) {
                throw new IllegalStateException("category deleted");
            }
            executed++;
        }

        @Override
        public void undo() {
            undone++;
        }

        @Override
        public String describe() {
            return "probe";
        }
    }

    private final Probe probe = new Probe();
    private final CommandHistory<Command> history = new CommandHistory<>();
    private final Proposal proposal = new Proposal("p1", ProposalType.BUDGET_ADJUSTMENT, "Title", "Why", probe, "tr1",
            Instant.parse("2026-11-01T10:00:00Z"));

    @Test
    void startsPendingAndApprovingRunsTheCommandOnce() {
        assertEquals("Pending", proposal.statusName());
        assertTrue(proposal.isPending());
        proposal.approve(history);
        assertEquals("Applied", proposal.statusName());
        assertEquals(1, probe.executed);
    }

    @Test
    void rejectingNeverRunsTheCommand() {
        proposal.reject();
        assertEquals("Rejected", proposal.statusName());
        assertEquals(0, probe.executed);
    }

    @Test
    void expiringNeverRunsTheCommand() {
        proposal.expire();
        assertEquals("Expired", proposal.statusName());
        assertEquals(0, probe.executed);
    }

    @Test
    void anAppliedProposalCannotBeApprovedRejectedOrExpired() {
        proposal.approve(history);
        assertThrows(IllegalStateTransitionException.class, () -> proposal.approve(history));
        assertThrows(IllegalStateTransitionException.class, proposal::reject);
        assertThrows(IllegalStateTransitionException.class, proposal::expire);
        assertEquals(1, probe.executed);
    }

    @Test
    void rejectedAndExpiredProposalsAreFinal() {
        proposal.reject();
        assertThrows(IllegalStateTransitionException.class, () -> proposal.approve(history));
        Proposal other = new Proposal("p2", ProposalType.FLAG_SUBSCRIPTION, "T", "W", new Probe(), "", Instant.now());
        other.expire();
        assertThrows(IllegalStateTransitionException.class, () -> other.approve(history));
        assertThrows(IllegalStateTransitionException.class, other::reject);
    }

    @Test
    void aFailingCommandLeavesTheProposalPending() {
        probe.fail = true;
        assertThrows(IllegalStateException.class, () -> proposal.approve(history));
        assertEquals("Pending", proposal.statusName());
        assertFalse(history.canUndo());
    }

    @Test
    void onlyAnAppliedProposalCanBeUndone() {
        assertThrows(IllegalStateTransitionException.class, () -> proposal.undo(history));
        proposal.approve(history);
        history.undo();
        proposal.undo(history);
        assertEquals("Undone", proposal.statusName());
        assertEquals(1, probe.undone);
        assertThrows(IllegalStateTransitionException.class, () -> proposal.undo(history));
    }

    @Test
    void aMissingTraceIdBecomesEmptyText() {
        Proposal p = new Proposal("p3", ProposalType.RECATEGORIZE, "T", "W", probe, null, Instant.now());
        assertEquals("", p.traceId());
    }
}
