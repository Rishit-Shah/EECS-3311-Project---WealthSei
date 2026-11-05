package wealthsei.agent;

import java.util.List;
import java.util.Objects;

/** The model is done: its answer and any changes it suggests. */
public record FinalAnswerStep(String answer, List<ProposalDraft> drafts) implements AgentStep {

    public FinalAnswerStep {
        Objects.requireNonNull(answer, "answer");
        drafts = List.copyOf(Objects.requireNonNull(drafts, "drafts"));
    }

    public FinalAnswerStep(String answer) {
        this(answer, List.of());
    }
}
