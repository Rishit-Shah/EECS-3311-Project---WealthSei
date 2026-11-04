package wealthsei.repository.file;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Budget;
import wealthsei.domain.BudgetLine;
import wealthsei.domain.Categories;
import wealthsei.domain.Category;
import wealthsei.domain.FinancialPlan;
import wealthsei.domain.FixedBill;
import wealthsei.domain.Frequency;
import wealthsei.domain.IncomeSource;
import wealthsei.domain.Money;
import wealthsei.domain.PlannedExpense;
import wealthsei.domain.Transaction;
import wealthsei.domain.TxFilter;
import wealthsei.repository.InMemoryBudgetRepository;
import wealthsei.repository.InMemoryLearnedRuleRepository;
import wealthsei.repository.InMemoryPlanRepository;
import wealthsei.repository.InMemoryTransactionRepository;

class PersistentRepositoriesTest {

    private static final LocalDate D = LocalDate.of(2026, 10, 5);

    private static Path tempDir() throws IOException {
        Path dir = Files.createTempDirectory("wealthsei-repo");
        dir.toFile().deleteOnExit();
        return dir;
    }

    private static Transaction tx(String id, String merchant, String desc, Category c) {
        return new Transaction(id, D, Money.of("12.30"), merchant, desc, false, c);
    }

    // ---- transactions ----

    @Test
    void transactionsSurviveARestart() throws IOException {
        Path file = tempDir().resolve("t.tsv");
        PersistentTransactionRepository a = new PersistentTransactionRepository(new InMemoryTransactionRepository(), file);
        a.saveAll(List.of(tx("1", "Loblaws", "LOBLAWS #12", Categories.GROCERIES),
                new Transaction("2", D, Money.of("2400"), "Payroll", "", true, Categories.INCOME)));
        PersistentTransactionRepository b = new PersistentTransactionRepository(new InMemoryTransactionRepository(), file);
        assertEquals(a.query(TxFilter.all()), b.query(TxFilter.all()));
        assertEquals(2, b.query(TxFilter.all()).size());
        assertTrue(b.findById("2").orElseThrow().income());
    }

    @Test
    void categoryChangesAreSaved() throws IOException {
        Path file = tempDir().resolve("t.tsv");
        PersistentTransactionRepository a = new PersistentTransactionRepository(new InMemoryTransactionRepository(), file);
        a.saveAll(List.of(tx("1", "Zxqv", "", Category.UNCATEGORIZED)));
        a.updateCategory("1", Categories.SHOPPING);
        PersistentTransactionRepository b = new PersistentTransactionRepository(new InMemoryTransactionRepository(), file);
        assertEquals(Categories.SHOPPING, b.findById("1").orElseThrow().category());
    }

    @Test
    void tabsAndNewlinesInTextAreKept() throws IOException {
        Path file = tempDir().resolve("t.tsv");
        PersistentTransactionRepository a = new PersistentTransactionRepository(new InMemoryTransactionRepository(), file);
        a.saveAll(List.of(tx("1", "A\tB", "two\nlines \\ café", Categories.DINING)));
        Transaction back = new PersistentTransactionRepository(new InMemoryTransactionRepository(), file)
                .findById("1").orElseThrow();
        assertEquals("A\tB", back.merchant());
        assertEquals("two\nlines \\ café", back.description());
    }

    @Test
    void aRejectedSaveLeavesTheFileUntouched() throws IOException {
        Path file = tempDir().resolve("t.tsv");
        PersistentTransactionRepository a = new PersistentTransactionRepository(new InMemoryTransactionRepository(), file);
        a.saveAll(List.of(tx("1", "One", "", Categories.DINING)));
        String before = Files.readString(file);
        assertThrows(IllegalArgumentException.class, () -> a.saveAll(List.of(tx("1", "Dup", "", Categories.DINING))));
        assertEquals(before, Files.readString(file));
    }

    @Test
    void aBrokenTransactionFileNamesTheLine() throws IOException {
        Path file = tempDir().resolve("t.tsv");
        Files.writeString(file, "1\t2026-10-05\t12.30\tA\t\tfalse\tdining\tDining out\ttrue\n"
                + "2\tnot-a-date\t5\tB\t\tfalse\tdining\tDining out\tfalse\n");
        StorageException e = assertThrows(StorageException.class,
                () -> new PersistentTransactionRepository(new InMemoryTransactionRepository(), file));
        assertTrue(e.getMessage().contains("line 2"), e.getMessage());
    }

    @Test
    void duplicateIdsInTheFileAreReported() throws IOException {
        Path file = tempDir().resolve("t.tsv");
        String row = "1\t2026-10-05\t12.30\tA\t\tfalse\tdining\tDining out\tfalse\n";
        Files.writeString(file, row + row);
        assertThrows(StorageException.class,
                () -> new PersistentTransactionRepository(new InMemoryTransactionRepository(), file));
    }

    @Test
    void aShortRowSaysWhichColumnIsMissing() throws IOException {
        Path file = tempDir().resolve("t.tsv");
        Files.writeString(file, "1\t2026-10-05\n");
        StorageException e = assertThrows(StorageException.class,
                () -> new PersistentTransactionRepository(new InMemoryTransactionRepository(), file));
        assertTrue(e.getMessage().contains("missing column"), e.getMessage());
    }

