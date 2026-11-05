package wealthsei.domain;

import java.util.Objects;

/**
 * A place money is held, such as a chequing account or a credit card (Stage 1 domain class).
 *
 * @param id             stable identifier
 * @param name           display name, e.g. "Chequing" or "Neo Mastercard"
 * @param openingBalance balance when tracking started; for a credit card this is normally zero
 */
public record Account(String id, String name, Money openingBalance) {

    public Account {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(openingBalance, "openingBalance");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Account id must not be blank");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("Account name must not be blank");
        }
    }

    /** The balance after applying the given transactions to the opening balance. */
    public Money balanceAfter(java.util.List<Transaction> transactions) {
        Money balance = openingBalance;
        for (Transaction tx : transactions) {
            balance = balance.add(tx.signedAmount());
        }
        return balance;
    }
}
