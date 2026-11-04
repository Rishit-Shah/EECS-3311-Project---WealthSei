package wealthsei.domain;

import java.util.Objects;

/**
 * One finding about the user's spending.
 *
 * @param type        what kind of habit it is
 * @param title       short heading
 * @param evidence    the figures behind it, e.g. "Weekend days averaged 62.00 vs 18.00 on weekdays"
 * @param severity    how much it matters
 * @param explanation a friendly tip based on the evidence
 */
public record Insight(InsightType type, String title, String evidence, AlertSeverity severity, String explanation) {

    public Insight {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(evidence, "evidence");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(explanation, "explanation");
        if (title.isBlank() || evidence.isBlank()) {
            throw new IllegalArgumentException("Title and evidence must not be blank");
        }
    }

    public Insight withExplanation(String text) {
        return new Insight(type, title, evidence, severity, text);
    }
}
