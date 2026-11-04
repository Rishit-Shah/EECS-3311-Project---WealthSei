package wealthsei.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import wealthsei.domain.FeasibilityReport;
import wealthsei.domain.GoalRequest;
import wealthsei.domain.GoalStatus;
import wealthsei.domain.Money;
import wealthsei.domain.SavingsGoal;
import wealthsei.repository.GoalRepository;

/**
 * Creates and tracks savings goals. A goal's status comes from simple rules, not a model:
 * achieved when saved reaches the target; on track when the monthly contribution covers what is
 * needed; at risk when it does not but the monthly surplus could; behind otherwise.
 * Every date is passed in, so callers and tests control "today".
 */
public class GoalService {

    private final GoalRepository repo;
    private final GoalPlanner planner;
    private final MonthlySurplusProvider surplus;
    private final AlertMonitor monitor;
    private final Supplier<String> ids;

    public GoalService(GoalRepository repo, GoalPlanner planner, MonthlySurplusProvider surplus, AlertMonitor monitor) {
        this(repo, planner, surplus, monitor, () -> UUID.randomUUID().toString());
    }

    public GoalService(GoalRepository repo, GoalPlanner planner, MonthlySurplusProvider surplus,
                       AlertMonitor monitor, Supplier<String> ids) {
        this.repo = Objects.requireNonNull(repo, "repo");
        this.planner = Objects.requireNonNull(planner, "planner");
        this.surplus = Objects.requireNonNull(surplus, "surplus");
        this.monitor = Objects.requireNonNull(monitor, "monitor");
        this.ids = Objects.requireNonNull(ids, "ids");
    }

    /**
     * @throws ValidationException if the name is blank, the target is not above zero, the saved
     *                             amount is negative or the deadline is not in the future
     */
    public SavingsGoal createGoal(GoalRequest request, LocalDate today) {
        Objects.requireNonNull(request, "request");
        if (request.name().isBlank()) {
            throw new ValidationException("A goal needs a name");
        }
        if (!request.target().isPositive()) {
            throw new ValidationException("The target must be more than zero");
        }
        if (request.saved().isNegative()) {
            throw new ValidationException("The amount already saved cannot be negative");
        }
        if (!request.deadline().isAfter(today)) {
            throw new ValidationException("The deadline must be after today (" + today + ")");
        }
        SavingsGoal draft = new SavingsGoal(ids.get(), request.name().trim(), request.target(), request.deadline(),
                request.saved(), Money.ZERO, GoalStatus.BEHIND);
        return store(draft, today);
    }

    public List<SavingsGoal> goals() {
        return repo.findAll();
    }

    /** @throws ValidationException if there is no goal with that id */
    public SavingsGoal find(String goalId) {
        return repo.findById(goalId).orElseThrow(() -> new ValidationException("No goal with id '" + goalId + "'"));
    }

    /** Works out the goal's current status without saving it. */
    public GoalStatus statusOf(SavingsGoal goal, LocalDate today) {
        if (goal.saved().compareTo(goal.target()) >= 0) {
            return GoalStatus.ACHIEVED;
        }
        Money required = planner.requiredMonthly(goal, today);
        if (planner.monthsLeft(goal.deadline(), today) == 0) {
            return GoalStatus.BEHIND;
        }
        if (!goal.monthlyContribution().isLessThan(required)) {
            return GoalStatus.ON_TRACK;
        }
        return available(goal, today).isLessThan(required) ? GoalStatus.BEHIND : GoalStatus.AT_RISK;
    }

    /** Re-checks one goal, saves the new status and raises an alert if it got worse. */
    public SavingsGoal evaluate(String goalId, LocalDate today) {
        return store(find(goalId), today);
    }

    /** Re-checks every goal (done after an import, when the numbers may have changed). */
    public List<SavingsGoal> evaluateAll(LocalDate today) {
        return repo.findAll().stream().map(g -> store(g, today)).toList();
    }

    /** Can the goal be reached on time, and what can be changed if not. */
    public FeasibilityReport feasibility(String goalId, LocalDate today) {
        SavingsGoal goal = find(goalId);
        Money available = available(goal, today);
        return new FeasibilityReport(planner.requiredMonthly(goal, today), available,
                planner.isFeasible(goal, available, today), planner.buildOptions(goal, available, today));
    }

    /** Sets what the user puts aside each month for the goal. */
    public SavingsGoal updateContribution(String goalId, Money monthly, LocalDate today) {
        if (monthly.isNegative()) {
            throw new ValidationException("The monthly contribution cannot be negative");
        }
        SavingsGoal g = find(goalId);
        return store(new SavingsGoal(g.id(), g.name(), g.target(), g.deadline(), g.saved(), monthly, g.status()), today);
    }

    /** Adds money that has been set aside to the goal. */
    public SavingsGoal addSaved(String goalId, Money amount, LocalDate today) {
        if (!amount.isPositive()) {
            throw new ValidationException("The amount to add must be more than zero");
        }
        SavingsGoal g = find(goalId);
        return store(new SavingsGoal(g.id(), g.name(), g.target(), g.deadline(), g.saved().add(amount),
                g.monthlyContribution(), g.status()), today);
    }

    /** What the goal could put aside each month: the surplus minus what the other open goals already take. */
    private Money available(SavingsGoal goal, LocalDate today) {
        Money others = Money.ZERO;
        for (SavingsGoal other : repo.findAll()) {
            if (!other.id().equals(goal.id()) && !other.isAchieved()) {
                others = others.add(other.monthlyContribution());
            }
        }
        return surplus.monthlySurplus(today).subtract(others);
    }

    private SavingsGoal store(SavingsGoal goal, LocalDate today) {
        GoalStatus status = statusOf(goal, today);
        SavingsGoal updated = new SavingsGoal(goal.id(), goal.name(), goal.target(), goal.deadline(), goal.saved(),
                goal.monthlyContribution(), status);
        repo.save(updated);
        monitor.evaluateGoal(updated, planner.requiredMonthly(updated, today));
        return updated;
    }
}
