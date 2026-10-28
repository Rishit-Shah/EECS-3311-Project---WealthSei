package wealthsei.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * How often a payment or income repeats. Also knows how to turn a repeating amount into a
 * monthly figure and how to list the dates it falls on, so the forecasting code never has
 * to special-case a frequency.
 */
public enum Frequency {
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    YEARLY;

    /**
     * The amount converted to an average per-month figure.
     * Weekly = amount x 52 / 12, bi-weekly = amount x 26 / 12, yearly = amount / 12.
     */
    public Money monthlyEquivalent(Money amount) {
        Objects.requireNonNull(amount, "amount");
        return switch (this) {
            case WEEKLY -> amount.multiply(52).divide(12);
            case BIWEEKLY -> amount.multiply(26).divide(12);
            case MONTHLY -> amount;
            case YEARLY -> amount.divide(12);
        };
    }

    /**
     * All dates in [from, to] (both inclusive) on which this frequency falls, counting from
     * {@code anchor} (the first known date). Dates before the anchor are never returned.
     *
     * <p>Each date is computed from the anchor, not from the previous date, so a payment on
     * Jan 31 lands on Feb 28, then Mar 31 (and does not drift to the 28th forever).
     */
    public List<LocalDate> occurrences(LocalDate anchor, LocalDate from, LocalDate to) {
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        List<LocalDate> dates = new ArrayList<>();
        if (to.isBefore(from)) {
            return dates;
        }
        for (long k = 0; ; k++) {
            LocalDate d = step(anchor, k);
            if (d.isAfter(to)) {
                break;
            }
            if (!d.isBefore(from)) {
                dates.add(d);
            }
        }
        return dates;
    }

    private LocalDate step(LocalDate anchor, long k) {
        return switch (this) {
            case WEEKLY -> anchor.plusWeeks(k);
            case BIWEEKLY -> anchor.plusWeeks(2 * k);
            case MONTHLY -> anchor.plusMonths(k);
            case YEARLY -> anchor.plusYears(k);
        };
    }
}
