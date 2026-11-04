package wealthsei.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * What the user types to create a goal.
 *
 * @param name     what it is for
 * @param target   the amount needed
 * @param deadline the date the money is needed by
 * @param saved    already set aside (use {@link Money#ZERO} for nothing)
 */
public record GoalRequest(String name, Money target, LocalDate deadline, Money saved) {

    public GoalRequest {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(deadline, "deadline");
        Objects.requireNonNull(saved, "saved");
    }

    public GoalRequest(String name, Money target, LocalDate deadline) {
        this(name, target, deadline, Money.ZERO);
    }
}
