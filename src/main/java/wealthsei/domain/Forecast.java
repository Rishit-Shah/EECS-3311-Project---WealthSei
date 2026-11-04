package wealthsei.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * The projected balance for each day, starting with today. Only money that is already
 * committed is projected (income, bills, recurring payments, planned expenses), not everyday spending.
 *
 * @param days one entry per day in date order; the first is today
 */
public record Forecast(List<DailyBalance> days) {

    public Forecast {
        days = List.copyOf(Objects.requireNonNull(days, "days"));
        if (days.isEmpty()) {
            throw new IllegalArgumentException("A forecast needs at least one day");
        }
    }

    /** The day with the lowest projected balance (the earliest one if several tie). */
    public DailyBalance lowest() {
        return days.stream().min(Comparator.comparing(DailyBalance::balance)).orElseThrow();
    }

    public Money lowestBalance() {
        return lowest().balance();
    }
}
