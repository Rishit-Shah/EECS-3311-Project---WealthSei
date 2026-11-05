package wealthsei.presentation.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import wealthsei.analytics.RecurringReport;
import wealthsei.domain.Alert;
import wealthsei.domain.BudgetLimits;
import wealthsei.domain.BudgetLineStatus;
import wealthsei.domain.BudgetStatus;
import wealthsei.domain.Categories;
import wealthsei.domain.DailyBalance;
import wealthsei.domain.Forecast;
import wealthsei.domain.SafeToSpendResult;
import wealthsei.domain.Category;
import wealthsei.domain.FinancialPlan;
import wealthsei.domain.FixedBill;
import wealthsei.domain.Frequency;
import wealthsei.domain.ImportResult;
import wealthsei.domain.IncomeSource;
import wealthsei.domain.Money;
import wealthsei.domain.PlannedExpense;
import wealthsei.domain.RecurringPayment;
import wealthsei.domain.Transaction;
import wealthsei.domain.TxFilter;
import wealthsei.facade.WealthSeiFacade;
import wealthsei.repository.file.StorageException;
import wealthsei.service.TransactionImportException;
import wealthsei.service.ValidationException;

/**
 * The command-line screen. It only reads text, calls the {@link WealthSeiFacade} and prints
 * the answer; no calculation happens here.
 */
public class CliApp {

    private static final String PROMPT = "wealthsei> ";

    private final WealthSeiFacade facade;
    private final PrintStream out;
    private final Supplier<LocalDate> today;

    public CliApp(WealthSeiFacade facade, PrintStream out) {
        this(facade, out, LocalDate::now);
    }

    /** @param today supplies the current date (tests pass a fixed one) */
    public CliApp(WealthSeiFacade facade, PrintStream out, Supplier<LocalDate> today) {
        this.facade = Objects.requireNonNull(facade, "facade");
        this.out = Objects.requireNonNull(out, "out");
        this.today = Objects.requireNonNull(today, "today");
        facade.addAlertListener(this::printAlert);
    }

    /** Reads commands until the input ends or the user types {@code exit}. */
    public void run(BufferedReader in) throws IOException {
        out.println("WealthSei - type 'help' for commands, 'exit' to quit.");
        out.print(PROMPT);
        out.flush();
        String line;
        while ((line = in.readLine()) != null) {
            if (!execute(line)) {
                return;
            }
            out.print(PROMPT);
            out.flush();
        }
        out.println();
    }

