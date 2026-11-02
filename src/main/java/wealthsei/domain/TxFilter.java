package wealthsei.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Which transactions to look up: a date range (both ends inclusive), optionally narrowed to
 * one category and/or a merchant name fragment (case-insensitive).
 */
public record TxFilter(LocalDate start, LocalDate end, Optional<Category> category,
                       Optional<String> merchant) {

    public TxFilter {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(merchant, "merchant");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("Filter end " + end + " is before start " + start);
        }
    }

    /** Matches every transaction. */
    public static TxFilter all() {
        return new TxFilter(LocalDate.MIN, LocalDate.MAX, Optional.empty(), Optional.empty());
    }

    public static TxFilter between(LocalDate start, LocalDate end) {
        return new TxFilter(start, end, Optional.empty(), Optional.empty());
    }

    public TxFilter withCategory(Category c) {
        return new TxFilter(start, end, Optional.of(c), merchant);
    }

    public TxFilter withMerchant(String fragment) {
        return new TxFilter(start, end, category, Optional.of(fragment));
    }

    public boolean matches(Transaction tx) {
        if (tx.date().isBefore(start) || tx.date().isAfter(end)) {
            return false;
        }
        if (category.isPresent() && !category.get().equals(tx.category())) {
            return false;
        }
        return merchant.isEmpty()
                || tx.merchant().toLowerCase().contains(merchant.get().toLowerCase());
    }
}
