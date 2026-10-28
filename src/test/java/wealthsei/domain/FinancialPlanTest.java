package wealthsei.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FinancialPlanTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);
    private static final Category RENT = new Category("rent", "Rent", true);

    private FinancialPlan base() {
        return FinancialPlan.starting(Money.of("1000"), TODAY);
    }

    @Test
    void startingPlanIsEmpty() {
        FinancialPlan p = base();
        assertEquals(Money.of("1000"), p.currentBalance());
        assertEquals(TODAY, p.balanceAsOf());
        assertEquals(Money.ZERO, p.safetyBuffer());
        assertTrue(p.incomeSources().isEmpty());
        assertTrue(p.fixedBills().isEmpty());
        assertTrue(p.plannedExpenses().isEmpty());
        assertEquals(Money.ZERO, p.monthlyIncome());
        assertEquals(Money.ZERO, p.monthlyFixedBills());
    }

    @Test
    void withMethodsReturnChangedCopiesAndKeepTheOriginal() {
        FinancialPlan p = base();
        FinancialPlan q = p.withSafetyBuffer(Money.of("200"))
                .withBalance(Money.of("1500"), TODAY.plusDays(1))
                .withIncomeSource(new IncomeSource("Salary", Money.of("2000"), Frequency.MONTHLY, TODAY))
                .withFixedBill(new FixedBill("Rent", Money.of("900"), Frequency.MONTHLY, TODAY, RENT))
                .withPlannedExpense(new PlannedExpense("Laptop", Money.of("1200"), TODAY.plusDays(30), null));
        assertEquals(Money.ZERO, p.safetyBuffer());
        assertTrue(p.incomeSources().isEmpty());
        assertEquals(Money.of("200"), q.safetyBuffer());
        assertEquals(Money.of("1500"), q.currentBalance());
        assertEquals(TODAY.plusDays(1), q.balanceAsOf());
        assertEquals(1, q.incomeSources().size());
        assertEquals(1, q.fixedBills().size());
        assertEquals(1, q.plannedExpenses().size());
    }

    @Test
    void monthlyTotalsConvertEachFrequency() {
        FinancialPlan p = base()
                .withIncomeSource(new IncomeSource("Job", Money.of("2000"), Frequency.BIWEEKLY, TODAY))
                .withIncomeSource(new IncomeSource("Tutoring", Money.of("500"), Frequency.MONTHLY, TODAY))
                .withFixedBill(new FixedBill("Rent", Money.of("1500"), Frequency.MONTHLY, TODAY, RENT))
                .withFixedBill(new FixedBill("Phone", Money.of("60"), Frequency.MONTHLY, TODAY, null))
                .withFixedBill(new FixedBill("Insurance", Money.of("1200"), Frequency.YEARLY, TODAY, null));
        assertEquals(Money.of("4833.33"), p.monthlyIncome());
        assertEquals(Money.of("1660.00"), p.monthlyFixedBills());
    }

    @Test
    void listsAreDefensivelyCopiedAndUnmodifiable() {
        List<IncomeSource> source = new ArrayList<>();
        FinancialPlan p = new FinancialPlan(Money.of("1"), TODAY, Money.ZERO, source, List.of(), List.of());
        source.add(new IncomeSource("Job", Money.of("1"), Frequency.MONTHLY, TODAY));
        assertTrue(p.incomeSources().isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> p.incomeSources().add(new IncomeSource("X", Money.of("1"), Frequency.MONTHLY, TODAY)));
    }

    @Test
    void allowsNegativeBalanceButNotNegativeBuffer() {
        assertEquals(Money.of("-50"), FinancialPlan.starting(Money.of("-50"), TODAY).currentBalance());
        assertThrows(IllegalArgumentException.class, () -> base().withSafetyBuffer(Money.of("-1")));
    }

    @Test
    void incomeSourceValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> new IncomeSource(" ", Money.of("1"), Frequency.MONTHLY, TODAY));
        assertThrows(IllegalArgumentException.class,
                () -> new IncomeSource("Job", Money.ZERO, Frequency.MONTHLY, TODAY));
        assertThrows(NullPointerException.class,
                () -> new IncomeSource("Job", Money.of("1"), null, TODAY));
    }

    @Test
    void fixedBillValidationAndOptionalCategory() {
        assertThrows(IllegalArgumentException.class,
                () -> new FixedBill("", Money.of("1"), Frequency.MONTHLY, TODAY, null));
        assertThrows(IllegalArgumentException.class,
                () -> new FixedBill("Rent", Money.of("-5"), Frequency.MONTHLY, TODAY, null));
        assertTrue(new FixedBill("Rent", Money.of("5"), Frequency.MONTHLY, TODAY, null).categoryIfSet().isEmpty());
        assertEquals(RENT, new FixedBill("Rent", Money.of("5"), Frequency.MONTHLY, TODAY, RENT).categoryIfSet().get());
    }

    @Test
    void plannedExpenseValidationAndOptionalCategory() {
        assertThrows(IllegalArgumentException.class, () -> new PlannedExpense("", Money.of("1"), TODAY, null));
        assertThrows(IllegalArgumentException.class, () -> new PlannedExpense("Trip", Money.ZERO, TODAY, null));
        assertTrue(new PlannedExpense("Trip", Money.of("1"), TODAY, null).categoryIfSet().isEmpty());
    }
}
