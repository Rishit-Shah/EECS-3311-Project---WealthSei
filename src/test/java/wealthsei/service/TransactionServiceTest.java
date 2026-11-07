package wealthsei.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Categories;
import wealthsei.domain.Category;
import wealthsei.domain.ImportResult;
import wealthsei.domain.Money;
import wealthsei.domain.Transaction;
import wealthsei.domain.TxFilter;
import wealthsei.importing.CsvImporter;
import wealthsei.repository.InMemoryTransactionRepository;

class TransactionServiceTest {

    private final AtomicInteger ids = new AtomicInteger();
    private final InMemoryTransactionRepository repo = new InMemoryTransactionRepository();
    private final TransactionService service = new TransactionService(repo,
            new CsvImporter(() -> "t" + ids.incrementAndGet()),
            new Categorizer(List.of(KeywordRuleStrategy.withDefaults())));

    private static Path csv(String content) throws IOException {
        Path p = Files.createTempFile("wealthsei-test", ".csv");
        Files.writeString(p, content);
        p.toFile().deleteOnExit();
        return p;
    }

    private static final String BASIC = "Date,Description,Amount\n"
            + "2026-10-01,LOBLAWS #12 TORONTO,-45.10\n"
            + "2026-10-02,TIM HORTONS #5,-3.25\n"
            + "2026-10-03,ZXQV WIDGETS,-9.99\n"
            + "2026-10-04,PAYROLL ACME,2000.00\n";

    @Test
    void importsAndCategorizes() throws IOException {
        ImportResult r = service.importFile(csv(BASIC));
        assertEquals(4, r.imported());
        assertEquals(0, r.duplicates());
        assertTrue(r.rejected().isEmpty());
        List<Transaction> all = service.query(TxFilter.all());
        assertEquals(Categories.GROCERIES, all.get(0).category());
        assertEquals(Categories.DINING, all.get(1).category());
        assertEquals(Category.UNCATEGORIZED, all.get(2).category());
        assertEquals(Categories.INCOME, all.get(3).category());
    }

    @Test
    void importingTheSameFileTwiceAddsNothing() throws IOException {
        service.importFile(csv(BASIC));
        ImportResult second = service.importFile(csv(BASIC));
        assertEquals(0, second.imported());
        assertEquals(4, second.duplicates());
        assertEquals(4, service.query(TxFilter.all()).size());
    }

    @Test
    void overlappingFileOnlyAddsTheNewRows() throws IOException {
        service.importFile(csv(BASIC));
        ImportResult r = service.importFile(csv("Date,Description,Amount\n"
                + "2026-10-04,PAYROLL ACME,2000.00\n"
                + "2026-10-05,NETFLIX,-16.49\n"));
        assertEquals(1, r.imported());
        assertEquals(1, r.duplicates());
    }

    @Test
    void identicalPurchasesInOneFileAreBothKept() throws IOException {
        ImportResult r = service.importFile(csv("Date,Description,Amount\n"
                + "2026-10-01,TIM HORTONS,-3.25\n"
                + "2026-10-01,TIM HORTONS,-3.25\n"));
        assertEquals(2, r.imported());
        assertEquals(0, r.duplicates());
    }

    @Test
    void reimportWithTwoIdenticalPurchasesStillAddsNothing() throws IOException {
        String twice = "Date,Description,Amount\n2026-10-01,TIM HORTONS,-3.25\n2026-10-01,TIM HORTONS,-3.25\n";
        service.importFile(csv(twice));
        ImportResult r = service.importFile(csv(twice));
        assertEquals(0, r.imported());
        assertEquals(2, r.duplicates());
    }

    @Test
    void oneStoredAndTwoInFileMeansOneNew() throws IOException {
        service.importFile(csv("Date,Description,Amount\n2026-10-01,TIM HORTONS,-3.25\n"));
        ImportResult r = service.importFile(csv("Date,Description,Amount\n"
                + "2026-10-01,TIM HORTONS,-3.25\n2026-10-01,TIM HORTONS,-3.25\n"));
        assertEquals(1, r.imported());
        assertEquals(1, r.duplicates());
    }

    @Test
    void incomeAndSpendingWithTheSameAmountAreNotDuplicates() throws IOException {
        ImportResult r = service.importFile(csv("Date,Description,Amount\n"
                + "2026-10-01,ACME,50\n2026-10-01,ACME,-50\n"));
        assertEquals(2, r.imported());
    }