    /** Runs one command line. Returns false when the user asked to quit. */
    public boolean execute(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return true;
        }
        try {
            List<String> args = ArgTokenizer.tokenize(trimmed);
            String command = args.get(0).toLowerCase();
            List<String> rest = args.subList(1, args.size());
            switch (command) {
                case "help" -> help();
                case "exit", "quit" -> {
                    out.println("Goodbye.");
                    return false;
                }
                case "import" -> importFile(rest);
                case "transactions" -> transactions(rest);
                case "categories" -> categories();
                case "correct" -> correct(rest);
                case "undo" -> undo();
                case "redo" -> redo();
                case "budget" -> budget(rest);
                case "plan" -> plan(rest);
                case "recurring" -> recurring(rest);
                case "safe" -> safe(rest);
                case "forecast" -> forecast(rest);
                case "habits" -> habits(rest);
                case "goal" -> goal(rest);
                case "afford" -> afford(rest);
                case "whatif" -> whatif(rest);
                case "review" -> review(rest);
                case "ask" -> ask(rest);
                case "forget" -> forget();
                case "trace" -> trace(rest);
                case "export" -> export(rest);
                case "proposals" -> proposals(rest);
                case "protect" -> protect(rest, true);
                case "unprotect" -> protect(rest, false);
                case "score" -> score(rest);
                default -> out.println("Unknown command '" + command + "'. Type 'help'.");
            }
        } catch (UsageException e) {
            out.println("Usage: " + e.getMessage());
        } catch (ValidationException | TransactionImportException | StorageException | DateTimeParseException
                 | IllegalArgumentException | wealthsei.command.IllegalStateTransitionException
                 | wealthsei.report.ExportException e) {
            out.println("Error: " + e.getMessage());
        }
        return true;
    }

    // ---- commands ----

    private void help() {
        out.println("""
                Commands:
                  import <file.csv>                         load a bank CSV export
                  transactions [month=YYYY-MM] [category=<id>] [merchant=<text>]
                  categories                                list category ids
                  correct <txId> <categoryId>               fix a category (the app remembers the merchant)
                  undo | redo                               reverse or repeat a correction
                  budget set <YYYY-MM> <categoryId>=<amount> ...
                  budget status <YYYY-MM>
                  plan show
                  plan balance <amount> [YYYY-MM-DD]
                  plan buffer <amount>
                  plan income <name> <amount> <frequency> <nextPayDate>
                  plan bill <name> <amount> <frequency> <nextDueDate> [categoryId]
                  plan expense <description> <amount> <date> [categoryId]
                  recurring                                 find subscriptions and other repeating payments
                  recurring flag|unflag <merchant>          mark a recurring payment for review
                  safe [YYYY-MM-DD]                         how much you can spend per day this month
                  forecast [days]                           projected balance for the next days (default 30)
                  habits <YYYY-MM>                          spending habits found in a month
                  goal add "<name>" <target> <YYYY-MM-DD> [saved]   create a savings goal
                  goal list                                 your goals and how they are doing
                  goal replan <id>                          can it be reached? what to change if not
                  goal save <id> <amount>                   record money set aside for a goal
                  goal monthly <id> <amount>                set what you put aside each month
                  afford "<item>" <price> [YYYY-MM-DD]      can I afford it? (AI agent, checked against your numbers)
                  whatif <text>                             try a scenario in plain words (AI turns it into changes)
                  whatif cancel=<name> caps=<cat>=<amt> income=<amt> buy=<amt>@<date>
                                                            the same with the changes typed directly (no AI)
                  trace [id]                                how the last answer was reached (tools used, figures)
                  export <YYYY-MM> <md|csv|txt> <file> [overwrite]  save a month's report to a file
                  proposals [list|all]                      suggested changes waiting for your approval
                  proposals from-whatif                     turn the last what-if into proposals
                  proposals from-goal <goalId>              suggest the monthly amount a goal needs
                  proposals approve|reject <id>             apply or discard a proposal (approve can be undone)
                  proposals undo                            reverse the last approved proposal
                  proposals why <id>                        what a proposal does and where it came from
                  review [YYYY-MM]                          monthly review: what happened, what went well, what to fix
                  ask "<question>"                          ask about your finances; follow-ups are remembered
                  forget                                    start a new conversation
                  protect|unprotect [<category>]            categories the agent may never suggest changing
                  score [YYYY-MM]                           financial health score out of 100
                  exit
                Frequency is one of: weekly, biweekly, monthly, yearly. Quote names with spaces.""");
    }

    private void importFile(List<String> args) {
        need(args.size() == 1, "import <file.csv>");
        ImportResult r = facade.importTransactions(Path.of(args.get(0)), today.get());
        out.println("Imported " + r.imported() + " transactions, skipped " + r.duplicates()
                + " duplicates, rejected " + r.rejected().size() + " rows.");
        r.rejected().forEach(reason -> out.println("  rejected: " + reason));
    }

    private void transactions(List<String> args) {
        TxFilter filter = TxFilter.all();
        for (String arg : args) {
            int eq = arg.indexOf('=');
            need(eq > 0, "transactions [month=YYYY-MM] [category=<id>] [merchant=<text>]");
            String key = arg.substring(0, eq).toLowerCase();
            String value = arg.substring(eq + 1);
            switch (key) {
                case "month" -> {
                    YearMonth m = YearMonth.parse(value);
                    filter = new TxFilter(m.atDay(1), m.atEndOfMonth(), filter.category(), filter.merchant());
                }
                case "category" -> filter = filter.withCategory(value.equalsIgnoreCase("uncategorized")
                        ? Category.UNCATEGORIZED : category(value));
                case "merchant" -> filter = filter.withMerchant(value);
                default -> throw new UsageException("transactions [month=YYYY-MM] [category=<id>] [merchant=<text>]");
            }
        }
        List<Transaction> list = facade.listTransactions(filter);
        if (list.isEmpty()) {
            out.println("No transactions.");
            return;
        }
        out.printf("%-9s %-10s %-26s %-14s %10s%n", "ID", "Date", "Merchant", "Category", "Amount");
        for (Transaction tx : list) {
            out.printf("%-9s %-10s %-26s %-14s %10s%n", shortId(tx.id()), tx.date(), cut(tx.merchant(), 26),
                    tx.category().name(), tx.signedAmount());
        }
        out.println(list.size() + " transactions.");
    }

    private void categories() {
        for (Category c : Categories.defaults()) {
            out.printf("%-14s %s%s%n", c.id(), c.name(), c.essential() ? " (essential)" : "");
        }
    }

    private void correct(List<String> args) {
        need(args.size() == 2, "correct <txId> <categoryId>");
        Category chosen = category(args.get(1));
        Transaction tx = facade.correctCategory(resolveId(args.get(0)), chosen);
        out.println(tx.merchant() + " is now " + tx.category().name()
                + ". Future imports of this merchant use it too. Type 'undo' to reverse.");
    }

    private void undo() {
        Optional<Transaction> tx = facade.undoCorrection();
        out.println(tx.map(t -> "Undone: " + t.merchant() + " is back to " + t.category().name())
                .orElse("Nothing to undo."));
    }

    private void redo() {
        Optional<Transaction> tx = facade.redoCorrection();
        out.println(tx.map(t -> "Redone: " + t.merchant() + " is " + t.category().name())
                .orElse("Nothing to redo."));
    }

    private void budget(List<String> args) {
        need(!args.isEmpty(), "budget set|status ...");
        switch (args.get(0).toLowerCase()) {
            case "set" -> {
                need(args.size() >= 3, "budget set <YYYY-MM> <categoryId>=<amount> ...");
                Map<Category, Money> limits = new LinkedHashMap<>();
                for (String pair : args.subList(2, args.size())) {
                    int eq = pair.indexOf('=');
                    need(eq > 0, "budget set <YYYY-MM> <categoryId>=<amount> ...");
                    limits.put(category(pair.substring(0, eq)), Money.of(pair.substring(eq + 1)));
                }
                printBudget(facade.setBudget(YearMonth.parse(args.get(1)), new BudgetLimits(limits)));
            }
            case "status" -> {
                need(args.size() == 2, "budget status <YYYY-MM>");
                printBudget(facade.getBudgetStatus(YearMonth.parse(args.get(1))));
            }
            default -> throw new UsageException("budget set|status ...");
        }
    }

    private void printBudget(BudgetStatus status) {
        out.println("Budget for " + status.month());
        if (status.lines().isEmpty()) {
            out.println("  (no limits set)");
            return;
        }
        for (BudgetLineStatus line : status.lines()) {
            out.printf("  %-14s %9s of %9s  %4.0f%%  %s%n", line.category().name(), line.spent(), line.limit(),
                    line.ratio() * 100, line.level());
        }
        out.printf("  %-14s %9s of %9s%n", "Total", status.totalSpent(), status.totalLimit());
    }

    private void plan(List<String> args) {
        need(!args.isEmpty(), "plan show|balance|buffer|income|bill|expense ...");
        switch (args.get(0).toLowerCase()) {
            case "show" -> printPlan();
            case "balance" -> {
                need(args.size() == 2 || args.size() == 3, "plan balance <amount> [YYYY-MM-DD]");
                LocalDate asOf = args.size() == 3 ? LocalDate.parse(args.get(2)) : today.get();
                facade.setBalance(Money.of(args.get(1)), asOf);
                out.println("Balance set.");
            }
            case "buffer" -> {
                need(args.size() == 2, "plan buffer <amount>");
                facade.setSafetyBuffer(Money.of(args.get(1)));
                out.println("Safety buffer set.");
            }
            case "income" -> {
                need(args.size() == 5, "plan income <name> <amount> <frequency> <nextPayDate>");
                facade.addIncome(new IncomeSource(args.get(1), Money.of(args.get(2)), frequency(args.get(3)),
                        LocalDate.parse(args.get(4))));
                out.println("Income added.");
            }
            case "bill" -> {
                need(args.size() == 5 || args.size() == 6, "plan bill <name> <amount> <frequency> <nextDueDate> [categoryId]");
                Category c = args.size() == 6 ? category(args.get(5)) : null;
                facade.addFixedBill(new FixedBill(args.get(1), Money.of(args.get(2)), frequency(args.get(3)),
                        LocalDate.parse(args.get(4)), c));
                out.println("Bill added.");
            }
            case "expense" -> {
                need(args.size() == 4 || args.size() == 5, "plan expense <description> <amount> <date> [categoryId]");
                Category c = args.size() == 5 ? category(args.get(4)) : null;
                facade.addPlannedExpense(new PlannedExpense(args.get(1), Money.of(args.get(2)),
                        LocalDate.parse(args.get(3)), c));
                out.println("Planned expense added.");
            }
            default -> throw new UsageException("plan show|balance|buffer|income|bill|expense ...");
        }
    }

    private void recurring(List<String> args) {
        if (args.isEmpty()) {
            printRecurring(facade.recurringPayments(today.get()));
            return;
        }
        String action = args.get(0).toLowerCase();
        need((action.equals("flag") || action.equals("unflag")) && args.size() == 2,
                "recurring [flag|unflag <merchant>]");
        boolean flag = action.equals("flag");
        facade.flagRecurring(args.get(1), flag, today.get());
        out.println(args.get(1) + (flag ? " flagged for review." : " is no longer flagged."));
    }

    private void safe(List<String> args) {
        need(args.size() <= 1, "safe [YYYY-MM-DD]");
        LocalDate date = args.isEmpty() ? today.get() : LocalDate.parse(args.get(0));
        SafeToSpendResult r = facade.getSafeToSpend(date);
        if (r.isOverCommitted()) {
            out.println("Safe to spend on " + date + ": 0.00 per day (over-committed by " + r.overCommittedBy() + ")");
        } else {
            out.println("Safe to spend on " + date + ": " + r.dailyAllowance() + " per day");
        }
        out.println("  " + r.remainingThisMonth() + " left this month over " + r.daysLeft() + " days");
        out.println("  balance " + r.balance() + " + income " + r.expectedIncome() + " - bills " + r.upcomingBills()
                + " - goals " + r.goalReserve() + " - buffer " + r.safetyBuffer());
        out.println(r.explanation());
    }

    private void afford(List<String> args) {
        need(args.size() == 2 || args.size() == 3, "afford \"<item>\" <price> [YYYY-MM-DD]");
        LocalDate date = args.size() == 3 ? LocalDate.parse(args.get(2)) : null;
        wealthsei.agent.AgentResult r = facade.canIAfford(args.get(0), Money.of(args.get(1)), date);
        if (r.verdict() != null) {
            out.println("Verdict: " + r.verdict().label());
        }
        out.println(r.answer());
        if (r.status() != wealthsei.agent.AgentStatus.COMPLETED) {
            out.println("[" + r.status() + "]");
        }
        out.println("(How did you get this? type: trace " + r.traceId() + ")");
    }

    private static final java.util.Map<String, String> WHATIF_KEYS = java.util.Map.of("cancel", "cancel", "caps", "caps",
            "income", "income_increase", "buy", "one_time_expenses");

    private void whatif(List<String> args) {
        need(!args.isEmpty(), "whatif <text>   or   whatif cancel=Spotify caps=dining=150 income=200 buy=600@2026-12-01");
        java.util.Map<String, String> fields = new java.util.LinkedHashMap<>();
        boolean structured = true;
        for (String arg : args) {
            int eq = arg.indexOf('=');
            String key = eq > 0 ? WHATIF_KEYS.get(arg.substring(0, eq).toLowerCase()) : null;
            if (key == null) {
                structured = false;
                break;
            }
            fields.merge(key, arg.substring(eq + 1), (a, b) -> a + "," + b);
        }
        if (!structured) {
            wealthsei.agent.AgentResult r = facade.runWhatIf(String.join(" ", args));
            out.println(r.answer());
            if (r.status() != wealthsei.agent.AgentStatus.COMPLETED) {
                out.println("[" + r.status() + "]");
            }
            out.println("(How did you get this? type: trace " + r.traceId() + ")");
            lastDrafts = r.drafts();
            lastDraftsTrace = r.traceId();
            hintProposals();
            return;
        }
        printScenario(facade.simulateWhatIf(fields, today.get()));
        lastDrafts = facade.whatIfDrafts(fields, today.get());
        lastDraftsTrace = "";
        hintProposals();
    }

    private void export(List<String> args) {
        need(args.size() == 3 || args.size() == 4, "export <YYYY-MM> <md|csv|txt> <file> [overwrite]");
        boolean overwrite = args.size() == 4;
        if (overwrite && !args.get(3).equalsIgnoreCase("overwrite")) {
            throw new UsageException("export <YYYY-MM> <md|csv|txt> <file> [overwrite]");
        }
        java.nio.file.Path written = facade.exportReport(YearMonth.parse(args.get(0)),
                wealthsei.domain.ExportFormat.parse(args.get(1)), Path.of(args.get(2)), overwrite);
        out.println("Report saved to " + written.toAbsolutePath());
    }

    private void hintProposals() {
        if (!lastDrafts.isEmpty()) {
            out.println(lastDrafts.size() + " change(s) from this what-if can become proposals: type 'proposals from-whatif'");
        }
    }

    private void proposals(List<String> args) {
        String sub = args.isEmpty() ? "list" : args.get(0).toLowerCase();
        switch (sub) {
            case "list" -> printProposals(facade.listPendingProposals(today.get()), "No proposals waiting for you.");
            case "all" -> printProposals(facade.listAllProposals(), "No proposals yet.");
            case "from-whatif" -> {
                need(args.size() == 1, "proposals from-whatif");
                if (lastDrafts.isEmpty()) {
                    out.println("Nothing to turn into proposals: run a 'whatif' with a cap or a cancel first.");
                    return;
                }
                reportCreated(facade.createProposals(lastDrafts, lastDraftsTrace));
                lastDrafts = List.of();
            }
            case "from-goal" -> {
                need(args.size() == 2, "proposals from-goal <goalId>");
                reportCreated(facade.proposeGoalFix(args.get(1), today.get()));
            }
            case "approve", "reject" -> {
                need(args.size() == 2, "proposals " + sub + " <id>");
                wealthsei.command.Proposal p = facade.decideProposal(args.get(1), sub.equals("approve"));
                out.println(p.statusName() + ": " + p.title());
                if (sub.equals("approve")) {
                    out.println("  Done: " + p.command().describe() + ". Type 'proposals undo' to reverse it.");
                }
            }
            case "undo" -> {
                need(args.size() == 1, "proposals undo");
                out.println(facade.undoLastProposal()
                        .map(p -> "Undone: " + p.title() + " (" + p.command().describe() + ")")
                        .orElse("Nothing to undo."));
            }
            case "why" -> {
                need(args.size() == 2, "proposals why <id>");
                wealthsei.command.Proposal p = facade.getProposal(args.get(1));
                out.println(p.title() + " [" + p.statusName() + "]");
                out.println("  Why: " + p.rationale());
                out.println("  What it does: " + p.command().describe());
                if (!p.traceId().isEmpty()) {
                    out.println("  How the AI got there: type: trace " + p.traceId());
                }
            }
            default -> throw new UsageException("proposals list|all|from-whatif|from-goal|approve|reject|undo|why");
        }
    }

    private void reportCreated(List<wealthsei.command.Proposal> created) {
        created.forEach(p -> out.println("Proposal " + p.id() + ": " + p.title()));
        facade.lastDiscardedDrafts().forEach(reason -> out.println("  Left out: " + reason));
        if (!created.isEmpty()) {
            out.println("Review with 'proposals', then 'proposals approve <id>' or 'proposals reject <id>'.");
        }
    }

    private void printProposals(List<wealthsei.command.Proposal> list, String emptyMessage) {
        if (list.isEmpty()) {
            out.println(emptyMessage);
            return;
        }
        out.printf("%-9s %-9s %s%n", "ID", "Status", "Proposal");
        list.forEach(p -> out.printf("%-9s %-9s %s%n", p.id(), p.statusName(), p.title()));
        out.println("Type 'proposals why <id>' for the reason, or 'proposals approve <id>'.");
    }

    private void review(List<String> args) {
        need(args.size() <= 1, "review [YYYY-MM]   (default: last month)");
        java.time.YearMonth month = args.isEmpty() ? java.time.YearMonth.from(today.get()).minusMonths(1)
                : java.time.YearMonth.parse(args.get(0));
        wealthsei.service.ReviewResult r = facade.generateMonthlyReview(month);
        if (r.replacedEarlier()) {
            out.println("(Replaced the earlier review of " + month + ".)");
        }
        out.println("Monthly review for " + month);
        out.println(r.review().narrative());
        if (r.review().healthScore().isAvailable()) {
            out.println("Health score: " + r.review().healthScore().total() + " / 100");
        }
        if (r.metricsOnly()) {
            out.println("[Numbers only] " + r.note());
        }
        if (r.proposals().isEmpty() && r.alreadyWaiting() > 0) {
            out.println(r.alreadyWaiting() + " suggested fix(es) are already waiting for your approval: type 'proposals'.");
        } else if (r.proposals().isEmpty()) {
            out.println("No fixes to suggest this time.");
        } else {
            out.println("Suggested fixes (waiting for your approval):");
            r.proposals().forEach(p -> out.printf("  %-9s %s%n", p.id(), p.title()));
            out.println("Type 'proposals why <id>' for the reason, or 'proposals approve <id>'.");
        }
        if (!r.review().traceId().isEmpty()) {
            out.println("(How did you get this? type: trace " + r.review().traceId() + ")");
        }
    }

    private void ask(List<String> args) {
        need(!args.isEmpty(), "ask \"<question>\"   for example: ask \"how much did I spend on dining in 2026-10?\"");
        wealthsei.agent.AgentResult r = facade.ask(String.join(" ", args));
        out.println(r.answer());
        if (r.status() != wealthsei.agent.AgentStatus.COMPLETED) {
            out.println("[" + r.status() + "]");
        }
        out.println("(How did you get this? type: trace " + r.traceId() + ")");
    }

    private void forget() {
        facade.clearConversation();
        out.println("Started a new conversation. Your saved preferences are kept.");
    }

    private void protect(List<String> args, boolean on) {
        if (args.isEmpty()) {
            out.println("Protected categories (the agent may not suggest changing these): "
                    + facade.protectedCategories().stream().map(Category::name).sorted().toList());
            return;
        }
        need(args.size() == 1, on ? "protect <category>" : "unprotect <category>");
        Category c = category(args.get(0));
        facade.setProtectedCategory(c, on);
        out.println(c.name() + (on ? " is now protected." : " is no longer protected."));
    }

    private List<wealthsei.agent.ProposalDraft> lastDrafts = List.of();
    private String lastDraftsTrace = "";

    private void printScenario(wealthsei.analytics.ScenarioResult r) {
        out.println("What-if over the next " + r.months() + " months:");
        r.changes().forEach(c -> out.println("  - " + c));
        out.printf("%-26s %12s %12s %12s%n", "", "Baseline", "Scenario", "Change");
        out.printf("%-26s %12s %12s %12s%n", "End balance", r.baselineEndBalance(), r.scenarioEndBalance(), r.endBalanceDelta());
        out.printf("%-26s %12s %12s %12s%n", "Lowest balance", r.baselineLowest(), r.scenarioLowest(),
                r.scenarioLowest().subtract(r.baselineLowest()));
        out.printf("%-26s %12s %12s %12s%n", "Money left per month", r.baselineMonthlySavings(), r.scenarioMonthlySavings(),
                r.monthlySavingsDelta());
        if (r.typicalMonthlySpending().isPositive()) {
            out.println("  (Both columns include your typical everyday spending of " + r.typicalMonthlySpending()
                    + " a month, from your past full months.)");
        }
        for (wealthsei.analytics.ScenarioResult.GoalImpact g : r.goalImpacts()) {
            out.printf("%-26s %12s %12s%n", "Goal '" + g.goalName() + "' reached",
                    g.baselineCompletion() == null ? "not reached" : g.baselineCompletion(),
                    g.scenarioCompletion() == null ? "not reached" : g.scenarioCompletion());
        }
    }

    private void trace(List<String> args) {
        need(args.size() <= 1, "trace [id]");
        java.util.Optional<wealthsei.agent.AgentTrace> found = args.isEmpty() ? facade.latestTrace()
                : facade.getTrace(args.get(0));
        if (found.isEmpty()) {
            out.println(args.isEmpty() ? "Nothing to show yet: ask a question first (afford ...)."
                    : "No trace with id '" + args.get(0) + "'.");
            return;
        }
        wealthsei.agent.AgentTrace t = found.get();
        out.println("Trace " + t.id() + " - " + t.question());
        for (wealthsei.agent.TraceStep s : t.steps()) {
            String what = s.kind().equals(wealthsei.agent.TraceStep.TOOL_CALL)
                    ? s.toolName() + " " + s.argsJson() + " -> " + s.resultSummary() : s.resultSummary();
            out.println("  " + s.index() + ". " + s.kind() + ": " + what);
        }
    }

    private void goal(List<String> args) {
        need(!args.isEmpty(), "goal add|list|replan|save|monthly ...");
        String action = args.get(0).toLowerCase();
        switch (action) {
            case "add" -> {
                need(args.size() == 4 || args.size() == 5, "goal add \"<name>\" <target> <YYYY-MM-DD> [saved]");
                Money saved = args.size() == 5 ? Money.of(args.get(4)) : Money.ZERO;
                wealthsei.domain.SavingsGoal g = facade.addGoal(new wealthsei.domain.GoalRequest(args.get(1),
                        Money.of(args.get(2)), LocalDate.parse(args.get(3)), saved), today.get());
                out.println("Goal added (" + shortId(g.id()) + "): " + g.name() + ", " + g.status());
            }
            case "list" -> {
                java.util.List<wealthsei.domain.SavingsGoal> all = facade.getGoals();
                if (all.isEmpty()) {
                    out.println("No goals yet. Add one: goal add \"Laptop\" 1500 2027-06-01");
                    return;
                }
                out.printf("%-8s %-18s %10s %10s %-10s %9s  %s%n", "Id", "Goal", "Target", "Saved", "Deadline",
                        "Monthly", "Status");
                for (wealthsei.domain.SavingsGoal g : all) {
                    out.printf("%-8s %-18s %10s %10s %-10s %9s  %s%n", shortId(g.id()), g.name(), g.target(),
                            g.saved(), g.deadline(), g.monthlyContribution(), g.status());
                }
            }
            case "replan" -> {
                need(args.size() == 2, "goal replan <id>");
                wealthsei.domain.FeasibilityReport r = facade.getGoalFeasibility(args.get(1), today.get());
                out.println("Needs " + r.requiredMonthly() + " per month; available after bills and other goals: "
                        + r.monthlySurplus() + ".");
                out.println(r.feasible() ? "This goal can be reached on time." : "This goal will not be reached on time.");
                for (wealthsei.domain.GoalOption o : r.options()) {
                    out.println("  - " + o.description() + " -> " + o.impact());
                }
            }
            case "save" -> {
                need(args.size() == 3, "goal save <id> <amount>");
                wealthsei.domain.SavingsGoal g = facade.addToGoal(args.get(1), Money.of(args.get(2)), today.get());
                out.println(g.name() + ": " + g.saved() + " of " + g.target() + " saved (" + g.status() + ")");
            }
            case "monthly" -> {
                need(args.size() == 3, "goal monthly <id> <amount>");
                wealthsei.domain.SavingsGoal g = facade.setGoalMonthlyContribution(args.get(1),
                        Money.of(args.get(2)), today.get());
                out.println(g.name() + ": putting aside " + g.monthlyContribution() + " per month (" + g.status() + ")");
            }
            default -> need(false, "goal add|list|replan|save|monthly ...");
        }
    }

    private void habits(List<String> args) {
        need(args.size() == 1, "habits <YYYY-MM>");
        java.time.YearMonth month = java.time.YearMonth.parse(args.get(0));
        java.util.List<wealthsei.domain.Insight> found = facade.getHabits(month);
        if (found.isEmpty()) {
            out.println("No notable patterns in " + month + ".");
            return;
        }
        for (wealthsei.domain.Insight i : found) {
            out.println("[" + i.severity() + "] " + i.title());
            out.println("  " + i.evidence());
            out.println("  " + i.explanation());
        }
    }

    private void score(List<String> args) {
        need(args.size() <= 1, "score [YYYY-MM]");
        java.time.YearMonth month = args.isEmpty() ? java.time.YearMonth.from(today.get())
                : java.time.YearMonth.parse(args.get(0));
        wealthsei.domain.HealthScore s = facade.getHealthScore(month, today.get());
        if (!s.isAvailable()) {
            out.println("Health score: N/A");
        } else {
            out.println("Health score for " + month + ": " + s.total() + " / 100");
            for (wealthsei.domain.ScoreComponent c : s.components()) {
                out.printf("  %-18s %2d / %2d  %s%n", c.name(), c.points(), c.maxPoints(), c.detail());
            }
            for (String e : s.excluded()) {
                out.println("  (left out) " + e);
            }
        }
        out.println(s.explanation());
    }

    private void forecast(List<String> args) {
        need(args.size() <= 1, "forecast [days]");
        int days = args.isEmpty() ? 30 : Integer.parseInt(args.get(0));
        need(days >= 1 && days <= 365, "forecast [days from 1 to 365]");
        Forecast f = facade.getForecast(days, today.get());
        out.printf("%-10s %10s %10s %10s%n", "Date", "Balance", "In", "Out");
        for (DailyBalance d : f.days()) {
            if (d.hasActivity() || d == f.days().get(0)) {
                out.printf("%-10s %10s %10s %10s%n", d.date(), d.balance(), d.income(), d.outgoing());
            }
        }
        DailyBalance low = f.lowest();
        out.println("Lowest balance " + low.balance() + " on " + low.date() + ".");
    }

    private void printRecurring(RecurringReport report) {
        if (!report.payments().isEmpty()) {
            out.printf("%-24s %9s %-9s %-10s %10s  %s%n", "Merchant", "Amount", "Every", "Next due", "Confidence", "Notes");
            for (RecurringPayment p : report.payments()) {
                List<String> notes = new java.util.ArrayList<>();
                if (p.hasPriceIncrease()) {
                    notes.add("price up " + p.priceIncrease());
                }
                if (p.isPossible()) {
                    notes.add("possible");
                }
                if (p.flaggedForReview()) {
                    notes.add("FLAGGED");
                }
                out.printf("%-24s %9s %-9s %-10s %9.0f%%  %s%n", cut(p.merchant(), 24), p.amount(),
                        p.frequency().name().toLowerCase(), p.nextDue(), p.confidence() * 100,
                        String.join(", ", notes));
            }
            out.println("About " + report.monthlyTotal() + " per month.");
        }
        out.println(report.summary());
    }

    private void printPlan() {
        Optional<FinancialPlan> maybe = facade.getPlan();
        if (maybe.isEmpty()) {
            out.println("No plan yet. Start with: plan balance <amount>");
            return;
        }
        FinancialPlan p = maybe.get();
        out.println("Balance " + p.currentBalance() + " as of " + p.balanceAsOf()
                + ", safety buffer " + p.safetyBuffer());
        out.println("Income (" + p.monthlyIncome() + " per month):");
        p.incomeSources().forEach(i -> out.println("  " + i.name() + " " + i.amount() + " "
                + i.frequency().name().toLowerCase() + ", next " + i.nextPayDate()));
        out.println("Fixed bills (" + p.monthlyFixedBills() + " per month):");
        p.fixedBills().forEach(b -> out.println("  " + b.name() + " " + b.amount() + " "
                + b.frequency().name().toLowerCase() + ", next " + b.nextDueDate()));
        out.println("Planned expenses:");
        p.plannedExpenses().forEach(e -> out.println("  " + e.description() + " " + e.amount() + " on " + e.date()));
    }

    // ---- helpers ----

    private void printAlert(Alert alert) {
        out.println("ALERT [" + alert.severity() + "] " + alert.message());
    }

    /** The transaction ids are long, so the table shows the first 8 characters. */
    private static String shortId(String id) {
        return id.length() <= 8 ? id : id.substring(0, 8);
    }

    /** Accepts a full id or any prefix that matches exactly one transaction. */
    private String resolveId(String typed) {
        List<String> matches = facade.listTransactions(TxFilter.all()).stream()
                .map(Transaction::id)
                .filter(id -> id.startsWith(typed))
                .toList();
        if (matches.size() == 1) {
            return matches.get(0);
        }
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("No transaction with id '" + typed + "'. Type 'transactions' to see ids.");
        }
        throw new IllegalArgumentException("'" + typed + "' matches " + matches.size() + " transactions; type more characters.");
    }

    private static Category category(String id) {
        return Categories.find(id).orElseThrow(() -> new IllegalArgumentException("Unknown category '" + id
                + "'. Type 'categories' to see the ids."));
    }

    private static Frequency frequency(String text) {
        try {
            return Frequency.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown frequency '" + text + "'. Use weekly, biweekly, monthly or yearly.");
        }
    }

    private static String cut(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "~";
    }

    private static void need(boolean ok, String usage) {
        if (!ok) {
            throw new UsageException(usage);
        }
    }

    /** The command was typed with the wrong arguments. */
    private static final class UsageException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        UsageException(String usage) {
            super(usage);
        }
    }
}
