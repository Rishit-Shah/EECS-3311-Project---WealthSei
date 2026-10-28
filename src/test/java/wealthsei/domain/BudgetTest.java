package wealthsei.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BudgetTest {

    private static final YearMonth OCT = YearMonth.of(2026, 10);
    private static final Category FOOD = new Category("groceries", "Groceries", true);
    private static final Category FUN = new Category("fun", "Fun", false);

    @Test
    void lineRequiresPositiveLimit() {
        assertThrows(IllegalArgumentException.class, () -> new BudgetLine(FOOD, Money.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new BudgetLine(FOOD, Money.of("-1")));
        assertEquals(Money.of("300"), new BudgetLine(FOOD, Money.of("300")).limit());
    }

    @Test
    void budgetNeedsAtLeastOneLine() {
        assertThrows(IllegalArgumentException.class, () -> new Budget(OCT, List.of()));
    }

    @Test
    void budgetRejectsDuplicateCategories() {
        assertThrows(IllegalArgumentException.class, () -> new Budget(OCT,
                List.of(new BudgetLine(FOOD, Money.of("1")), new BudgetLine(FOOD, Money.of("2")))));
    }

    @Test
    void limitForAndTotal() {
        Budget b = new Budget(OCT, List.of(new BudgetLine(FOOD, Money.of("300")), new BudgetLine(FUN, Money.of("100.50"))));
        assertEquals(Money.of("300"), b.limitFor(FOOD).orElseThrow());
        assertTrue(b.limitFor(new Category("rent", "Rent", true)).isEmpty());
        assertEquals(Money.of("400.50"), b.totalLimit());
    }

    @Test
    void fromLimitsKeepsEntryOrder() {
        Map<Category, Money> entered = new LinkedHashMap<>();
        entered.put(FUN, Money.of("50"));
        entered.put(FOOD, Money.of("200"));
        Budget b = Budget.from(OCT, new BudgetLimits(entered));
        assertEquals(OCT, b.month());
        assertEquals(FUN, b.lines().get(0).category());
        assertEquals(FOOD, b.lines().get(1).category());
    }

    @Test
    void fromLimitsRejectsBadAmountAndEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> Budget.from(OCT, new BudgetLimits(Map.of(FOOD, Money.ZERO))));
        assertThrows(IllegalArgumentException.class, () -> Budget.from(OCT, new BudgetLimits(Map.of())));
    }

    @Test
    void budgetLimitsIsDefensivelyCopied() {
        Map<Category, Money> m = new LinkedHashMap<>();
        m.put(FOOD, Money.of("1"));
        BudgetLimits limits = new BudgetLimits(m);
        m.put(FUN, Money.of("2"));
        assertEquals(1, limits.limits().size());
        assertThrows(UnsupportedOperationException.class, () -> limits.limits().put(FUN, Money.of("2")));
    }
}
