package wealthsei.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * One money movement. The amount is always a positive magnitude; {@code income} says which
 * way the money went. Immutable: re-categorising produces a new Transaction via
 * {@link #withCategory(Category)}.
 *
 * @param id          unique id (assigned on import / creation)
 * @param date        the day the money moved
 * @param amount      positive magnitude
 * @param merchant    cleaned merchant name, e.g. {@code "Loblaws"}
 * @param description raw bank description
 * @param income      true if money came in, false if it went out
 * @param category    never null; {@link Category#UNCATEGORIZED} when unknown
 */
public record Transaction(String id, LocalDate date, Money amount, String merchant,
                          String description, boolean income, Category category) {

    public Transaction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(merchant, "merchant");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(category, "category");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Transaction id must not be blank");
        }
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Transaction amount must be positive, was " + amount);
        }
    }

    public Transaction withCategory(Category newCategory) {
        return new Transaction(id, date, amount, merchant, description, income, newCategory);
    }

    /** Positive for income, negative for spending; handy for balance maths. */
    public Money signedAmount() {
        return income ? amount : amount.negate();
    }

    public boolean isExpense() {
        return !income;
    }
}
