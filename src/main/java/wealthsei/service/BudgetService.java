package wealthsei.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import wealthsei.domain.Budget;
import wealthsei.domain.BudgetLimits;
import wealthsei.domain.BudgetLine;
import wealthsei.domain.BudgetStatus;
import wealthsei.domain.Category;
import wealthsei.domain.Money;
import wealthsei.domain.TxFilter;
import wealthsei.repository.BudgetRepository;
import wealthsei.repository.TransactionRepository;

/** Sets monthly budgets, reports how spending compares, and keeps alerts up to date. */
public class BudgetService {

    private final BudgetRepository repo;
    private final TransactionRepository txRepo;
    private final AlertMonitor monitor;

    public BudgetService(BudgetRepository repo, TransactionRepository txRepo, AlertMonitor monitor) {
        this.repo = Objects.requireNonNull(repo, "repo");
        this.txRepo = Objects.requireNonNull(txRepo, "txRepo");
        this.monitor = Objects.requireNonNull(monitor, "monitor");
    }

    /**
     * Saves the month's budget (replacing any earlier one), computes the status and raises
     * alerts.
     *
     * @throws ValidationException if there are no limits or any limit is not above zero
     */
    public BudgetStatus setBudget(YearMonth month, BudgetLimits limits) {
        validate(limits);
        repo.save(Budget.from(month, limits));
        return statusAndAlert(month);
    }

    /**
     * Changes one category's limit, adding the category if the month has no limit for it yet
     * (and creating the month's budget if there is none).
     *
     * @throws ValidationException if the limit is not above zero
     */
    public BudgetStatus setLimit(YearMonth month, Category category, Money limit) {
        Objects.requireNonNull(category, "category");
        if (limit == null || !limit.isPositive()) {
            throw new ValidationException("The limit for " + category.name() + " must be more than zero");
        }
        List<BudgetLine> lines = new ArrayList<>();
        boolean replaced = false;
        for (BudgetLine line : repo.find(month).map(Budget::lines).orElse(List.of())) {
            if (line.category().equals(category)) {
                lines.add(new BudgetLine(category, limit));
                replaced = true;
            } else {
                lines.add(line);
            }
        }
        if (!replaced) {
            lines.add(new BudgetLine(category, limit));
        }
        repo.save(new Budget(month, lines));
        return statusAndAlert(month);
    }

    /** @throws ValidationException if no budget has been set for the month */
    public BudgetStatus getStatus(YearMonth month) {
        return computeStatus(repo.find(month)
                .orElseThrow(() -> new ValidationException("No budget has been set for " + month)));
    }

    /** The limit set for a category in a month, or empty if there is none. */
    public java.util.Optional<Money> limitOf(YearMonth month, Category category) {
        return repo.find(month).flatMap(b -> b.limitFor(category));
    }

    public boolean hasBudget(YearMonth month) {
        return repo.find(month).isPresent();
    }

    /** Re-checks the given months (for example after an import) and raises any new alerts. */
    public void refreshAlerts(Set<YearMonth> months) {
        for (YearMonth month : months) {
            repo.find(month).ifPresent(budget -> monitor.evaluate(computeStatus(budget)));
        }
    }

    private BudgetStatus statusAndAlert(YearMonth month) {
        BudgetStatus status = getStatus(month);
        monitor.evaluate(status);
        return status;
    }

    private BudgetStatus computeStatus(Budget budget) {
        YearMonth month = budget.month();
        return BudgetCalculator.compute(budget,
                txRepo.query(TxFilter.between(month.atDay(1), month.atEndOfMonth())));
    }

    private void validate(BudgetLimits limits) {
        Objects.requireNonNull(limits, "limits");
        if (limits.limits().isEmpty()) {
            throw new ValidationException("Enter at least one category limit");
        }
        limits.limits().forEach((category, limit) -> {
            if (limit == null || !limit.isPositive()) {
                throw new ValidationException("The limit for " + category.name() + " must be more than zero");
            }
        });
    }
}
