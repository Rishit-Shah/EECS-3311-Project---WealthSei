package wealthsei.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * A one-off expense the user already knows is coming (a flight, a laptop, a dentist visit).
 *
 * @param description what it is
 * @param amount      expected cost (positive)
 * @param date        expected date
 * @param category    optional spending category; {@code null} when not set
 */
public record PlannedExpense(String description, Money amount, LocalDate date, Category category) {

    public PlannedExpense {
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(date, "date");
        if (description.isBlank()) {
            throw new IllegalArgumentException("Description must not be blank");
        }
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Planned expense must be positive, was " + amount);
        }
    }

    public Optional<Category> categoryIfSet() {
        return Optional.ofNullable(category);
    }
}
