package wealthsei.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A recurring income such as a salary.
 *
 * @param name        label, e.g. "Salary"
 * @param amount      amount received each time (positive)
 * @param frequency   how often it arrives
 * @param nextPayDate the next date it arrives (also the anchor for later dates)
 */
public record IncomeSource(String name, Money amount, Frequency frequency, LocalDate nextPayDate) {

    public IncomeSource {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(frequency, "frequency");
        Objects.requireNonNull(nextPayDate, "nextPayDate");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Income name must not be blank");
        }
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Income amount must be positive, was " + amount);
        }
    }

    public Money monthlyEquivalent() {
        return frequency.monthlyEquivalent(amount);
    }
}
