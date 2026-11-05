package wealthsei.agent;

import java.util.Objects;

/** A change the agent suggests. It is only a suggestion until the user approves it. */
public record ProposalDraft(String title, String rationale) {

    public ProposalDraft {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(rationale, "rationale");
    }
}
