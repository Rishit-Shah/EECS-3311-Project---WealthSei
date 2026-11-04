package wealthsei.analytics;

import java.util.List;
import wealthsei.domain.Money;
import wealthsei.domain.RecurringPayment;

/**
 * Writes the plain-language summary from the computed facts. Used when no language model is
 * available, and as the fixed wording the numbers always come from.
 */
final class RecurringSummaryTemplate {

    private RecurringSummaryTemplate() {
    }

    static String write(List<RecurringPayment> payments, Money monthlyTotal, boolean hasTransactions) {
        if (!hasTransactions) {
            return "There are no transactions yet. Import a bank CSV first.";
        }
        if (payments.isEmpty()) {
            return "No recurring payments found. A pattern needs at least 3 payments at a regular interval.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("You have ").append(payments.size()).append(payments.size() == 1 ? " recurring payment" : " recurring payments")
                .append(" costing about ").append(monthlyTotal).append(" per month.");
        RecurringPayment top = payments.get(0);
        sb.append(" The biggest is ").append(top.merchant()).append(" at ").append(top.amount()).append(' ')
                .append(top.frequency().name().toLowerCase()).append('.');
        for (RecurringPayment p : payments) {
            if (p.hasPriceIncrease()) {
                sb.append(' ').append(p.merchant()).append(" went up by ").append(p.priceIncrease()).append('.');
            }
        }
        long possible = payments.stream().filter(RecurringPayment::isPossible).count();
        if (possible > 0) {
            sb.append(' ').append(possible).append(possible == 1 ? " pattern is" : " patterns are")
                    .append(" only possible because the dates or amounts vary.");
        }
        long flagged = payments.stream().filter(RecurringPayment::flaggedForReview).count();
        if (flagged > 0) {
            sb.append(' ').append(flagged).append(" flagged for review.");
        }
        return sb.toString();
    }
}
