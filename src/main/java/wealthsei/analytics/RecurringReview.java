package wealthsei.analytics;

import java.util.Objects;

/**
 * A short review of the recurring payments.
 *
 * @param text the review, in plain sentences
 * @param byAi true if a language model worded it, false if it is the fixed template wording
 * @param note why the template was used when {@code byAi} is false (empty otherwise)
 */
public record RecurringReview(String text, boolean byAi, String note) {

    public RecurringReview {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(note, "note");
    }
}
