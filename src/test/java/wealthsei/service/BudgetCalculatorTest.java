package wealthsei.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Budget;
import wealthsei.domain.BudgetLine;
import wealthsei.domain.BudgetStatus;
import wealthsei.domain.Categories;
import wealthsei.domain.Category;
import wealthsei.domain.Money;
import wealthsei.domain.Transaction;

class BudgetCalculatorTest {

    private static final YearMonth OCT = YearMonth.of(2026, 10);
    private final Budget budget = new Budget(OCT, List.of(
            new BudgetLine(Categories.GROCERIES, Money.of("300")),
            new BudgetLine(Categories.DINING, Money.of("100"))));

    private static Transaction tx(String id, String date, String amount, Category c, boolean income) {
        return new Transaction(id, LocalDate.parse(date), Money.of(amount), "M", "M", income, c);
    }

    @Test
    void sumsExpensesPerCategoryForTheMonth() {
        BudgetStatus s = BudgetCalculator.compute(budget, List.of(
                tx("1", "2026-10-01", "50.25", Categories.GROCERIES, false),
                tx("2", "2026-10-31", "100", Categories.GROCERIES, false),
                tx("3", "2026-10-15", "20", Categories.DINING, false)));
        assertEquals(Money.of("150.25"), s.lineFor(Categories.GROCERIES).orElseThrow().spent());
        assertEquals(Money.of("20"), s.lineFor(Categories.DINING).orElseThrow().spent());
        assertEquals(Money.of("170.25"), s.totalSpent());
        assertEquals(Money.of("400"), s.totalLimit());
    }

    @Test
    void ignoresOtherMonthsIncomeAndUnbudgetedCategories() {
        BudgetStatus s = BudgetCalculator.compute(budget, List.of(
                tx("1", "2026-09-30", "999", Categories.GROCERIES, false),
                tx("2", "2026-11-01", "999", Categories.GROCERIES, false),
                tx("3", "2026-10-05", "999", Categories.GROCERIES, true),
                tx("4", "2026-10-05", "999", Categories.SHOPPING, false),
                tx("5", "2026-10-05", "999", Category.UNCATEGORIZED, false)));
        assertEquals(Money.ZERO, s.totalSpent());
        assertEquals(2, s.lines().size());
    }

    @Test
    void noTransactionsMeansZeroSpentEverywhere() {
        BudgetStatus s = BudgetCalculator.compute(budget, List.of());
        assertEquals(Money.ZERO, s.totalSpent());
        assertEquals(Money.of("400"), s.totalLimit());
    }

    @Test
    void keepsTheBudgetsLineOrder() {
        BudgetStatus s = BudgetCalculator.compute(budget, List.of());
        assertEquals(Categories.GROCERIES, s.lines().get(0).category());
        assertEquals(Categories.DINING, s.lines().get(1).category());
    }
}
