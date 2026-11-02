package wealthsei.service;

import java.time.Instant;
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

    /** Sends an alert to every listener (also used for low-balance and goal alerts later). */
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
