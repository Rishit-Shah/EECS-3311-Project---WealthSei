package wealthsei.command;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Supplier;
import wealthsei.analytics.AnalyticsService;

/** Marks a recurring payment for review; undoing it clears the mark unless it was already there. */
public class FlagSubscriptionCommand implements Command {

    private final AnalyticsService analytics;
    private final Supplier<LocalDate> today;
    private final String merchant;
    private boolean wasFlagged;

    public FlagSubscriptionCommand(AnalyticsService analytics, Supplier<LocalDate> today, String merchant) {
        this.analytics = Objects.requireNonNull(analytics, "analytics");
        this.today = Objects.requireNonNull(today, "today");
        this.merchant = Objects.requireNonNull(merchant, "merchant");
    }

    @Override
    public void execute() {
        wasFlagged = analytics.detectRecurring(today.get()).stream()
                .anyMatch(p -> p.merchant().equalsIgnoreCase(merchant) && p.flaggedForReview());
        analytics.flagRecurring(merchant, true, today.get());
    }

    @Override
    public void undo() {
        if (!wasFlagged) {
            analytics.flagRecurring(merchant, false, today.get());
        }
    }

    @Override
    public String describe() {
        return "Flag " + merchant + " for review";
    }
}
