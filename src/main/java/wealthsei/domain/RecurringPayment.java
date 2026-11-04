package wealthsei.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A payment that repeats at a regular interval, found in the transaction history.
 *
 * @param merchant        display name, taken from the most recent payment
 * @param amount          the most recent amount
 * @param frequency       how often it repeats
 * @param nextDue         the date the next payment is expected
 * @param confidence      from 0 to 1; how regular the dates and amounts are
 * @param flaggedForReview true if the user marked it to look at again
 * @param priceIncrease   how much the latest payment rose over the one before it (zero if it did not)
 * @param occurrences     how many payments the pattern is based on
 */
public record RecurringPayment(String merchant, Money amount, Frequency frequency, LocalDate nextDue,
                               double confidence, boolean flaggedForReview, Money priceIncrease,
                               int occurrences) {

    /** Below this confidence a pattern is shown as only "possible". */
    public static final double POSSIBLE_BELOW = 0.85;

    public RecurringPayment {
        Objects.requireNonNull(merchant, "merchant");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(frequency, "frequency");
        Objects.requireNonNull(nextDue, "nextDue");
        Objects.requireNonNull(priceIncrease, "priceIncrease");
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0 and 1, was " + confidence);
        }
        if (priceIncrease.isNegative()) {
            throw new IllegalArgumentException("Price increase must not be negative");
        }
        if (occurrences < 1) {
            throw new IllegalArgumentException("A pattern needs at least one payment");
        }
    }

    public boolean isPossible() {
        return confidence < POSSIBLE_BELOW;
    }

    public boolean hasPriceIncrease() {
        return priceIncrease.isPositive();
    }

    /** What this payment costs per month on average. */
    public Money monthlyCost() {
        return frequency.monthlyEquivalent(amount);
    }

    public RecurringPayment withFlag(boolean flagged) {
        return new RecurringPayment(merchant, amount, frequency, nextDue, confidence, flagged,
                priceIncrease, occurrences);
    }
}
