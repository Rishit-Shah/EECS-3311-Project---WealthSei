package wealthsei.command;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Categories;
import wealthsei.domain.Category;
import wealthsei.domain.Transaction;
import wealthsei.domain.TxFilter;
import wealthsei.importing.CsvImporter;
import wealthsei.repository.InMemoryLearnedRuleRepository;
import wealthsei.repository.InMemoryTransactionRepository;
import wealthsei.service.Categorizer;
import wealthsei.service.KeywordRuleStrategy;
import wealthsei.service.LearnedRuleStrategy;
import wealthsei.service.TransactionService;
import wealthsei.service.ValidationException;

class CategoryCorrectorTest {

    private final AtomicInteger ids = new AtomicInteger();
    private final InMemoryLearnedRuleRepository rules = new InMemoryLearnedRuleRepository();
    private final TransactionService service = new TransactionService(
            new InMemoryTransactionRepository(),
            new CsvImporter(() -> "t" + ids.incrementAndGet()),
            new Categorizer(List.of(new LearnedRuleStrategy(rules), KeywordRuleStrategy.withDefaults())));
    private final CategoryCorrector corrector = new CategoryCorrector(service, rules);

    private void importCsv(String body) throws IOException {
        Path p = Files.createTempFile("wealthsei-corr", ".csv");
        Files.writeString(p, "Date,Description,Amount\n" + body);
        p.toFile().deleteOnExit();
        service.importFile(p);
    }

    private Transaction first() {
        return service.query(TxFilter.all()).get(0);
    }

    @Test
    void correctChangesTheCategoryAndLearnsTheMerchant() throws IOException {
        importCsv("2026-10-02,ZXQV WIDGETS,-5\n");
        assertEquals(Category.UNCATEGORIZED, first().category());
        Transaction changed = corrector.correct(first().id(), Categories.SHOPPING);
        assertEquals(Categories.SHOPPING, changed.category());
        assertEquals(Categories.SHOPPING, first().category());
        assertEquals(Categories.SHOPPING, rules.find("zxqv widgets").orElseThrow());
    }

    @Test
    void undoRestoresTheOldCategoryAndForgetsTheRule() throws IOException {
        importCsv("2026-10-02,ZXQV WIDGETS,-5\n");
        corrector.correct(first().id(), Categories.SHOPPING);
        Transaction restored = corrector.undo().orElseThrow();
        assertEquals(Category.UNCATEGORIZED, restored.category());
        assertEquals(Category.UNCATEGORIZED, first().category());
        assertTrue(rules.find("ZXQV WIDGETS").isEmpty());
    }

    @Test
    void undoPutsBackAnEarlierLearnedRule() throws IOException {
        importCsv("2026-10-02,ZXQV WIDGETS,-5\n");
        String id = first().id();
        corrector.correct(id, Categories.SHOPPING);
        corrector.correct(id, Categories.HEALTH);
        assertEquals(Categories.HEALTH, rules.find("ZXQV WIDGETS").orElseThrow());
        corrector.undo();
        assertEquals(Categories.SHOPPING, first().category());
        assertEquals(Categories.SHOPPING, rules.find("ZXQV WIDGETS").orElseThrow());
    }

    @Test
    void redoAppliesTheCorrectionAgain() throws IOException {
        importCsv("2026-10-02,ZXQV WIDGETS,-5\n");
        corrector.correct(first().id(), Categories.SHOPPING);
        corrector.undo();
        assertTrue(corrector.canRedo());
        Transaction again = corrector.redo().orElseThrow();
        assertEquals(Categories.SHOPPING, again.category());
        assertEquals(Categories.SHOPPING, rules.find("ZXQV WIDGETS").orElseThrow());
    }

    @Test
    void laterImportsUseTheLearnedRule() throws IOException {
        importCsv("2026-10-02,ZXQV WIDGETS,-5\n");
        corrector.correct(first().id(), Categories.ENTERTAINMENT);
        importCsv("2026-10-09,Zxqv Widgets,-8\n");
        List<Transaction> all = service.query(TxFilter.all());
        assertEquals(2, all.size());
        assertTrue(all.stream().allMatch(t -> t.category().equals(Categories.ENTERTAINMENT)));
    }

    @Test
    void unknownIdThrowsAndLeavesNothingToUndo() {
        assertThrows(ValidationException.class, () -> corrector.correct("nope", Categories.SHOPPING));
        assertFalse(corrector.canUndo());
        assertTrue(corrector.undo().isEmpty());
    }

    @Test
    void incomeAndUncategorizedCorrectionsAreNotLearned() throws IOException {
        importCsv("2026-10-01,ACME PAYROLL,2000\n2026-10-02,ZXQV WIDGETS,-5\n");
        Transaction income = service.query(TxFilter.all()).stream().filter(Transaction::income).findFirst().orElseThrow();
        Transaction expense = service.query(TxFilter.all()).stream().filter(t -> !t.income()).findFirst().orElseThrow();
        corrector.correct(income.id(), Categories.SHOPPING);
        corrector.correct(expense.id(), Category.UNCATEGORIZED);
        assertTrue(rules.all().isEmpty());
    }
}
