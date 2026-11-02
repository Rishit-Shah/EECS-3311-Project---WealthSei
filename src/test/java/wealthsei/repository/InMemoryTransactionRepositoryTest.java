package wealthsei.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Categories;
import wealthsei.domain.Category;
import wealthsei.domain.Money;
import wealthsei.domain.Transaction;
import wealthsei.domain.TxFilter;

class InMemoryTransactionRepositoryTest {

    private static Transaction tx(String id, String date, String merchant) {
        return new Transaction(id, LocalDate.parse(date), Money.of("10"), merchant, merchant, false,
                Category.UNCATEGORIZED);
    }

    @Test
    void savesAndFindsById() {
        InMemoryTransactionRepository repo = new InMemoryTransactionRepository();
        repo.saveAll(List.of(tx("a", "2026-10-01", "X")));
        assertEquals("X", repo.findById("a").orElseThrow().merchant());
        assertTrue(repo.findById("zzz").isEmpty());
    }

    @Test
    void queryReturnsOldestFirstAndKeepsSaveOrderWithinADay() {
        InMemoryTransactionRepository repo = new InMemoryTransactionRepository();
        repo.saveAll(List.of(tx("late", "2026-10-09", "L"), tx("early1", "2026-10-01", "E1"),
                tx("early2", "2026-10-01", "E2")));
        List<Transaction> all = repo.query(TxFilter.all());
        assertEquals("early1", all.get(0).id());
        assertEquals("early2", all.get(1).id());
        assertEquals("late", all.get(2).id());
    }

    @Test
    void queryAppliesTheFilter() {
        InMemoryTransactionRepository repo = new InMemoryTransactionRepository();
        repo.saveAll(List.of(tx("a", "2026-10-01", "Tim Hortons"), tx("b", "2026-11-01", "Tim Hortons")));
        assertEquals(1, repo.query(TxFilter.between(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"))).size());
        assertEquals(2, repo.query(TxFilter.all().withMerchant("tim")).size());
        assertEquals(0, repo.query(TxFilter.all().withMerchant("starbucks")).size());
    }

    @Test
    void updatesCategory() {
        InMemoryTransactionRepository repo = new InMemoryTransactionRepository();
        repo.saveAll(List.of(tx("a", "2026-10-01", "X")));
        repo.updateCategory("a", Categories.DINING);
        assertEquals(Categories.DINING, repo.findById("a").orElseThrow().category());
    }

    @Test
    void unknownIdOnUpdateIsAnError() {
        InMemoryTransactionRepository repo = new InMemoryTransactionRepository();
        assertThrows(IllegalArgumentException.class, () -> repo.updateCategory("nope", Categories.DINING));
    }

    @Test
    void duplicateIdSavesNothing() {
        InMemoryTransactionRepository repo = new InMemoryTransactionRepository();
        repo.saveAll(List.of(tx("a", "2026-10-01", "X")));
        assertThrows(IllegalArgumentException.class,
                () -> repo.saveAll(List.of(tx("b", "2026-10-02", "Y"), tx("a", "2026-10-03", "Z"))));
        assertTrue(repo.findById("b").isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> repo.saveAll(List.of(tx("c", "2026-10-02", "Y"), tx("c", "2026-10-03", "Z"))));
    }
}
