package wealthsei.analytics;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import wealthsei.domain.Money;
import wealthsei.domain.RecurringPayment;
import wealthsei.domain.TxFilter;
import wealthsei.repository.RecurringRepository;
import wealthsei.repository.TransactionRepository;
import wealthsei.service.ValidationException;

/**
 * Looks at the stored history and answers questions about it. Everything here is computed by
 * ordinary code; a language model, when added, only phrases the result.
 */
public class AnalyticsService {

    private final TransactionRepository txRepo;
    private final RecurringRepository flags;
    private final RecurringDetector detector;

    public AnalyticsService(TransactionRepository txRepo, RecurringRepository flags, RecurringDetector detector) {
        this.txRepo = Objects.requireNonNull(txRepo, "txRepo");
        this.flags = Objects.requireNonNull(flags, "flags");
        this.detector = Objects.requireNonNull(detector, "detector");
    }

    /** Recurring payments as of {@code asOf}, with the user's review flags applied. */
    public List<RecurringPayment> detectRecurring(LocalDate asOf) {
        return detector.detect(txRepo.query(TxFilter.all()), asOf).stream()
                .map(p -> p.withFlag(flags.isFlagged(p.merchant())))
                .toList();
    }

    /** The recurring-payments panel: the list, the monthly total and a summary. */
    public RecurringReport recurringReport(LocalDate asOf) {
        List<RecurringPayment> payments = detectRecurring(asOf);
        Money total = payments.stream().map(RecurringPayment::monthlyCost).reduce(Money.ZERO, Money::add);
        boolean hasTransactions = !txRepo.query(TxFilter.all()).isEmpty();
        return new RecurringReport(payments, total,
                RecurringSummaryTemplate.write(payments, total, hasTransactions));
    }

    /**
     * Marks a recurring payment for review, or clears the mark.
     *
     * @throws ValidationException if no recurring payment with that merchant name is found
     */
    public void flagRecurring(String merchant, boolean flagged, LocalDate asOf) {
        Objects.requireNonNull(merchant, "merchant");
        boolean known = detectRecurring(asOf).stream()
                .anyMatch(p -> RecurringRepository.keyOf(p.merchant()).equals(RecurringRepository.keyOf(merchant)));
        if (!known) {
            throw new ValidationException("No recurring payment found for '" + merchant + "'");
        }
        flags.setFlagged(merchant, flagged);
    }
}
