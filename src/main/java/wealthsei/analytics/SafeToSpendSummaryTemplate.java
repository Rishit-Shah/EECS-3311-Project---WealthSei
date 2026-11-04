package wealthsei.analytics;

import wealthsei.domain.DailyBalance;
import wealthsei.domain.Forecast;
import wealthsei.domain.SafeToSpendResult;

/**
 * Writes the plain-language explanation of a safe-to-spend result from the computed figures.
 * Used when no language model is available.
 */
final class SafeToSpendSummaryTemplate {

    private SafeToSpendSummaryTemplate() {
    }

    static String write(SafeToSpendResult r, Forecast forecast, boolean hasTransactions) {
        StringBuilder sb = new StringBuilder();
        if (r.isOverCommitted()) {
            sb.append("You are over-committed by ").append(r.overCommittedBy()).append(" this month, so there is nothing safe to spend.");
        } else {
            sb.append("You can spend about ").append(r.dailyAllowance()).append(" per day for the next ")
                    .append(r.daysLeft()).append(r.daysLeft() == 1 ? " day" : " days").append(" (")
                    .append(r.remainingThisMonth()).append(" in total).");
        }
        sb.append(" That counts your balance of ").append(r.balance());
        if (r.expectedIncome().isPositive()) {
            sb.append(", ").append(r.expectedIncome()).append(" of expected income");
        }
        sb.append(", and sets aside ").append(r.upcomingBills()).append(" for upcoming bills");
        if (r.goalReserve().isPositive()) {
            sb.append(", ").append(r.goalReserve()).append(" for goals");
        }
        sb.append(" and your ").append(r.safetyBuffer()).append(" safety buffer.");
        DailyBalance low = forecast.lowest();
        if (low.balance().isLessThan(r.safetyBuffer())) {
            sb.append(" Your balance is projected to fall to ").append(low.balance()).append(" on ")
                    .append(low.date()).append(", below your safety buffer.");
        }
        if (!hasTransactions) {
            sb.append(" No transactions are imported yet, so recurring payments are not included.");
        }
        return sb.toString();
    }
}
