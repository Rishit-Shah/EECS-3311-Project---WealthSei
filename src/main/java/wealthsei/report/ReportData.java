package wealthsei.report;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import wealthsei.domain.AlertSeverity;
import wealthsei.domain.Category;
import wealthsei.domain.MonthlyReview;
import wealthsei.domain.Money;
import wealthsei.domain.SavingsGoal;

/**
 * Everything a report shows, collected once so every format prints the same facts.
 *
 * @param month         the month
 * @param generatedOn   the day the report was made
 * @param income        money that came in during the month
 * @param spending      money that went out during the month
 * @param transactionCount how many transactions the month has
 * @param categories    spending per category, with the limit and level when the month has a budget
 * @param goals         the savings goals and how they are doing
 * @param review        the saved monthly review, if one was generated
 */
public record ReportData(YearMonth month, LocalDate generatedOn, Money income, Money spending, int transactionCount,
                         List<CategoryRow> categories, List<SavingsGoal> goals, Optional<MonthlyReview> review) {

    /** One category's line: spent, and (when budgeted) the limit and how close it is. */
    public record CategoryRow(Category category, Money spent, Optional<Money> limit, Optional<AlertSeverity> level) {

        public CategoryRow {
            Objects.requireNonNull(category, "category");
            Objects.requireNonNull(spent, "spent");
            Objects.requireNonNull(limit, "limit");
            Objects.requireNonNull(level, "level");
        }

        public Optional<Money> remaining() {
            return limit.map(l -> l.subtract(spent));
        }
    }

    public ReportData {
        Objects.requireNonNull(month, "month");
        Objects.requireNonNull(generatedOn, "generatedOn");
        Objects.requireNonNull(income, "income");
        Objects.requireNonNull(spending, "spending");
        categories = List.copyOf(Objects.requireNonNull(categories, "categories"));
        goals = List.copyOf(Objects.requireNonNull(goals, "goals"));
        Objects.requireNonNull(review, "review");
    }

    public Money net() {
        return income.subtract(spending);
    }

    public boolean hasBudget() {
        return categories.stream().anyMatch(c -> c.limit().isPresent());
    }
}
