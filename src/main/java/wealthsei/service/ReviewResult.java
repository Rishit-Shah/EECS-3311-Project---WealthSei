package wealthsei.service;

import java.util.List;
import java.util.Objects;
import wealthsei.command.Proposal;
import wealthsei.domain.MonthlyReview;

/**
 * What generating a review produced.
 *
 * @param review         the saved review
 * @param proposals      the fixes it suggested, waiting for approval
 * @param metricsOnly    true when the AI could not write the narrative, so the text is a fixed summary of the numbers
 * @param replacedEarlier true when an earlier review of the same month was replaced
 * @param alreadyWaiting fixes left out because the same fix is already waiting for approval
 * @param note           why the narrative is metrics-only, or empty
 */
public record ReviewResult(MonthlyReview review, List<Proposal> proposals, boolean metricsOnly,
                           boolean replacedEarlier, int alreadyWaiting, String note) {

    public ReviewResult {
        Objects.requireNonNull(review, "review");
        proposals = List.copyOf(Objects.requireNonNull(proposals, "proposals"));
        note = note == null ? "" : note;
    }
}
