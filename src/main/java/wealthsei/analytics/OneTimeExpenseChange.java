package wealthsei.analytics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import wealthsei.domain.Money;
import wealthsei.domain.PlannedExpense;
import wealthsei.service.ValidationException;

/** A single extra expense on a given day, within the six months being simulated. */
public class OneTimeExpenseChange implements ScenarioChange {

    private final Money amount;
    private final LocalDate onDate;

    public OneTimeExpenseChange(Money amount, LocalDate onDate) {
        this.amount = Objects.requireNonNull(amount, "amount");
        this.onDate = Objects.requireNonNull(onDate, "onDate");
        if (!amount.isPositive()) {
            throw new ValidationException("The expense must be more than zero");
        }
    }

    @Override
    public ForecastInputs applyTo(ForecastInputs inputs) {
        LocalDate last = inputs.today().plusDays(ScenarioSimulator.HORIZON_DAYS);
        if (!onDate.isAfter(inputs.today()) || onDate.isAfter(last)) {
            throw new ValidationException("The expense date must be after today and within the next 6 months (by " + last + ")");
        }
        List<PlannedExpense> expenses = new ArrayList<>(inputs.plannedExpenses());
        expenses.add(new PlannedExpense("What-if expense", amount, onDate, null));
        return new ForecastInputs(inputs.today(), inputs.openingBalance(), inputs.transactions(), inputs.recurring(),
                inputs.incomeSources(), inputs.fixedBills(), expenses);
    }

    @Override
    public String describe() {
        return "Spend " + amount + " once on " + onDate;
    }
}
