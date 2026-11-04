package wealthsei.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Alert;
import wealthsei.domain.AlertSeverity;
import wealthsei.domain.AlertType;
import wealthsei.domain.BudgetLineStatus;
import wealthsei.domain.BudgetStatus;
import wealthsei.domain.Categories;
import wealthsei.domain.DailyBalance;
import wealthsei.domain.Money;

class AlertMonitorTest {

    private static final Instant NOW = Instant.parse("2026-10-27T12:00:00Z");
    private static final YearMonth OCT = YearMonth.of(2026, 10);

    private final AlertMonitor monitor = new AlertMonitor(() -> NOW);
    private final List<Alert> received = new ArrayList<>();

    private BudgetStatus groceries(String spent) {
        return new BudgetStatus(OCT, List.of(
                new BudgetLineStatus(Categories.GROCERIES, Money.of(spent), Money.of("300"))));
    }

    @Test
    void noAlertBelowEightyPercent() {
        monitor.attach(received::add);
        monitor.evaluate(groceries("239.99"));
        assertTrue(received.isEmpty());
    }

    @Test
    void warnsAtEightyPercent() {
        monitor.attach(received::add);
        monitor.evaluate(groceries("255"));
        assertEquals(1, received.size());
        Alert a = received.get(0);
        assertEquals(AlertType.BUDGET, a.type());
        assertEquals(AlertSeverity.WARNING, a.severity());
        assertEquals("Groceries is at 85% of its $300.00 budget ($255.00 spent)", a.message());
        assertEquals(NOW, a.createdAt());
    }

    @Test
    void overBudgetIsRaisedAsOver() {
        monitor.attach(received::add);
        monitor.evaluate(groceries("340"));
        assertEquals(AlertSeverity.OVER, received.get(0).severity());
        assertEquals("Groceries is over budget: $340.00 spent of $300.00", received.get(0).message());
    }

    @Test
    void sameLevelIsNotRepeated() {
        monitor.attach(received::add);
        monitor.evaluate(groceries("255"));
        monitor.evaluate(groceries("260"));
        assertEquals(1, received.size());
    }

    @Test
    void escalationFromWarningToOverIsRaised() {
        monitor.attach(received::add);
        monitor.evaluate(groceries("255"));
        monitor.evaluate(groceries("310"));
        assertEquals(2, received.size());
        assertEquals(AlertSeverity.OVER, received.get(1).severity());
    }

    @Test
    void droppingBelowEightyPercentResetsSoItCanWarnAgain() {
        monitor.attach(received::add);
        monitor.evaluate(groceries("255"));
        monitor.evaluate(groceries("100"));
        monitor.evaluate(groceries("260"));
        assertEquals(2, received.size());
    }

    @Test
    void droppingFromOverToWarningDoesNotAlertAgainButOverAfterThatDoes() {
        monitor.attach(received::add);
        monitor.evaluate(groceries("310"));
        monitor.evaluate(groceries("270"));
        assertEquals(1, received.size());
        monitor.evaluate(groceries("320"));
        assertEquals(2, received.size());
    }

    @Test
    void eachMonthIsTrackedSeparately() {
        monitor.attach(received::add);
        monitor.evaluate(groceries("255"));
        monitor.evaluate(new BudgetStatus(YearMonth.of(2026, 11), List.of(
                new BudgetLineStatus(Categories.GROCERIES, Money.of("255"), Money.of("300")))));
        assertEquals(2, received.size());
    }

    @Test
    void allListenersAreToldAndDetachedOnesAreNot() {
        List<Alert> second = new ArrayList<>();
        AlertListener first = received::add;
        AlertListener other = second::add;
        monitor.attach(first);
        monitor.attach(other);
        monitor.publish(new Alert(AlertType.LOW_BALANCE, AlertSeverity.WARNING, "Low balance", NOW));
        assertEquals(1, received.size());
        assertEquals(1, second.size());
        monitor.detach(first);
        monitor.publish(new Alert(AlertType.LOW_BALANCE, AlertSeverity.WARNING, "Low balance", NOW));
        assertEquals(1, received.size());
        assertEquals(2, second.size());
    }

    @Test
    void aBrokenListenerDoesNotStopTheOthers() {
        monitor.attach(alert -> {
            throw new IllegalStateException("window closed");
        });
        monitor.attach(received::add);
        monitor.evaluate(groceries("255"));
        assertEquals(1, received.size());
    }

    private static DailyBalance low(String balance) {
        return new DailyBalance(LocalDate.of(2026, 11, 20), Money.of(balance), Money.ZERO, Money.ZERO);
    }

    @Test
    void lowBalanceAboveTheBufferRaisesNothing() {
        monitor.attach(received::add);
        monitor.evaluateLowBalance(low("200"), Money.of("200"));
        monitor.evaluateLowBalance(low("500"), Money.of("200"));
        assertTrue(received.isEmpty());
    }

    @Test
    void lowBalanceBelowTheBufferWarnsOnceWithTheDateAndAmounts() {
        monitor.attach(received::add);
        monitor.evaluateLowBalance(low("150"), Money.of("200"));
        monitor.evaluateLowBalance(low("120"), Money.of("200"));
        assertEquals(1, received.size());
        Alert a = received.get(0);
        assertEquals(AlertType.LOW_BALANCE, a.type());
        assertEquals(AlertSeverity.WARNING, a.severity());
        assertEquals(NOW, a.createdAt());
        assertTrue(a.message().contains("$150.00") && a.message().contains("2026-11-20") && a.message().contains("$200.00"),
                a.message());
    }

    @Test
    void lowBalanceEscalatesToOverBelowZeroAndResetsWhenBackAboveTheBuffer() {
        monitor.attach(received::add);
        monitor.evaluateLowBalance(low("100"), Money.of("200"));
        monitor.evaluateLowBalance(low("-50"), Money.of("200"));
        assertEquals(2, received.size());
        assertEquals(AlertSeverity.OVER, received.get(1).severity());
        monitor.evaluateLowBalance(low("900"), Money.of("200"));
        monitor.evaluateLowBalance(low("100"), Money.of("200"));
        assertEquals(3, received.size());
    }
}
