package wealthsei.analytics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import wealthsei.domain.Frequency;
import wealthsei.domain.IncomeSource;
import wealthsei.domain.Money;
import wealthsei.service.ValidationException;

/** A raise or extra income of a fixed amount every month, starting on the first of next month. */
public class IncomeChange implements ScenarioChange {

    static final String NAME = "What-if extra income";

    private final Money monthlyDelta;

    public IncomeChange(Money monthlyDelta) {
        this.monthlyDelta = Objects.requireNonNull(monthlyDelta, "monthlyDelta");
        if (!monthlyDelta.isPositive()) {
            throw new ValidationException("The monthly income change must be more than zero");
        }
    }

    @Override
    public ForecastInputs applyTo(ForecastInputs inputs) {
        return addMonthlyIncome(inputs, NAME + " " + monthlyDelta, monthlyDelta);
    }

    @Override
    public String describe() {
        return "Earn " + monthlyDelta + " more each month";
    }

    /** Adds a monthly inflow starting the first of next month; shared with the category cap. */
    static ForecastInputs addMonthlyIncome(ForecastInputs inputs, String name, Money amount) {
        List<IncomeSource> incomes = new ArrayList<>(inputs.incomeSources());
        LocalDate start = inputs.today().withDayOfMonth(1).plusMonths(1);
        incomes.add(new IncomeSource(name, amount, Frequency.MONTHLY, start));
        return new ForecastInputs(inputs.today(), inputs.openingBalance(), inputs.transactions(), inputs.recurring(),
                incomes, inputs.fixedBills(), inputs.plannedExpenses(), inputs.typicalMonthlySpending());
    }
}
