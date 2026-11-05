package wealthsei.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * What a purchase would do to the next month of money, worked out by code (never by the model).
 *
 * @param description      the item
 * @param price            its cost
 * @param date             when it would be bought
 * @param lowestBefore     the lowest projected balance without the purchase
 * @param lowestAfter      the lowest projected balance with it
 * @param allowanceBefore  safe-to-spend per day without it
 * @param allowanceAfter   safe-to-spend per day with it
 * @param safetyBuffer     the money the user wants to keep untouched
 * @param goalsNeedingHelp number of goals that are at risk or behind
 * @param suggested        the verdict the rules give
 * @param reason           one sentence saying which rule decided it
 */
public record PurchaseImpact(String description, Money price, LocalDate date, DailyBalance lowestBefore,
                             DailyBalance lowestAfter, Money allowanceBefore, Money allowanceAfter,
                             Money safetyBuffer, int goalsNeedingHelp, Verdict suggested, String reason) {

    public PurchaseImpact {
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(lowestBefore, "lowestBefore");
        Objects.requireNonNull(lowestAfter, "lowestAfter");
        Objects.requireNonNull(allowanceBefore, "allowanceBefore");
        Objects.requireNonNull(allowanceAfter, "allowanceAfter");
        Objects.requireNonNull(safetyBuffer, "safetyBuffer");
        Objects.requireNonNull(suggested, "suggested");
        Objects.requireNonNull(reason, "reason");
    }
}
