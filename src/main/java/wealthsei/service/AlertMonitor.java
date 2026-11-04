package wealthsei.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import wealthsei.domain.Alert;
import wealthsei.domain.AlertSeverity;
import wealthsei.domain.AlertType;
import wealthsei.domain.BudgetLineStatus;
import wealthsei.domain.BudgetStatus;
import wealthsei.domain.DailyBalance;
import wealthsei.domain.GoalStatus;
import wealthsei.domain.Money;
import wealthsei.domain.SavingsGoal;

/**
 * The subject in the Observer pattern: keeps the list of {@link AlertListener}s and tells all
 * of them whenever an alert is published.
 *
 * <p>To avoid repeating itself, a budget alert for a category is only raised when its level
 * gets worse than the last one raised (nothing to WARNING, WARNING to OVER). Dropping back
 * under 80% resets it. One failing listener never stops the others from being told.
 */
public class AlertMonitor {

    private final List<AlertListener> listeners = new ArrayList<>();
    private final Map<String, AlertSeverity> lastRaised = new HashMap<>();
    private final Supplier<Instant> clock;

    public AlertMonitor() {
        this(Instant::now);
    }

    /** @param clock supplies the time stamped on each alert (tests pass a fixed one) */
    public AlertMonitor(Supplier<Instant> clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void attach(AlertListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void detach(AlertListener listener) {
        listeners.remove(listener);
    }

    /** Raises a budget alert for each category that has newly reached 80% or gone over its limit. */
    public void evaluate(BudgetStatus status) {
        for (BudgetLineStatus line : status.lines()) {
            String key = status.month() + "/" + line.category().id();
            AlertSeverity level = line.level();
            if (level == AlertSeverity.INFO) {
                lastRaised.remove(key);
                continue;
            }
            AlertSeverity previous = lastRaised.get(key);
            lastRaised.put(key, level);
            if (previous == null || level.compareTo(previous) > 0) {
                publish(new Alert(AlertType.BUDGET, level, messageFor(line, level), clock.get()));
            }
        }
    }

    /**
     * Raises a LOW_BALANCE alert when the projected balance falls below the safety buffer. Like
     * budget alerts, it is raised when the situation first appears or gets worse (WARNING to
     * OVER, when the balance would go below zero), and again only after the balance has been
     * back above the buffer.
     *
     * @param lowest the day with the lowest projected balance
     * @param buffer the safety buffer
     */
    public void evaluateLowBalance(DailyBalance lowest, Money buffer) {
        Objects.requireNonNull(lowest, "lowest");
        Objects.requireNonNull(buffer, "buffer");
        String key = "low-balance";
        if (!lowest.balance().isLessThan(buffer)) {
            lastRaised.remove(key);
            return;
        }
        AlertSeverity level = lowest.balance().isNegative() ? AlertSeverity.OVER : AlertSeverity.WARNING;
        AlertSeverity previous = lastRaised.get(key);
        lastRaised.put(key, level);
        if (previous == null || level.compareTo(previous) > 0) {
            LocalDate on = lowest.date();
            publish(new Alert(AlertType.LOW_BALANCE, level, "Balance is projected to drop to $"
                    + lowest.balance() + " on " + on + ", below your $" + buffer + " safety buffer", clock.get()));
        }
    }

    /**
     * Raises a GOAL_RISK alert when a goal newly becomes AT_RISK (WARNING) or BEHIND (OVER), or gets worse.
     * Goals that are on track or achieved reset it.
     *
     * @param requiredMonthly what the goal needs per month, used in the message
     */
    public void evaluateGoal(SavingsGoal goal, Money requiredMonthly) {
        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(requiredMonthly, "requiredMonthly");
        String key = "goal/" + goal.id();
        GoalStatus status = goal.status();
        if (status == GoalStatus.ON_TRACK || status == GoalStatus.ACHIEVED) {
            lastRaised.remove(key);
            return;
        }
        AlertSeverity level = status == GoalStatus.BEHIND ? AlertSeverity.OVER : AlertSeverity.WARNING;
        AlertSeverity previous = lastRaised.get(key);
        lastRaised.put(key, level);
        if (previous == null || level.compareTo(previous) > 0) {
            String how = status == GoalStatus.BEHIND ? "is behind" : "is at risk";
            publish(new Alert(AlertType.GOAL_RISK, level, "Goal '" + goal.name() + "' " + how + ": it needs $"
                    + requiredMonthly + " per month to reach $" + goal.target() + " by " + goal.deadline()
                    + " (you are putting aside $" + goal.monthlyContribution() + ")", clock.get()));
        }
    }

    public void publish(Alert alert) {
        notifyListeners(Objects.requireNonNull(alert, "alert"));
    }

    private void notifyListeners(Alert alert) {
        for (AlertListener listener : List.copyOf(listeners)) {
            try {
                listener.onAlert(alert);
            } catch (RuntimeException e) {
                // A broken listener (for example a closed window) must not block the rest.
            }
        }
    }

    private static String messageFor(BudgetLineStatus line, AlertSeverity level) {
        String name = line.category().name();
        if (level == AlertSeverity.OVER) {
            return name + " is over budget: $" + line.spent() + " spent of $" + line.limit();
        }
        long percent = Math.round(line.ratio() * 100);
        return name + " is at " + percent + "% of its $" + line.limit() + " budget ($" + line.spent() + " spent)";
    }
}
