package wealthsei.domain;

import java.util.Objects;

/**
 * What the user can spend per day for the rest of the month.
 *
 * @param dailyAllowance     the amount that can be spent each remaining day (never negative)
 * @param remainingThisMonth the total that can be spent before month end (never negative)
 * @param upcomingBills      bills, recurring payments and planned expenses due before month end
 * @param goalReserve        money set aside for savings goals this month
 * @param explanation        plain-language explanation of the figures
 * @param balance            the balance today
 * @param expectedIncome     income expected before month end
 * @param safetyBuffer       the money kept untouched
 * @param daysLeft           days left in the month, counting today
 * @param overCommittedBy    how far commitments exceed the money available (zero when not over-committed)
 */
public record SafeToSpendResult(Money dailyAllowance, Money remainingThisMonth, Money upcomingBills,
                                Money goalReserve, String explanation, Money balance, Money expectedIncome,
                                Money safetyBuffer, int daysLeft, Money overCommittedBy) {

    public SafeToSpendResult {
        Objects.requireNonNull(dailyAllowance, "dailyAllowance");
        Objects.requireNonNull(remainingThisMonth, "remainingThisMonth");
        Objects.requireNonNull(upcomingBills, "upcomingBills");
        Objects.requireNonNull(goalReserve, "goalReserve");
        Objects.requireNonNull(explanation, "explanation");
        Objects.requireNonNull(balance, "balance");
        Objects.requireNonNull(expectedIncome, "expectedIncome");
        Objects.requireNonNull(safetyBuffer, "safetyBuffer");
        Objects.requireNonNull(overCommittedBy, "overCommittedBy");
        if (dailyAllowance.isNegative() || remainingThisMonth.isNegative() || overCommittedBy.isNegative()) {
            throw new IllegalArgumentException("Allowance, remaining and over-commitment must not be negative");
        }
        if (daysLeft < 1) {
            throw new IllegalArgumentException("Days left must be at least 1, was " + daysLeft);
        }
    }

    public boolean isOverCommitted() {
        return overCommittedBy.isPositive();
    }

    public SafeToSpendResult withExplanation(String text) {
        return new SafeToSpendResult(dailyAllowance, remainingThisMonth, upcomingBills, goalReserve, text,
                balance, expectedIncome, safetyBuffer, daysLeft, overCommittedBy);
    }
}
