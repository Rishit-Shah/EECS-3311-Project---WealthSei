package wealthsei.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TransactionTest {

    private static final Category GROCERIES = new Category("groceries", "Groceries", true);

    private Transaction expense(String amount) {
        return new Transaction("t1", LocalDate.of(2026, 10, 5), Money.of(amount),
                "Loblaws", "LOBLAWS #1234 TORONTO", false, GROCERIES);
    }

    @Test
    void signedAmountIsNegativeForExpenseAndPositiveForIncome() {
        assertEquals(Money.of("-42.10"), expense("42.10").signedAmount());
        Transaction pay = new Transaction("t2", LocalDate.of(2026, 10, 1), Money.of("2000"),
                "Employer", "PAYROLL", true, Category.UNCATEGORIZED);
        assertEquals(Money.of("2000"), pay.signedAmount());
        assertTrue(expense("1").isExpense());
        assertFalse(pay.isExpense());
    }

    @Test
    void withCategoryReturnsNewInstanceAndKeepsOriginal() {
        Transaction original = expense("10").withCategory(Category.UNCATEGORIZED);
        Category dining = new Category("dining", "Dining", false);
        Transaction changed = original.withCategory(dining);
        assertEquals(Category.UNCATEGORIZED, original.category());
        assertEquals(dining, changed.category());
        assertEquals(original.id(), changed.id());
        assertEquals(original.amount(), changed.amount());
    }

    @Test
    void rejectsNonPositiveAmountAndBlankId() {
        assertThrows(IllegalArgumentException.class, () -> expense("0"));
        assertThrows(IllegalArgumentException.class, () -> expense("-5"));
        assertThrows(IllegalArgumentException.class, () -> new Transaction(" ",
                LocalDate.of(2026, 1, 1), Money.of("1"), "m", "d", false, GROCERIES));
    }

    @Test
    void rejectsNulls() {
        assertThrows(NullPointerException.class, () -> new Transaction("t", null,
                Money.of("1"), "m", "d", false, GROCERIES));
        assertThrows(NullPointerException.class, () -> new Transaction("t",
                LocalDate.of(2026, 1, 1), Money.of("1"), "m", "d", false, null));
    }
    
}