    // ---- budgets ----

    @Test
    void budgetsSurviveARestart() throws IOException {
        Path file = tempDir().resolve("b.tsv");
        PersistentBudgetRepository a = new PersistentBudgetRepository(new InMemoryBudgetRepository(), file);
        Budget oct = new Budget(YearMonth.of(2026, 10), List.of(
                new BudgetLine(Categories.GROCERIES, Money.of("150")), new BudgetLine(Categories.DINING, Money.of("40.50"))));
        Budget nov = new Budget(YearMonth.of(2026, 11), List.of(new BudgetLine(Categories.RENT, Money.of("900"))));
        a.save(oct);
        a.save(nov);
        PersistentBudgetRepository b = new PersistentBudgetRepository(new InMemoryBudgetRepository(), file);
        assertEquals(oct, b.find(YearMonth.of(2026, 10)).orElseThrow());
        assertEquals(List.of(oct, nov), b.findAll());
    }

    @Test
    void savingABudgetAgainReplacesThatMonthOnly() throws IOException {
        Path file = tempDir().resolve("b.tsv");
        PersistentBudgetRepository a = new PersistentBudgetRepository(new InMemoryBudgetRepository(), file);
        a.save(new Budget(YearMonth.of(2026, 10), List.of(new BudgetLine(Categories.GROCERIES, Money.of("150")))));
        Budget changed = new Budget(YearMonth.of(2026, 10), List.of(new BudgetLine(Categories.DINING, Money.of("20"))));
        a.save(changed);
        PersistentBudgetRepository b = new PersistentBudgetRepository(new InMemoryBudgetRepository(), file);
        assertEquals(List.of(changed), b.findAll());
    }

    @Test
    void aBrokenBudgetFileNamesTheLine() throws IOException {
        Path file = tempDir().resolve("b.tsv");
        Files.writeString(file, "2026-13\tgroceries\tGroceries\ttrue\t100\n");
        StorageException e = assertThrows(StorageException.class,
                () -> new PersistentBudgetRepository(new InMemoryBudgetRepository(), file));
        assertTrue(e.getMessage().contains("line 1"));
    }

    // ---- plan ----

    @Test
    void aFullPlanSurvivesARestart() throws IOException {
        Path file = tempDir().resolve("p.tsv");
        PersistentPlanRepository a = new PersistentPlanRepository(new InMemoryPlanRepository(), file);
        FinancialPlan plan = FinancialPlan.starting(Money.of("1500"), D)
                .withSafetyBuffer(Money.of("200"))
                .withIncomeSource(new IncomeSource("Job \"A\"", Money.of("2000"), Frequency.BIWEEKLY, D))
                .withFixedBill(new FixedBill("Rent", Money.of("900"), Frequency.MONTHLY, D, Categories.RENT))
                .withFixedBill(new FixedBill("Mystery", Money.of("10"), Frequency.YEARLY, D, null))
                .withPlannedExpense(new PlannedExpense("Laptop", Money.of("1200"), D.plusDays(30), null))
                .withPlannedExpense(new PlannedExpense("Trip", Money.of("300"), D.plusDays(60), Categories.ENTERTAINMENT));
        a.save(plan);
        PersistentPlanRepository b = new PersistentPlanRepository(new InMemoryPlanRepository(), file);
        assertEquals(plan, b.find().orElseThrow());
    }

    @Test
    void noFileMeansNoPlan() throws IOException {
        assertTrue(new PersistentPlanRepository(new InMemoryPlanRepository(), tempDir().resolve("p.tsv")).find().isEmpty());
    }

    @Test
    void aPlanFileThatDoesNotStartWithTheBalanceIsRejected() throws IOException {
        Path file = tempDir().resolve("p.tsv");
        Files.writeString(file, "buffer\t100\n");
        assertThrows(StorageException.class, () -> new PersistentPlanRepository(new InMemoryPlanRepository(), file));
    }

    @Test
    void anUnknownPlanLineTypeIsRejected() throws IOException {
        Path file = tempDir().resolve("p.tsv");
        Files.writeString(file, "balance\t100\t2026-10-05\nbanana\t1\n");
        StorageException e = assertThrows(StorageException.class,
                () -> new PersistentPlanRepository(new InMemoryPlanRepository(), file));
        assertTrue(e.getMessage().contains("line 2"));
    }

    // ---- learned rules ----

    @Test
    void learnedRulesSurviveARestartAndRemovalIsSaved() throws IOException {
        Path file = tempDir().resolve("r.tsv");
        PersistentLearnedRuleRepository a = new PersistentLearnedRuleRepository(new InMemoryLearnedRuleRepository(), file);
        a.save("Zxqv Widgets", Categories.SHOPPING);
        a.save("Cafe X", Categories.DINING);
        a.remove("cafe x");
        PersistentLearnedRuleRepository b = new PersistentLearnedRuleRepository(new InMemoryLearnedRuleRepository(), file);
        assertEquals(Categories.SHOPPING, b.find("ZXQV WIDGETS").orElseThrow());
        assertTrue(b.find("cafe x").isEmpty());
        assertEquals(1, b.all().size());
    }
}
