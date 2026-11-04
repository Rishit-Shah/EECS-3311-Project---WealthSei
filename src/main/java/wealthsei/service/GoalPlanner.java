package wealthsei.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.GoalOption;
import wealthsei.domain.GoalOptionKind;
import wealthsei.domain.Money;
import wealthsei.domain.SavingsGoal;

/** The goal arithmetic: how much per month, when it will be done, and what to change if it won't. */
public class GoalPlanner {

    /**
     * Calendar months left until the deadline, where part of a month counts as a whole month
     * (one year away is 12 months, a year and a day is 13). At least 1 before the deadline, 0 once it has passed.
     */
    public long monthsLeft(LocalDate deadline, LocalDate today) {
        if (!deadline.isAfter(today)) {
            return 0;
        }
        long months = ChronoUnit.MONTHS.between(today, deadline);
        if (today.plusMonths(months).isBefore(deadline)) {
            months++;
        }
        return Math.max(1, months);
    }

    /**
     * What must be put aside each month to reach the target by the deadline. Zero once the goal is
     * reached; the whole remaining amount if the deadline has passed.
     */
    public Money requiredMonthly(SavingsGoal goal, LocalDate today) {
        Objects.requireNonNull(goal, "goal");
        Money remaining = goal.remaining();
        if (remaining.isZero()) {
            return Money.ZERO;
        }
        long months = monthsLeft(goal.deadline(), today);
        if (months == 0) {
            return remaining;
        }
        return Money.of(remaining.amount().divide(BigDecimal.valueOf(months), 2, RoundingMode.CEILING));
    }

    /** The day the goal would be reached at this monthly saving, or empty if the surplus is not positive. */
    public Optional<LocalDate> projectedCompletion(SavingsGoal goal, Money monthlySurplus, LocalDate today) {
        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(monthlySurplus, "monthlySurplus");
        Money remaining = goal.remaining();
        if (remaining.isZero()) {
            return Optional.of(today);
        }
        if (!monthlySurplus.isPositive()) {
            return Optional.empty();
        }
        long months = remaining.amount().divide(monthlySurplus.amount(), 0, RoundingMode.CEILING).longValueExact();
        return Optional.of(today.plusMonths(months));
    }

    /** True if the surplus covers what is needed each month. */
    public boolean isFeasible(SavingsGoal goal, Money monthlySurplus, LocalDate today) {
        Money required = requiredMonthly(goal, today);
        return required.isZero() || !monthlySurplus.isLessThan(required);
    }

    /** Suggestions: a single "keep going" if the goal is reachable, otherwise up to three changes. */
    public List<GoalOption> buildOptions(SavingsGoal goal, Money monthlySurplus, LocalDate today) {
        List<GoalOption> options = new ArrayList<>();
        Money required = requiredMonthly(goal, today);
        if (required.isZero()) {
            return options;
        }
        if (isFeasible(goal, monthlySurplus, today)) {
            options.add(new GoalOption(GoalOptionKind.KEEP_PLAN,
                    "Put aside " + required + " per month until " + goal.deadline(),
                    "Reaches " + goal.target() + " on time and leaves " + monthlySurplus.subtract(required)
                            + " per month free"));
            return options;
        }
        Money gap = required.subtract(Money.max(monthlySurplus, Money.ZERO));
        options.add(new GoalOption(GoalOptionKind.INCREASE_SAVING,
                "Free up " + gap + " more per month (spend less or earn more)",
                "Reaches " + goal.target() + " by " + goal.deadline()));
        Optional<LocalDate> later = projectedCompletion(goal, monthlySurplus, today);
        if (later.isPresent()) {
            options.add(new GoalOption(GoalOptionKind.EXTEND_DEADLINE,
                    "Move the deadline to " + later.get(),
                    "Needs only your current surplus of " + monthlySurplus + " per month"));
        }
        long months = monthsLeft(goal.deadline(), today);
        Money reachable = goal.saved().add(Money.max(monthlySurplus, Money.ZERO).multiply(months));
        if (reachable.isPositive() && reachable.isLessThan(goal.target())) {
            options.add(new GoalOption(GoalOptionKind.REDUCE_TARGET,
                    "Lower the target to " + reachable,
                    "Fits your surplus of " + Money.max(monthlySurplus, Money.ZERO) + " per month by "
                            + goal.deadline()));
        }
        return options;
    }
}
