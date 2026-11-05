package wealthsei.analytics;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import wealthsei.domain.FixedBill;
import wealthsei.domain.RecurringPayment;
import wealthsei.service.ValidationException;

/** Stops a recurring payment or a bill from the plan (matched by name, ignoring case). */
public class CancelRecurringChange implements ScenarioChange {

    private final String merchant;

    public CancelRecurringChange(String merchant) {
        this.merchant = Objects.requireNonNull(merchant, "merchant").trim();
        if (this.merchant.isEmpty()) {
            throw new ValidationException("Say which payment to cancel");
        }
    }

    /**
     * Matches by exact name (ignoring case), or else by a part of the name when that picks exactly one
     * item, so "Netflix" finds "Netflix.com".
     */
    @Override
    public ForecastInputs applyTo(ForecastInputs inputs) {
        List<String> names = new ArrayList<>();
        inputs.recurring().forEach(r -> names.add(r.merchant()));
        inputs.fixedBills().forEach(b -> names.add(b.name()));
        String target = resolve(names);
        List<RecurringPayment> recurring = new ArrayList<>(inputs.recurring());
        List<FixedBill> bills = new ArrayList<>(inputs.fixedBills());
        recurring.removeIf(r -> r.merchant().equalsIgnoreCase(target));
        bills.removeIf(b -> b.name().equalsIgnoreCase(target));
        return new ForecastInputs(inputs.today(), inputs.openingBalance(), inputs.transactions(), recurring,
                inputs.incomeSources(), bills, inputs.plannedExpenses(), inputs.typicalMonthlySpending());
    }

    private String resolve(List<String> names) {
        for (String name : names) {
            if (name.equalsIgnoreCase(merchant)) {
                return name;
            }
        }
        List<String> partial = names.stream().distinct()
                .filter(n -> n.toLowerCase().contains(merchant.toLowerCase())).toList();
        if (partial.size() == 1) {
            return partial.get(0);
        }
        String known = names.isEmpty() ? "" : " Known ones: " + String.join(", ", names.stream().distinct().toList()) + ".";
        if (partial.size() > 1) {
            throw new ValidationException("'" + merchant + "' matches more than one payment (" + String.join(", ", partial)
                    + "). Use the full name.");
        }
        throw new ValidationException("No recurring payment or bill called '" + merchant + "' was found." + known);
    }

    @Override
    public String describe() {
        return "Cancel " + merchant;
    }
}
