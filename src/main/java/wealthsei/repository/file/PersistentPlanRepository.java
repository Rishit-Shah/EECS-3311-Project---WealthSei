package wealthsei.repository.file;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.Category;
import wealthsei.domain.FinancialPlan;
import wealthsei.domain.FixedBill;
import wealthsei.domain.Frequency;
import wealthsei.domain.IncomeSource;
import wealthsei.domain.Money;
import wealthsei.domain.PlannedExpense;
import wealthsei.repository.PlanRepository;

/**
 * Adds saving to a file on top of any {@link PlanRepository} (Decorator pattern). Each line
 * starts with what it holds: balance, buffer, income, bill or expense.
 */
public class PersistentPlanRepository implements PlanRepository {

    private final PlanRepository inner;
    private final Path file;

    public PersistentPlanRepository(PlanRepository inner, Path file) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.file = Objects.requireNonNull(file, "file");
        FinancialPlan plan = null;
        for (TsvFile.Row row : TsvFile.read(file)) {
            plan = apply(row, plan);
        }
        if (plan != null) {
            inner.save(plan);
        }
    }

    @Override
    public void save(FinancialPlan plan) {
        inner.save(plan);
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("balance", plan.currentBalance().toString(), plan.balanceAsOf().toString()));
        rows.add(List.of("buffer", plan.safetyBuffer().toString()));
        for (IncomeSource i : plan.incomeSources()) {
            rows.add(List.of("income", i.name(), i.amount().toString(), i.frequency().name(),
                    i.nextPayDate().toString()));
        }
        for (FixedBill b : plan.fixedBills()) {
            List<String> row = new ArrayList<>(List.of("bill", b.name(), b.amount().toString(),
                    b.frequency().name(), b.nextDueDate().toString()));
            FileCodec.addCategory(row, b.categoryIfSet().orElse(null));
            rows.add(row);
        }
        for (PlannedExpense e : plan.plannedExpenses()) {
            List<String> row = new ArrayList<>(List.of("expense", e.description(), e.amount().toString(),
                    e.date().toString()));
            FileCodec.addCategory(row, e.categoryIfSet().orElse(null));
            rows.add(row);
        }
        TsvFile.write(file, rows);
    }

    @Override
    public Optional<FinancialPlan> find() {
        return inner.find();
    }

    private FinancialPlan apply(TsvFile.Row row, FinancialPlan plan) {
        return FileCodec.parse(file, row, r -> {
            String kind = FileCodec.field(r, 0);
            if (kind.equals("balance")) {
                Money balance = Money.of(FileCodec.field(r, 1));
                LocalDate asOf = LocalDate.parse(FileCodec.field(r, 2));
                return plan == null ? FinancialPlan.starting(balance, asOf) : plan.withBalance(balance, asOf);
            }
            if (plan == null) {
                throw new IllegalArgumentException("'" + kind + "' line comes before the balance line");
            }
            return switch (kind) {
                case "buffer" -> plan.withSafetyBuffer(Money.of(FileCodec.field(r, 1)));
                case "income" -> plan.withIncomeSource(new IncomeSource(FileCodec.field(r, 1),
                        Money.of(FileCodec.field(r, 2)), Frequency.valueOf(FileCodec.field(r, 3)),
                        LocalDate.parse(FileCodec.field(r, 4))));
                case "bill" -> {
                    Category c = FileCodec.readCategory(r, 5);
                    yield plan.withFixedBill(new FixedBill(FileCodec.field(r, 1), Money.of(FileCodec.field(r, 2)),
                            Frequency.valueOf(FileCodec.field(r, 3)), LocalDate.parse(FileCodec.field(r, 4)), c));
                }
                case "expense" -> {
                    Category c = FileCodec.readCategory(r, 4);
                    yield plan.withPlannedExpense(new PlannedExpense(FileCodec.field(r, 1),
                            Money.of(FileCodec.field(r, 2)), LocalDate.parse(FileCodec.field(r, 3)), c));
                }
                default -> throw new IllegalArgumentException("unknown line type '" + kind + "'");
            };
        });
    }
}
