package wealthsei.service;

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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Alert;
import wealthsei.domain.AlertSeverity;
import wealthsei.domain.BudgetLimits;
import wealthsei.domain.BudgetStatus;
import wealthsei.domain.Categories;
import wealthsei.domain.Money;
import wealthsei.domain.Transaction;
import wealthsei.importing.CsvImporter;
import wealthsei.repository.InMemoryBudgetRepository;
import wealthsei.repository.InMemoryTransactionRepository;

class BudgetServiceTest {

    private static final YearMonth OCT = YearMonth.of(2026, 10);

    private final InMemoryTransactionRepository txRepo = new InMemoryTransactionRepository();
    private final AlertMonitor monitor = new AlertMonitor(() -> Instant.parse("2026-10-27T12:00:00Z"));
    private final BudgetService service = new BudgetService(new InMemoryBudgetRepository(), txRepo, monitor);
    private final List<Alert> alerts = new ArrayList<>();

    private int nextId = 1;

    private void spend(String date, String amount) {
        txRepo.saveAll(List.of(new Transaction("t" + nextId++, LocalDate.parse(date), Money.of(amount),
                "Loblaws", "LOBLAWS", false, Categories.GROCERIES)));
    }

    private static BudgetLimits limits(Object... pairs) {
        Map<wealthsei.domain.Category, Money> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((wealthsei.domain.Category) pairs[i], Money.of((String) pairs[i + 1]));
        }
        return new BudgetLimits(m);
    }

    @Test
    void setBudgetReturnsTheStatusFromExistingTransactions() {
        spend("2026-10-03", "120");
        BudgetStatus s = service.setBudget(OCT, limits(Categories.GROCERIES, "300", Categories.DINING, "100"));
        assertEquals(Money.of("120"), s.lineFor(Categories.GROCERIES).orElseThrow().spent());
        assertEquals(Money.of("400"), s.totalLimit());
        assertTrue(service.hasBudget(OCT));
    }

    @Test
    void setBudgetRaisesAlertsForCategoriesAlreadyNearTheLimit() {
        monitor.attach(alerts::add);
        spend("2026-10-03", "270");
        service.setBudget(OCT, limits(Categories.GROCERIES, "300"));
        assertEquals(1, alerts.size());
        assertEquals(AlertSeverity.WARNING, alerts.get(0).severity());
    }

    @Test
    void setBudgetRejectsEmptyAndNonPositiveLimits() {
        assertThrows(ValidationException.class, () -> service.setBudget(OCT, limits()));
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.setBudget(OCT, limits(Categories.GROCERIES, "0")));
        assertTrue(e.getMessage().contains("Groceries"));
        assertThrows(ValidationException.class,
                () -> service.setBudget(OCT, limits(Categories.GROCERIES, "-5")));
        assertFalse(service.hasBudget(OCT));
    }

    @Test
    void settingTheBudgetAgainReplacesTheOldOne() {
        service.setBudget(OCT, limits(Categories.GROCERIES, "300", Categories.DINING, "100"));
        BudgetStatus s = service.setBudget(OCT, limits(Categories.GROCERIES, "500"));
        assertEquals(1, s.lines().size());
        assertEquals(Money.of("500"), s.totalLimit());
    }

    @Test
    void setLimitChangesOneCategoryAndKeepsTheRest() {
        service.setBudget(OCT, limits(Categories.GROCERIES, "300", Categories.DINING, "100"));
        BudgetStatus s = service.setLimit(OCT, Categories.GROCERIES, Money.of("350"));
        assertEquals(Money.of("350"), s.lineFor(Categories.GROCERIES).orElseThrow().limit());
        assertEquals(Money.of("100"), s.lineFor(Categories.DINING).orElseThrow().limit());
        assertEquals(Categories.GROCERIES, s.lines().get(0).category());
    }

    @Test
    void setLimitAddsANewCategoryOrCreatesTheBudget() {
        BudgetStatus created = service.setLimit(OCT, Categories.GROCERIES, Money.of("300"));
        assertEquals(1, created.lines().size());
        BudgetStatus added = service.setLimit(OCT, Categories.DINING, Money.of("80"));
        assertEquals(2, added.lines().size());
    }

    @Test
    void setLimitRejectsNonPositive() {
        assertThrows(ValidationException.class, () -> service.setLimit(OCT, Categories.GROCERIES, Money.ZERO));
        assertThrows(ValidationException.class, () -> service.setLimit(OCT, Categories.GROCERIES, null));
    }

    @Test
    void getStatusWithoutABudgetIsAnError() {
        ValidationException e = assertThrows(ValidationException.class, () -> service.getStatus(OCT));
        assertTrue(e.getMessage().contains("2026-10"));
    }

    @Test
    void statusFollowsNewSpending() {
        service.setBudget(OCT, limits(Categories.GROCERIES, "300"));
        assertEquals(Money.ZERO, service.getStatus(OCT).totalSpent());
        spend("2026-10-10", "75");
        assertEquals(Money.of("75"), service.getStatus(OCT).totalSpent());
    }

    @Test
    void refreshAlertsChecksEachGivenMonthThatHasABudget() {
        monitor.attach(alerts::add);
        service.setBudget(OCT, limits(Categories.GROCERIES, "300"));
        assertTrue(alerts.isEmpty());
        spend("2026-10-10", "290");
        service.refreshAlerts(Set.of(OCT, YearMonth.of(2026, 12)));
        assertEquals(1, alerts.size());
        service.refreshAlerts(Set.of(OCT));
        assertEquals(1, alerts.size());
    }

    @Test
    void importThenRefreshRaisesTheAlert() throws IOException {
        monitor.attach(alerts::add);
        service.setBudget(OCT, limits(Categories.GROCERIES, "100"));
        Path csv = Files.createTempFile("wealthsei-test", ".csv");
        csv.toFile().deleteOnExit();
        Files.writeString(csv, "Date,Description,Amount\n2026-10-02,LOBLAWS #1,-60\n2026-10-09,SOBEYS,-55\n");
        AtomicInteger ids = new AtomicInteger();
        TransactionService transactions = new TransactionService(txRepo,
                new CsvImporter(() -> "imp" + ids.incrementAndGet()),
                new Categorizer(List.of(KeywordRuleStrategy.withDefaults())));
        transactions.importFile(csv);
        service.refreshAlerts(Set.of(OCT));
        assertEquals(1, alerts.size());
        assertEquals(AlertSeverity.OVER, alerts.get(0).severity());
    }
}
