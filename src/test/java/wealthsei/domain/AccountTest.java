package wealthsei.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccountTest {

    private static Transaction tx(String amount, boolean income) {
        return new Transaction("t", LocalDate.of(2026, 10, 1), Money.of(amount), "M", "M", income, Category.UNCATEGORIZED);
    }

    @Test
    void balanceAppliesIncomeAndSpendingToTheOpeningBalance() {
        Account a = new Account("a1", "Chequing", Money.of("100.00"));
        assertEquals(Money.of("115.00"), a.balanceAfter(List.of(tx("40.00", true), tx("25.00", false))));
    }

    @Test
    void anAccountWithNoTransactionsKeepsItsOpeningBalance() {
        assertEquals(Money.of("50.00"), new Account("a1", "Card", Money.of("50.00")).balanceAfter(List.of()));
    }

    @Test
    void blankIdOrNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Account(" ", "x", Money.of("0")));
        assertThrows(IllegalArgumentException.class, () -> new Account("a", " ", Money.of("0")));
    }
}
