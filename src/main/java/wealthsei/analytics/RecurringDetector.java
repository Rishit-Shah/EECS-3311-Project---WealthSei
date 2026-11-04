package wealthsei.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import wealthsei.domain.Frequency;
import wealthsei.domain.Money;
import wealthsei.domain.RecurringPayment;
import wealthsei.domain.Transaction;

/**
 * Finds payments that repeat at a regular interval. No AI is involved: expenses are grouped
 * by merchant, and a group counts as recurring when it has at least three payments, the
 * gaps between them match one frequency, and the amounts stay similar.
 *
 * <p>Confidence is 0.6 x (share of gaps that match the frequency) + 0.4 x (share of payments
 * within 10% of the payment before). At least 75% of gaps and 50% of amounts must be regular,
 * otherwise the group is not reported at all.
 */
public class RecurringDetector {

    static final int MIN_OCCURRENCES = 3;
    static final double MIN_GAP_SHARE = 0.75;
    static final double MIN_AMOUNT_SHARE = 0.5;
    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.10");

    /**
     * @param txs  transaction history (income and later-dated rows are ignored)
     * @param asOf today; a pattern whose last payment is more than two intervals old is treated as
     *             cancelled and left out
     * @return patterns, the most expensive per month first
     */
    public List<RecurringPayment> detect(List<Transaction> txs, LocalDate asOf) {
        Objects.requireNonNull(txs, "txs");
        Objects.requireNonNull(asOf, "asOf");
        Map<String, List<Transaction>> groups = new LinkedHashMap<>();
        txs.stream()
                .filter(Transaction::isExpense)
                .filter(tx -> !tx.date().isAfter(asOf))
                .sorted(Comparator.comparing(Transaction::date))
                .forEach(tx -> groups.computeIfAbsent(tx.merchant().trim().toLowerCase(Locale.ROOT),
                        k -> new ArrayList<>()).add(tx));
        List<RecurringPayment> found = new ArrayList<>();
        for (List<Transaction> group : groups.values()) {
            RecurringPayment p = analyse(group, asOf);
            if (p != null) {
                found.add(p);
            }
        }
        found.sort(Comparator.comparing(RecurringPayment::monthlyCost).reversed()
                .thenComparing(RecurringPayment::merchant));
        return found;
    }

    private RecurringPayment analyse(List<Transaction> group, LocalDate asOf) {
        if (group.size() < MIN_OCCURRENCES) {
            return null;
        }
        List<Long> gaps = new ArrayList<>();
        for (int i = 1; i < group.size(); i++) {
            gaps.add(ChronoUnit.DAYS.between(group.get(i - 1).date(), group.get(i).date()));
        }
        Frequency frequency = frequencyOf(median(gaps));
        if (frequency == null) {
            return null;
        }
        double gapShare = share(gaps.stream().filter(g -> matches(frequency, g)).count(), gaps.size());
        if (gapShare < MIN_GAP_SHARE) {
            return null;
        }
        int steady = 0;
        for (int i = 1; i < group.size(); i++) {
            if (withinTolerance(group.get(i - 1).amount(), group.get(i).amount())) {
                steady++;
            }
        }
        double amountShare = share(steady, group.size() - 1);
        if (amountShare < MIN_AMOUNT_SHARE) {
            return null;
        }
        Transaction last = group.get(group.size() - 1);
        if (ChronoUnit.DAYS.between(last.date(), asOf) > 2 * typicalDays(frequency)) {
            return null;
        }
        Money before = group.get(group.size() - 2).amount();
        Money increase = last.amount().isGreaterThan(before) ? last.amount().subtract(before) : Money.ZERO;
        double confidence = Math.round((0.6 * gapShare + 0.4 * amountShare) * 100.0) / 100.0;
        return new RecurringPayment(last.merchant(), last.amount(), frequency, nextDue(last.date(), frequency),
                confidence, false, increase, group.size());
    }

    private static double share(long part, int whole) {
        return (double) part / whole;
    }

    private static long median(List<Long> values) {
        List<Long> sorted = values.stream().sorted().toList();
        return sorted.get(sorted.size() / 2);
    }

    /** The frequency whose usual gap contains {@code days}, or null if none does. */
    private static Frequency frequencyOf(long days) {
        for (Frequency f : Frequency.values()) {
            if (matches(f, days)) {
                return f;
            }
        }
        return null;
    }

    private static boolean matches(Frequency f, long days) {
        return switch (f) {
            case WEEKLY -> days >= 6 && days <= 8;
            case BIWEEKLY -> days >= 13 && days <= 15;
            case MONTHLY -> days >= 26 && days <= 35;
            case YEARLY -> days >= 355 && days <= 375;
        };
    }

    private static long typicalDays(Frequency f) {
        return switch (f) {
            case WEEKLY -> 7;
            case BIWEEKLY -> 14;
            case MONTHLY -> 30;
            case YEARLY -> 365;
        };
    }

    private static LocalDate nextDue(LocalDate last, Frequency f) {
        return switch (f) {
            case WEEKLY -> last.plusDays(7);
            case BIWEEKLY -> last.plusDays(14);
            case MONTHLY -> last.plusMonths(1);
            case YEARLY -> last.plusYears(1);
        };
    }

    private static boolean withinTolerance(Money previous, Money current) {
        BigDecimal allowed = previous.amount().multiply(AMOUNT_TOLERANCE);
        return previous.amount().subtract(current.amount()).abs().compareTo(allowed) <= 0;
    }
}
