package wealthsei.agent;

import java.util.Objects;
import wealthsei.command.ProposalType;

/**
 * A change the agent suggests. It is only a suggestion until the user approves it.
 *
 * @param type      what kind of change; null means an unknown kind, which is discarded
 * @param title     short description for the approval list
 * @param rationale why it is suggested
 * @param params    the details the change needs (see {@code CommandFactory})
 */
public record ProposalDraft(ProposalType type, String title, String rationale, ParamSet params) {

    public ProposalDraft {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(rationale, "rationale");
        Objects.requireNonNull(params, "params");
    }

    /** A free-text suggestion with no machine-readable change; it cannot become a proposal. */
    public ProposalDraft(String title, String rationale) {
        this(null, title, rationale, ParamSet.empty());
    }
}
