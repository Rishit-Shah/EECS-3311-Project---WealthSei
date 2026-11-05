package wealthsei.agent;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import wealthsei.domain.Categories;
import wealthsei.domain.Category;
import wealthsei.domain.Money;
import wealthsei.domain.Transaction;
import wealthsei.domain.TxFilter;
import wealthsei.llm.ParamSpec;
import wealthsei.llm.ParamType;
import wealthsei.llm.ToolArgs;
import wealthsei.llm.ToolSpec;
import wealthsei.service.TransactionService;

/** Totals and the biggest items for transactions matching a month, category or merchant. */
public class TransactionQueryTool implements Tool {

    private static final int TOP = 5;

    private final TransactionService transactions;

    public TransactionQueryTool(TransactionService transactions) {
        this.transactions = transactions;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec("transactions",
                "Totals (spent, received, count) and the largest expenses for transactions that match the filters.",
                List.of(new ParamSpec("month", ParamType.STRING, "Month as YYYY-MM", false),
                        new ParamSpec("category", ParamType.STRING, "Category id or name, e.g. dining", false),
                        new ParamSpec("merchant", ParamType.STRING, "Part of the merchant name", false)));
    }

    @Override
    public ToolResult run(ToolArgs args) {
        TxFilter filter = TxFilter.all();
        if (args.has("month")) {
            YearMonth month;
            try {
                month = YearMonth.parse(args.requireText("month"));
            } catch (java.time.format.DateTimeParseException e) {
                return ToolResult.failure("The month must look like 2026-10");
            }
            filter = TxFilter.between(month.atDay(1), month.atEndOfMonth());
        }
        if (args.has("category")) {
            Category category = findCategory(args.requireText("category"));
            if (category == null) {
                return ToolResult.failure("Unknown category '" + args.requireText("category") + "'");
            }
            filter = filter.withCategory(category);
        }
        if (args.has("merchant")) {
            filter = filter.withMerchant(args.requireText("merchant"));
        }
        List<Transaction> found = transactions.query(filter);
        Money spent = Money.ZERO;
        Money received = Money.ZERO;
        List<Transaction> expenses = new ArrayList<>();
        for (Transaction t : found) {
            if (t.isExpense()) {
                spent = spent.add(t.amount());
                expenses.add(t);
            } else {
                received = received.add(t.amount());
            }
        }
        expenses.sort(Comparator.comparing(Transaction::amount).reversed());
        List<Object> top = new ArrayList<>();
        for (Transaction t : expenses.subList(0, Math.min(TOP, expenses.size()))) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", t.date().toString());
            row.put("merchant", t.merchant());
            row.put("amount", t.amount().amount());
            top.add(row);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "transactions");
        data.put("count", found.size());
        data.put("totalSpent", spent.amount());
        data.put("totalReceived", received.amount());
        data.put("largestExpenses", top);
        return ToolResult.ok(data);
    }

    private static Category findCategory(String text) {
        return Categories.find(text.toLowerCase()).orElseGet(() -> Categories.defaults().stream()
                .filter(c -> c.name().equalsIgnoreCase(text)).findFirst().orElse(null));
    }
}
