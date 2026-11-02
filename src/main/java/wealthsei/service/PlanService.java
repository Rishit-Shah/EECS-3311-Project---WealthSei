package wealthsei.service;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import wealthsei.domain.FinancialPlan;
import wealthsei.domain.FixedBill;
import wealthsei.domain.IncomeSource;
import wealthsei.domain.Money;
import wealthsei.domain.PlannedExpense;
import wealthsei.repository.PlanRepository;

/**
 * Validates and saves the user's plan: starting balance, safety buffer, recurring income,
 * fixed bills and planned expenses. The add methods build on whatever is already saved; if
 * nothing is saved yet they start from a zero balance dated today.
 */
public class PlanService {

    private final PlanRepository repo;
    private final Supplier<LocalDate> today;

    public PlanService(PlanRepository repo) {
        this(repo, LocalDate::now);
    }

    /** @param today supplies the current date (tests pass a fixed one) */
    public PlanService(PlanRepository repo, Supplier<LocalDate> today) {
        this.repo = Objects.requireNonNull(repo, "repo");
        this.today = Objects.requireNonNull(today, "today");
    }

    public Optional<FinancialPlan> getPlan() {
        return repo.find();
    }

    /**
     * @throws ValidationException if the balance date is in the future
     */
    public FinancialPlan savePlan(FinancialPlan plan) {
        validate(plan);
        repo.save(plan);
        return plan;
    }

    public FinancialPlan setBalance(Money balance, LocalDate asOf) {
        Objects.requireNonNull(balance, "balance");
        Objects.requireNonNull(asOf, "asOf");
        return savePlan(current().withBalance(balance, asOf));
    }

    /** @throws ValidationException if the buffer is negative */
    public FinancialPlan setSafetyBuffer(Money buffer) {
        if (buffer == null || buffer.isNegative()) {
            throw new ValidationException("The safety buffer cannot be negative");
        }
        return savePlan(current().withSafetyBuffer(buffer));
    }

    public FinancialPlan addIncome(IncomeSource income) {
        return savePlan(current().withIncomeSource(Objects.requireNonNull(income, "income")));
    }

    public FinancialPlan addFixedBill(FixedBill bill) {
        return savePlan(current().withFixedBill(Objects.requireNonNull(bill, "bill")));
    }

    public FinancialPlan addPlannedExpense(PlannedExpense expense) {
        return savePlan(current().withPlannedExpense(Objects.requireNonNull(expense, "expense")));
    }

    private FinancialPlan current() {
        return repo.find().orElseGet(() -> FinancialPlan.starting(Money.ZERO, today.get()));
    }

    private void validate(FinancialPlan plan) {
        Objects.requireNonNull(plan, "plan");
        if (plan.balanceAsOf().isAfter(today.get())) {
            throw new ValidationException("The balance date " + plan.balanceAsOf() + " is in the future");
        }
    }
}
