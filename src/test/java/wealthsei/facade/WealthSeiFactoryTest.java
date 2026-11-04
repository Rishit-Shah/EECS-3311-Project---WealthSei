package wealthsei.facade;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import wealthsei.domain.BudgetLimits;
import wealthsei.domain.Categories;
import wealthsei.domain.Category;
import wealthsei.domain.Money;
import wealthsei.domain.Transaction;
import wealthsei.domain.TxFilter;
import wealthsei.repository.file.StorageException;

class WealthSeiFactoryTest {

    private static final YearMonth OCT = YearMonth.of(2026, 10);

    private static Path tempDir() throws IOException {
        Path dir = Files.createTempDirectory("wealthsei-app");
        dir.toFile().deleteOnExit();
        return dir;
    }

    private static Path csv(String body) throws IOException {
        Path p = Files.createTempFile("wealthsei-f", ".csv");
        Files.writeString(p, "Date,Description,Amount\n" + body);
        p.toFile().deleteOnExit();
        return p;
    }

    @Test
    void inMemoryFacadeStartsEmpty() {
        WealthSeiFacade f = WealthSeiFactory.inMemory();
        assertTrue(f.listTransactions(TxFilter.all()).isEmpty());
        assertTrue(f.getPlan().isEmpty());
    }

    @Test
    void everythingSurvivesARestart() throws IOException {
        Path dir = tempDir().resolve("data");
        WealthSeiFacade first = WealthSeiFactory.persistent(dir);
        first.importTransactions(csv("2026-10-02,LOBLAWS,-90\n2026-10-03,ZXQV WIDGETS,-9.99\n"));
        first.setBudget(OCT, new BudgetLimits(Map.of(Categories.GROCERIES, Money.of("100"))));
        first.setBalance(Money.of("1500"), LocalDate.of(2026, 11, 1));
        Transaction widgets = first.listTransactions(TxFilter.all()).stream()
                .filter(t -> t.merchant().startsWith("Zxqv")).findFirst().orElseThrow();
        first.correctCategory(widgets.id(), Categories.ENTERTAINMENT);

        WealthSeiFacade second = WealthSeiFactory.persistent(dir);
        List<Transaction> all = second.listTransactions(TxFilter.all());
        assertEquals(2, all.size());
        assertEquals(Money.of("90"), second.getBudgetStatus(OCT).totalSpent());
        assertEquals(Money.of("1500"), second.getPlan().orElseThrow().currentBalance());
        assertEquals(Categories.ENTERTAINMENT, all.stream()
                .filter(t -> t.merchant().startsWith("Zxqv")).findFirst().orElseThrow().category());

        // the learned rule is used for a later import after the restart
        second.importTransactions(csv("2026-10-20,Zxqv Widgets,-4\n"));
        List<Category> categories = second.listTransactions(TxFilter.all().withMerchant("zxqv")).stream()
                .map(Transaction::category).toList();
        assertEquals(List.of(Categories.ENTERTAINMENT, Categories.ENTERTAINMENT), categories);
    }

    @Test
    void reimportingAfterARestartSkipsDuplicates() throws IOException {
        Path dir = tempDir().resolve("data");
        Path file = csv("2026-10-02,LOBLAWS,-90\n");
        WealthSeiFactory.persistent(dir).importTransactions(file);
        assertEquals(0, WealthSeiFactory.persistent(dir).importTransactions(file).imported());
    }

    @Test
    void aCorruptDataFileIsReportedAtStartup() throws IOException {
        Path dir = tempDir();
        Files.writeString(dir.resolve("transactions.tsv"), "garbage\n");
        assertThrows(StorageException.class, () -> WealthSeiFactory.persistent(dir));
    }

    @Test
    void recurringFlagsSurviveARestart() throws IOException {
        Path dir = tempDir().resolve("data");
        LocalDate today = LocalDate.of(2026, 11, 4);
        WealthSeiFacade first = WealthSeiFactory.persistent(dir);
        first.importTransactions(csv("2026-08-05,NETFLIX.COM,-16.49\n2026-09-05,NETFLIX.COM,-16.49\n2026-10-05,NETFLIX.COM,-16.49\n"));
        first.flagRecurring("netflix.com", true, today);
        wealthsei.analytics.RecurringReport report = WealthSeiFactory.persistent(dir).recurringPayments(today);
        assertEquals(1, report.payments().size());
        assertTrue(report.payments().get(0).flaggedForReview());
    }
}
