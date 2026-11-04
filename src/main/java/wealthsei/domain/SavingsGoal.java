package wealthsei.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Something the user is saving for.
 *
 * @param id                  unique id
 * @param name                what it is for, e.g. "Laptop"
 * @param target              the amount needed (positive)
 * @param deadline            the date the money is needed by
 * @param saved               how much has been put aside so far
 * @param monthlyContribution what the user plans to put aside each month
 * @param status              how the goal is doing
 */
public record SavingsGoal(String id, String name, Money target, LocalDate deadline, Money saved,
                          Money monthlyContribution, GoalStatus status) {

    public SavingsGoal {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(deadline, "deadline");
        Objects.requireNonNull(saved, "saved");
        Objects.requireNonNull(monthlyContribution, "monthlyContribution");
        Objects.requireNonNull(status, "status");
        if (id.isBlank() || name.isBlank()) {
            throw new IllegalArgumentException("Goal id and name must not be blank");
        }
        if (!target.isPositive()) {
            throw new IllegalArgumentException("Goal target must be positive, was " + target);
        }
        if (saved.isNegative() || monthlyContribution.isNegative()) {
            throw new IllegalArgumentException("Saved amount and monthly contribution must not be negative");
        }
    }

    /** How much is still needed (zero once the target is reached). */
    public Money remaining() {
        return Money.max(Money.ZERO, target.subtract(saved));
    }

    public boolean isAchieved() {
        return status == GoalStatus.ACHIEVED;
    }
}
