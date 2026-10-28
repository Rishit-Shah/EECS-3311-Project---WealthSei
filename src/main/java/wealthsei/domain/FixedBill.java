package wealthsei.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * A recurring bill such as rent or a phone plan.
 *
 * @param name        label, e.g. "Rent"
 * @param amount      amount due each time (positive)
 * @param frequency   how often it is due
 * @param nextDueDate the next due date (also the anchor for later dates)
 * @param category    optional spending category; {@code null} when not set
 */
public record FixedBill(String name, Money amount, Frequency frequency, LocalDate nextDueDate,
                        Category category) {

    public FixedBill {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(frequency, "frequency");
        Objects.requireNonNull(nextDueDate, "nextDueDate");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Bill name must not be blank");
        }
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Bill amount must be positive, was " + amount);
        }
    }

    public Optional<Category> categoryIfSet() {
        return Optional.ofNullable(category);
    }

    public Money monthlyEquivalent() {
        return frequency.monthlyEquivalent(amount);
    }
}
