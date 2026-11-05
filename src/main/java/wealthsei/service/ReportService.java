package wealthsei.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import wealthsei.domain.BudgetLineStatus;
import wealthsei.domain.BudgetStatus;
import wealthsei.domain.Category;
import wealthsei.domain.ExportFormat;
import wealthsei.domain.Money;
import wealthsei.domain.Transaction;
import wealthsei.domain.TxFilter;
import wealthsei.repository.ReviewRepository;
import wealthsei.report.CsvExporter;
import wealthsei.report.ExportException;
import wealthsei.report.MarkdownExporter;
import wealthsei.report.ReportData;
import wealthsei.report.ReportExporter;
import wealthsei.report.TextExporter;

/**
 * Collects a month's figures into {@link ReportData} and writes them out in the chosen format. It also
 * chooses the exporter for a format (Factory pattern), so callers never name a concrete exporter.
 */
public class ReportService {

    private final TransactionService transactions;
    private final BudgetService budgets;
    private final GoalService goals;
    private final ReviewRepository reviews;
    private final Supplier<LocalDate> today;

    public ReportService(TransactionService transactions, BudgetService budgets, GoalService goals,
                         ReviewRepository reviews, Supplier<LocalDate> today) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.budgets = Objects.requireNonNull(budgets, "budgets");
        this.goals = Objects.requireNonNull(goals, "goals");
        this.reviews = Objects.requireNonNull(reviews, "reviews");
        this.today = Objects.requireNonNull(today, "today");
    }

    /** @throws ValidationException if the month has no transactions */
    public ReportData buildReportData(YearMonth month) {
        List<Transaction> txs = transactions.query(TxFilter.between(month.atDay(1), month.atEndOfMonth()));
        if (txs.isEmpty()) {
            throw new ValidationException("There are no transactions in " + month + ", so there is nothing to report");
        }
        Money income = Money.ZERO;
        Money spending = Money.ZERO;
        Map<Category, Money> spentBy = new HashMap<>();
        for (Transaction tx : txs) {
            if (tx.income()) {
                income = income.add(tx.amount());
            } else {
                spending = spending.add(tx.amount());
                spentBy.merge(tx.category(), tx.amount(), Money::add);
            }
        }
        Map<Category, BudgetLineStatus> lines = new HashMap<>();
        if (budgets.hasBudget(month)) {
            BudgetStatus status = budgets.getStatus(month);
            status.lines().forEach(l -> lines.put(l.category(), l));
        }
        List<ReportData.CategoryRow> rows = new ArrayList<>();
        for (Category c : union(spentBy, lines)) {
            BudgetLineStatus line = lines.get(c);
            rows.add(new ReportData.CategoryRow(c, spentBy.getOrDefault(c, Money.ZERO),
                    Optional.ofNullable(line).map(BudgetLineStatus::limit),
                    Optional.ofNullable(line).map(BudgetLineStatus::level)));
        }
        rows.sort(Comparator.comparing(ReportData.CategoryRow::spent).reversed()
                .thenComparing(r -> r.category().name()));
        return new ReportData(month, today.get(), income, spending, txs.size(), rows, goals.goals(),
                reviews.findByMonth(month));
    }

    /**
     * Writes the report for a month.
     *
     * @param overwrite whether an existing file may be replaced
     * @return the file written
     * @throws ValidationException if the month has nothing to report
     * @throws ExportException     if the file exists and may not be replaced, or cannot be written
     */
    public Path export(YearMonth month, ExportFormat format, Path out, boolean overwrite) {
        Objects.requireNonNull(out, "out");
        if (Files.exists(out) && !overwrite) {
            throw new ExportException(out + " already exists. Add 'overwrite' to replace it.");
        }
        return exporterFor(format).export(buildReportData(month), out);
    }

    ReportExporter exporterFor(ExportFormat format) {
        return switch (Objects.requireNonNull(format, "format")) {
            case MARKDOWN -> new MarkdownExporter();
            case CSV -> new CsvExporter();
            case TEXT -> new TextExporter();
        };
    }

    private static java.util.Set<Category> union(Map<Category, Money> spent, Map<Category, BudgetLineStatus> lines) {
        java.util.Set<Category> all = new java.util.LinkedHashSet<>(spent.keySet());
        all.addAll(lines.keySet());
        return all;
    }
}
