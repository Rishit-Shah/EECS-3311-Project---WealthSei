package wealthsei.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable amount of money in a single currency (CAD).
 *
 * <p>Wraps a {@link BigDecimal} so no floating-point error can creep in. Every value is
 * normalised to exactly two decimal places (banker's rounding), so {@code equals} and
 * {@code hashCode} behave the way people expect: {@code Money.of("5")} equals
 * {@code Money.of("5.00")}.
 */
public final class Money implements Comparable<Money> {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    // Must stay below SCALE/ROUNDING: static initialisers run in textual order.
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount.setScale(SCALE, ROUNDING);
    }

    public static Money of(BigDecimal amount) {
        return new Money(Objects.requireNonNull(amount, "amount"));
    }

    public static Money of(String amount) {
        Objects.requireNonNull(amount, "amount");
        try {
            return new Money(new BigDecimal(amount.trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a valid amount: '" + amount + "'", e);
        }
    }

    public static Money ofCents(long cents) {
        return new Money(BigDecimal.valueOf(cents, SCALE));
    }

    public BigDecimal amount() {
        return amount;
    }

    public Money add(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    public Money multiply(BigDecimal factor) {
        return new Money(amount.multiply(Objects.requireNonNull(factor, "factor")));
    }

    public Money multiply(long factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)));
    }

    /** Divides and rounds to cents; used e.g. to spread a goal over N months. */
    public Money divide(long divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Cannot divide money by zero");
        }
        return new Money(amount.divide(BigDecimal.valueOf(divisor), SCALE, ROUNDING));
    }

    public Money negate() {
        return new Money(amount.negate());
    }

    public Money abs() {
        return new Money(amount.abs());
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    public boolean isLessThan(Money other) {
        return compareTo(other) < 0;
    }

    public static Money max(Money a, Money b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static Money min(Money a, Money b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Money m && amount.equals(m.amount);
    }

    @Override
    public int hashCode() {
        return amount.hashCode();
    }

    /** Plain decimal text such as {@code -12.30}; no currency symbol. */
    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
