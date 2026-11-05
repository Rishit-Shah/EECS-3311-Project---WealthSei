package wealthsei.agent;

import java.util.List;
import java.util.Objects;
import wealthsei.domain.Verdict;

/**
 * What an agent run produced.
 *
 * @param answer     the text for the user
 * @param drafts     suggested changes (waiting for approval)
 * @param status     how the run ended
 * @param grounding  the figure check
 * @param traceId    the id of the saved trace
 * @param verdict    for affordability questions, the final verdict; otherwise null
 * @param tokensUsed tokens the model used in this run
 */
public record AgentResult(String answer, List<ProposalDraft> drafts, AgentStatus status, GroundingReport grounding,
                          String traceId, Verdict verdict, int tokensUsed) {

    public AgentResult {
        Objects.requireNonNull(answer, "answer");
        drafts = List.copyOf(Objects.requireNonNull(drafts, "drafts"));
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(grounding, "grounding");
        Objects.requireNonNull(traceId, "traceId");
    }
}
