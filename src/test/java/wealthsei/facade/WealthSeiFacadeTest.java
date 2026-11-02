package wealthsei.facade;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Alert;
import wealthsei.domain.AlertSeverity;
import wealthsei.domain.BudgetLimits;
import wealthsei.domain.BudgetStatus;
import wealthsei.domain.Categories;
import wealthsei.domain.Category;
import wealthsei.domain.ImportResult;
import wealthsei.domain.Money;
import wealthsei.domain.Transaction;
import wealthsei.domain.TxFilter;
import wealthsei.importing.CsvImporter;
import wealthsei.repository.InMemoryBudgetRepository;
import wealthsei.repository.InMemoryPlanRepository;
import wealthsei.repository.InMemoryTransactionRepository;
import wealthsei.service.AlertMonitor;
import wealthsei.service.BudgetService;
import wealthsei.service.Categorizer;
import wealthsei.service.KeywordRuleStrategy;
import wealthsei.service.PlanService;
import wealthsei.service.TransactionImportException;
import wealthsei.service.TransactionService;
import wealthsei.service.ValidationException;

class WealthSeiFacadeTest {

    private static final YearMonth OCT = YearMonth.of(2026, 10);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 2);

    private final AtomicInteger ids = new AtomicInteger();
    private final List<Alert> alerts = new ArrayList<>();
    private final WealthSeiFacade facade;

    WealthSeiFacadeTest() {
        InMemoryTransactionRepository txRepo = new InMemoryTransactionRepository();
        AlertMonitor monitor = new AlertMonitor(() -> Instant.parse("2026-11-02T12:00:00Z"));
        TransactionService transactions = new TransactionService(txRepo,
                new CsvImporter(() -> "t" + ids.incrementAndGet()),
                new Categorizer(List.of(KeywordRuleStrategy.withDefaults())));
        BudgetService budgets = new BudgetService(new InMemoryBudgetRepository(), txRepo, monitor);
        PlanService plans = new PlanService(new InMemoryPlanRepository(), () -> TODAY);
        facade = new WealthSeiFacade(transactions, budgets, plans, monitor);
        facade.addAlertListener(alerts::add);
    }

    private static Path csv(String content) throws IOException {
        Path p = Files.createTempFile("wealthsei-test", ".csv");
        Files.writeString(p, content);
        p.toFile().deleteOnExit();
        return p;
    }

    private static BudgetLimits limits(Category c, String amount) {
        Map<Category, Money> m = new LinkedHashMap<>();
        m.put(c, Money.of(amount));
        return new BudgetLimits(m);
    }

    @Test
    void importReportsAffectedMonths() throws IOException {
        ImportResult r = facade.importTransactions(csv("Date,Description,Amount\n"
                + "2026-10-02,LOBLAWS,-10\n2026-11-01,LOBLAWS,-10\n"));
        assertEquals(2, r.imported());
        assertEquals(java.util.Set.of(OCT, YearMonth.of(2026, 11)), r.affectedMonths());
    }

    @Test
    void importRefreshesBudgetAlerts() throws IOException {
        facade.setBudget(OCT, limits(Categories.GROCERIES, "100"));
        assertTrue(alerts.isEmpty());
        facade.importTransactions(csv("Date,Description,Amount\n2026-10-02,LOBLAWS,-90\n"));
        assertEquals(1, alerts.size());
        assertEquals(AlertSeverity.WARNING, alerts.get(0).severity());
        BudgetStatus status = facade.getBudgetStatus(OCT);
        assertEquals(Money.of("90"), status.totalSpent());
    }

    @Test
    void reimportingDoesNotRepeatTheAlert() throws IOException {
        facade.setBudget(OCT, limits(Categories.GROCERIES, "100"));
        String file = "Date,Description,Amount\n2026-10-02,LOBLAWS,-90\n";
        facade.importTransactions(csv(file));
        facade.importTransactions(csv(file));
        assertEquals(1, alerts.size());
    }

    @Test
    void correctingACategoryMovesSpendingBetweenBudgetLines() throws IOException {
        facade.setBudget(OCT, limits(Categories.GROCERIES, "100"));
        facade.importTransactions(csv("Date,Description,Amount\n2026-10-02,ZXQV WIDGETS,-95\n"));
        assertTrue(alerts.isEmpty());
        String id = facade.listTransactions(TxFilter.all()).get(0).id();
        Transaction changed = facade.correctCategory(id, Categories.GROCERIES);
        assertEquals(Categories.GROCERIES, changed.category());
        assertEquals(Money.of("95"), facade.getBudgetStatus(OCT).totalSpent());
        assertEquals(1, alerts.size());
    }

    @Test
    void unsupportedFilePropagatesAsTransactionImportException() throws IOException {
        Path p = csv("Foo,Bar\n1,2\n");
        assertThrows(TransactionImportException.class, () -> facade.importTransactions(p));
    }

    @Test
    void planMethodsGoThroughToThePlanService() {
        assertTrue(facade.getPlan().isEmpty());
        facade.setBalance(Money.of("800"), TODAY);
        facade.setSafetyBuffer(Money.of("100"));
        assertEquals(Money.of("100"), facade.getPlan().orElseThrow().safetyBuffer());
        assertThrows(ValidationException.class, () -> facade.setBalance(Money.of("1"), TODAY.plusDays(3)));
    }

    @Test
    void setBudgetLimitUpdatesOneCategory() {
        facade.setBudget(OCT, limits(Categories.GROCERIES, "100"));
        BudgetStatus s = facade.setBudgetLimit(OCT, Categories.DINING, Money.of("50"));
        assertEquals(2, s.lines().size());
    }
}
