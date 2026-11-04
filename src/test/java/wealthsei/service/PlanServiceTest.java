package wealthsei.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import wealthsei.domain.Categories;
import wealthsei.domain.FinancialPlan;
import wealthsei.domain.FixedBill;
import wealthsei.domain.Frequency;
import wealthsei.domain.IncomeSource;
import wealthsei.domain.Money;
import wealthsei.domain.PlannedExpense;
import wealthsei.repository.InMemoryPlanRepository;

class PlanServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 2);
    private final PlanService service = new PlanService(new InMemoryPlanRepository(), () -> TODAY);

    @Test
    void noPlanUntilOneIsSaved() {
        assertTrue(service.getPlan().isEmpty());
        service.setBalance(Money.of("1000"), TODAY);
        assertEquals(Money.of("1000"), service.getPlan().orElseThrow().currentBalance());
    }

    @Test
    void addMethodsStartFromAZeroBalanceDatedTodayWhenNothingIsSaved() {
        FinancialPlan p = service.addIncome(new IncomeSource("Job", Money.of("2000"), Frequency.MONTHLY, TODAY));
        assertEquals(Money.ZERO, p.currentBalance());
        assertEquals(TODAY, p.balanceAsOf());
        assertEquals(1, p.incomeSources().size());
    }

    @Test
    void addMethodsBuildOnTheSavedPlan() {
        service.setBalance(Money.of("500"), TODAY);
        service.addIncome(new IncomeSource("Job", Money.of("2000"), Frequency.MONTHLY, TODAY));
        service.addFixedBill(new FixedBill("Rent", Money.of("900"), Frequency.MONTHLY, TODAY, Categories.RENT));
        FinancialPlan p = service.addPlannedExpense(
                new PlannedExpense("Laptop", Money.of("1200"), TODAY.plusDays(20), null));
        assertEquals(Money.of("500"), p.currentBalance());
        assertEquals(1, p.incomeSources().size());
        assertEquals(1, p.fixedBills().size());
        assertEquals(1, p.plannedExpenses().size());
        assertEquals(p, service.getPlan().orElseThrow());
    }

    @Test
    void balanceDateInTheFutureIsRejectedAndNothingIsSaved() {
        assertThrows(ValidationException.class, () -> service.setBalance(Money.of("1"), TODAY.plusDays(1)));
        assertTrue(service.getPlan().isEmpty());
    }

    @Test
    void balanceDateTodayOrEarlierIsFine() {
        service.setBalance(Money.of("1"), TODAY);
        service.setBalance(Money.of("2"), TODAY.minusDays(30));
        assertEquals(Money.of("2"), service.getPlan().orElseThrow().currentBalance());
    }

    @Test
    void safetyBufferCannotBeNegative() {
        assertThrows(ValidationException.class, () -> service.setSafetyBuffer(Money.of("-1")));
        assertEquals(Money.of("250"), service.setSafetyBuffer(Money.of("250")).safetyBuffer());
    }

    @Test
    void savePlanReplacesTheOldOne() {
        service.setBalance(Money.of("1"), TODAY);
        service.savePlan(FinancialPlan.starting(Money.of("99"), TODAY));
        assertEquals(Money.of("99"), service.getPlan().orElseThrow().currentBalance());
    }

    @Test
    void aNewPlanStartsWithTheDefaultSafetyBuffer() {
        assertEquals(PlanService.DEFAULT_SAFETY_BUFFER, service.setBalance(Money.of("10"), TODAY).safetyBuffer());
        assertEquals(Money.of("200"), PlanService.DEFAULT_SAFETY_BUFFER);
    }
}
