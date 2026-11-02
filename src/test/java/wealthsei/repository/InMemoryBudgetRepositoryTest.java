package wealthsei.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Budget;
import wealthsei.domain.BudgetLine;
import wealthsei.domain.Categories;
import wealthsei.domain.Money;

class InMemoryBudgetRepositoryTest {

    private static final YearMonth OCT = YearMonth.of(2026, 10);

    private static Budget budget(String limit) {
        return new Budget(OCT, List.of(new BudgetLine(Categories.GROCERIES, Money.of(limit))));
    }

    @Test
    void savesAndFindsByMonth() {
        InMemoryBudgetRepository repo = new InMemoryBudgetRepository();
        assertTrue(repo.find(OCT).isEmpty());
        repo.save(budget("300"));
        assertEquals(Money.of("300"), repo.find(OCT).orElseThrow().totalLimit());
        assertTrue(repo.find(YearMonth.of(2026, 11)).isEmpty());
    }

    @Test
    void savingAgainReplacesTheMonthsBudget() {
        InMemoryBudgetRepository repo = new InMemoryBudgetRepository();
        repo.save(budget("300"));
        repo.save(budget("450"));
        assertEquals(Money.of("450"), repo.find(OCT).orElseThrow().totalLimit());
    }
}
