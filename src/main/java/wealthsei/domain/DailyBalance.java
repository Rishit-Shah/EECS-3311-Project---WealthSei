package wealthsei.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * One day of a forecast.
 *
 * @param date     the day
 * @param balance  the projected balance at the end of the day
 * @param income   expected money coming in that day
 * @param outgoing expected bills and payments that day
 */
public record DailyBalance(LocalDate date, Money balance, Money income, Money outgoing) {

    public DailyBalance {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(balance, "balance");
        Objects.requireNonNull(income, "income");
        Objects.requireNonNull(outgoing, "outgoing");
        if (income.isNegative() || outgoing.isNegative()) {
            throw new IllegalArgumentException("Income and outgoing must not be negative");
        }
    }

    public boolean hasActivity() {
        return !income.isZero() || !outgoing.isZero();
    }
}
