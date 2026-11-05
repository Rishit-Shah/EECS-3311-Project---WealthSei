package wealthsei.command;

import java.time.YearMonth;
import java.util.Objects;
import wealthsei.domain.Category;
import wealthsei.domain.Money;
import wealthsei.service.BudgetService;
import wealthsei.service.ValidationException;

/** Changes one category's limit for a month; undoing it puts the old limit back. */
public class SetBudgetLimitCommand implements Command {

    private final BudgetService budgets;
    private final YearMonth month;
    private final Category category;
    private final Money newLimit;
    private Money oldLimit;

    public SetBudgetLimitCommand(BudgetService budgets, YearMonth month, Category category, Money newLimit) {
        this.budgets = Objects.requireNonNull(budgets, "budgets");
        this.month = Objects.requireNonNull(month, "month");
        this.category = Objects.requireNonNull(category, "category");
        this.newLimit = Objects.requireNonNull(newLimit, "newLimit");
    }

    @Override
    public void execute() {
        oldLimit = budgets.limitOf(month, category).orElseThrow(() -> new ValidationException(
                "There is no " + category.name() + " limit for " + month + " to change"));
        budgets.setLimit(month, category, newLimit);
    }

    @Override
    public void undo() {
        if (oldLimit != null) {
            budgets.setLimit(month, category, oldLimit);
        }
    }

    @Override
    public String describe() {
        return "Set the " + category.name() + " limit for " + month + " to " + newLimit
                + (oldLimit != null ? " (was " + oldLimit + ")" : "");
    }
}