    @Test
    void rejectedRowsAreReportedAndTheRestAreSaved() throws IOException {
        ImportResult r = service.importFile(csv("Date,Description,Amount\n"
                + "bad,Coffee,-3\n2026-10-02,Coffee,-3\n"));
        assertEquals(1, r.imported());
        assertEquals(1, r.rejected().size());
        assertTrue(r.rejected().get(0).contains("invalid date"));
    }

    @Test
    void unsupportedFileThrowsAndSavesNothing() throws IOException {
        Path p = csv("Foo,Bar\n1,2\n");
        TransactionImportException e = assertThrows(TransactionImportException.class, () -> service.importFile(p));
        assertTrue(e.getMessage().contains("Supported layouts"));
        assertTrue(service.query(TxFilter.all()).isEmpty());
    }

    @Test
    void headerOnlyFileImportsNothing() throws IOException {
        ImportResult r = service.importFile(csv("Date,Description,Amount\n"));
        assertEquals(0, r.imported());
        assertEquals(0, r.duplicates());
    }

    @Test
    void updateCategoryChangesAndReturnsTheTransaction() throws IOException {
        service.importFile(csv(BASIC));
        String id = service.query(TxFilter.all().withMerchant("zxqv")).get(0).id();
        Transaction changed = service.updateCategory(id, Categories.SHOPPING);
        assertEquals(Categories.SHOPPING, changed.category());
        assertEquals(Money.of("9.99"), changed.amount());
        assertThrows(IllegalArgumentException.class, () -> service.updateCategory("nope", Categories.SHOPPING));
    }

    @Test
    void addsAHandEnteredTransactionAndCategorizesIt() {
        Transaction tx = service.addTransaction(wealthsei.domain.NewTransaction.expense(
                java.time.LocalDate.of(2026, 10, 5), Money.of("12.50"), "  Loblaws  "));
        assertEquals("Loblaws", tx.merchant());
        assertEquals(Categories.GROCERIES, tx.category());
        assertFalse(tx.income());
        assertEquals(tx, service.findById(tx.id()).orElseThrow());
    }

    @Test
    void aChosenCategoryBeatsTheCategorizer() {
        Transaction tx = service.addTransaction(new wealthsei.domain.NewTransaction(
                java.time.LocalDate.of(2026, 10, 5), Money.of("12.50"), "Loblaws", "gift",
                java.util.Optional.of(Categories.SHOPPING), false));
        assertEquals(Categories.SHOPPING, tx.category());
        assertEquals("gift", tx.description());
    }

    @Test
    void anIncomeEntryIsSavedAsIncome() {
        Transaction tx = service.addTransaction(new wealthsei.domain.NewTransaction(
                java.time.LocalDate.of(2026, 10, 5), Money.of("900"), "Acme", "",
                java.util.Optional.of(Categories.INCOME), true));
        assertTrue(tx.income());
        assertEquals(Money.of("900"), tx.signedAmount());
    }

    @Test
    void rejectsEntriesWithMissingOrBadFields() {
        java.time.LocalDate d = java.time.LocalDate.of(2026, 10, 5);
        assertThrows(ValidationException.class, () -> service.addTransaction(null));
        assertThrows(ValidationException.class, () -> service.addTransaction(
                wealthsei.domain.NewTransaction.expense(null, Money.of("5"), "A")));
        assertThrows(ValidationException.class, () -> service.addTransaction(
                wealthsei.domain.NewTransaction.expense(d, Money.of("0"), "A")));
        assertThrows(ValidationException.class, () -> service.addTransaction(
                wealthsei.domain.NewTransaction.expense(d, Money.of("5"), "   ")));
        assertTrue(service.query(TxFilter.all()).isEmpty());
    }

    @Test
    void handEnteredTransactionsGetDistinctIds() {
        java.time.LocalDate d = java.time.LocalDate.of(2026, 10, 5);
        Transaction a = service.addTransaction(wealthsei.domain.NewTransaction.expense(d, Money.of("5"), "Loblaws"));
        Transaction b = service.addTransaction(wealthsei.domain.NewTransaction.expense(d, Money.of("5"), "Loblaws"));
        assertFalse(a.id().equals(b.id()));
        assertEquals(2, service.query(TxFilter.all()).size());
    }
}
