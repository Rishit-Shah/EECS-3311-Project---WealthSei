package wealthsei.domain;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/**
 * The saved monthly review: the score and the written summary, plus the proposals it led to.
 *
 * @param month       the month reviewed
 * @param healthScore the score at the time
 * @param narrative   what happened, what went well, what to fix
 * @param proposalIds the proposals created from this review
 * @param traceId     the agent run that wrote the narrative, or empty text for a metrics-only review
 * @param generatedAt when it was made
 */
public record MonthlyReview(YearMonth month, HealthScore healthScore, String narrative, List<String> proposalIds,
                            String traceId, Instant generatedAt) {

    public MonthlyReview {
        Objects.requireNonNull(month, "month");
        Objects.requireNonNull(healthScore, "healthScore");
        Objects.requireNonNull(narrative, "narrative");
        proposalIds = List.copyOf(Objects.requireNonNull(proposalIds, "proposalIds"));
        traceId = traceId == null ? "" : traceId;
        Objects.requireNonNull(generatedAt, "generatedAt");
    }
}
