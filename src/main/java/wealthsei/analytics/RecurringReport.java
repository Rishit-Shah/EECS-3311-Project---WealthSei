package wealthsei.analytics;

import java.util.List;
import java.util.Objects;
import wealthsei.domain.Money;
import wealthsei.domain.RecurringPayment;

/**
 * The recurring-payments panel: the payments, what they cost per month, and a short
 * plain-language summary.
 */
public record RecurringReport(List<RecurringPayment> payments, Money monthlyTotal, String summary) {

    public RecurringReport {
        payments = List.copyOf(Objects.requireNonNull(payments, "payments"));
        Objects.requireNonNull(monthlyTotal, "monthlyTotal");
        Objects.requireNonNull(summary, "summary");
    }
}
