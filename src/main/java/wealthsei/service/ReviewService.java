package wealthsei.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import wealthsei.agent.AgentController;
import wealthsei.agent.AgentResult;
import wealthsei.agent.AgentStatus;
import wealthsei.agent.AgentTask;
import wealthsei.agent.MemoryManager;
import wealthsei.agent.ParamSet;
import wealthsei.agent.ProposalDraft;
import wealthsei.agent.TaskParams;
import wealthsei.agent.TaskType;
import wealthsei.analytics.AnalyticsService;
import wealthsei.command.Proposal;
import wealthsei.command.ProposalType;
import wealthsei.domain.BudgetLineStatus;
import wealthsei.domain.BudgetStatus;
import wealthsei.domain.Category;
import wealthsei.domain.GoalStatus;
import wealthsei.domain.HealthScore;
import wealthsei.domain.Insight;
import wealthsei.domain.Money;
import wealthsei.domain.MonthlyReview;
import wealthsei.domain.RecurringPayment;
import wealthsei.domain.SavingsGoal;
import wealthsei.domain.ScoreComponent;
import wealthsei.domain.Transaction;
import wealthsei.domain.TxFilter;
import wealthsei.repository.ReviewRepository;

/**
 * Makes the monthly review: gathers the month's numbers, asks the agent to write what happened, what
 * went well and what to fix, turns up to three fixes into proposals waiting for approval, and saves
 * the result. If the agent cannot be used the review still exists, as a fixed summary of the numbers.
 */
public class ReviewService {

    static final int MAX_FIXES = 3;

    private final TransactionService transactions;
    private final AnalyticsService analytics;
    private final BudgetService budgets;
    private final GoalService goals;
    private final AgentController agent;
    private final ProposalService proposals;
    private final ReviewRepository reviews;
    private final MemoryManager memory;
    private final Supplier<LocalDate> today;
    private final Supplier<Instant> clock;

    public ReviewService(TransactionService transactions, AnalyticsService analytics, BudgetService budgets,
                         GoalService goals, AgentController agent, ProposalService proposals,
                         ReviewRepository reviews, MemoryManager memory, Supplier<LocalDate> today,
                         Supplier<Instant> clock) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.analytics = Objects.requireNonNull(analytics, "analytics");
        this.budgets = Objects.requireNonNull(budgets, "budgets");
        this.goals = Objects.requireNonNull(goals, "goals");
        this.agent = Objects.requireNonNull(agent, "agent");
        this.proposals = Objects.requireNonNull(proposals, "proposals");
        this.reviews = Objects.requireNonNull(reviews, "reviews");
        this.memory = memory;
        this.today = Objects.requireNonNull(today, "today");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Generates (or regenerates) the review for {@code month}.
     *
     * @throws ValidationException if the month is in the future or has no transactions
     */
    public ReviewResult generate(YearMonth month) {
        Objects.requireNonNull(month, "month");
        if (month.isAfter(YearMonth.from(today.get()))) {
            throw new ValidationException("Cannot review " + month + " yet: it has not started");
        }
        List<Transaction> monthTx = transactions.query(TxFilter.between(month.atDay(1), month.atEndOfMonth()));
        if (monthTx.isEmpty()) {
            throw new ValidationException("There are no transactions in " + month + " to review. Import them first.");
        }
        boolean replaced = reviews.findByMonth(month).isPresent();
        BudgetStatus status = budgets.hasBudget(month) ? budgets.getStatus(month) : null;
        HealthScore score = analytics.healthScore(status, today.get());

        List<String> previous = previousSummaries(month);
        String[] params = previous.isEmpty()
                ? new String[] {"month", month.toString()}
                : new String[] {"month", month.toString(), "previous", String.join(" | ", previous)};
        AgentResult result = agent.handle(new AgentTask(TaskType.MONTHLY_REVIEW,
                "Write the monthly review for " + month, TaskParams.of(params)));

        boolean metricsOnly = result.status() != AgentStatus.COMPLETED;
        String narrative = metricsOnly ? metricsNarrative(month, monthTx, status, score) : result.answer();
        String note = metricsOnly ? noteFor(result) : "";

        List<ProposalDraft> candidates = candidateFixes();
        List<String> waiting = proposals.listPending().stream().map(Proposal::title).toList();
        List<ProposalDraft> fixes = candidates.stream().filter(d -> !waiting.contains(d.title())).limit(MAX_FIXES).toList();
        int alreadyWaiting = (int) candidates.stream().filter(d -> waiting.contains(d.title())).count();
        List<Proposal> created = proposals.createFromDrafts(fixes, result.traceId());

        MonthlyReview review = new MonthlyReview(month, score, narrative,
                created.stream().map(Proposal::id).toList(), result.traceId(), clock.get());
        reviews.save(review);
        if (memory != null) {
            memory.rememberReview(month + ": score " + (score.isAvailable() ? score.total() + "/100" : "n/a") + ". "
                    + firstSentences(narrative, 220));
        }
        return new ReviewResult(review, created, metricsOnly, replaced, alreadyWaiting, note);
    }

    public java.util.Optional<MonthlyReview> find(YearMonth month) {
        return reviews.findByMonth(month);
    }

    // ---- fixes ----

    /** Every fix the code can back with numbers, before ones already waiting for approval are left out. */
    private List<ProposalDraft> candidateFixes() {
        LocalDate now = today.get();
        List<ProposalDraft> drafts = new ArrayList<>();
        for (SavingsGoal goal : goals.goals()) {
            GoalStatus s = goals.statusOf(goal, now);
            if (s != GoalStatus.BEHIND && s != GoalStatus.AT_RISK) {
                continue;
            }
            Money required = goals.feasibility(goal.id(), now).requiredMonthly();
            if (required.compareTo(goal.monthlyContribution()) <= 0) {
                continue;
            }
            drafts.add(new ProposalDraft(ProposalType.GOAL_ADJUSTMENT,
                    "Put " + required + " a month toward " + goal.name(),
                    "It needs " + required + " a month to be reached by " + goal.deadline() + "; you plan "
                            + goal.monthlyContribution() + ".",
                    ParamSet.of("goalId", goal.id(), "monthly", required.toString())));
        }
        for (RecurringPayment p : analytics.detectRecurring(now)) {
            if (p.priceIncrease().isPositive() && !p.flaggedForReview()) {
                drafts.add(new ProposalDraft(ProposalType.FLAG_SUBSCRIPTION,
                        "Review the " + p.merchant() + " payment",
                        p.merchant() + " went up by " + p.priceIncrease() + " on its latest payment.",
                        ParamSet.of("merchant", p.merchant())));
            }
        }
        return drafts;
    }

    // ---- text ----

    private List<String> previousSummaries(YearMonth month) {
        if (memory == null) {
            return List.of();
        }
        return memory.pastReviewSummaries().stream().filter(s -> !s.startsWith(month + ":")).toList();
    }

    private static String noteFor(AgentResult r) {
        return r.status() == AgentStatus.MODEL_UNAVAILABLE
                ? "The AI model was not available, so this is a summary of the numbers only."
                : "The AI's write-up could not be fully checked against the numbers, so this is a summary of the numbers only.";
    }

    static String firstSentences(String text, int maxLength) {
        String flat = text.replaceAll("\\s+", " ").trim();
        if (flat.length() <= maxLength) {
            return flat;
        }
        int cut = flat.lastIndexOf(". ", maxLength);
        return cut > 40 ? flat.substring(0, cut + 1) : flat.substring(0, maxLength).trim() + "...";
    }

    /** The fixed, number-only review used when the AI cannot write one. */
    private String metricsNarrative(YearMonth month, List<Transaction> monthTx, BudgetStatus status, HealthScore score) {
        String name = month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear();
        Money spent = Money.ZERO;
        Money received = Money.ZERO;
        Map<Category, Money> byCategory = new LinkedHashMap<>();
        int expenses = 0;
        for (Transaction t : monthTx) {
            if (t.isExpense()) {
                spent = spent.add(t.amount());
                expenses++;
                byCategory.merge(t.category(), t.amount(), Money::add);
            } else {
                received = received.add(t.amount());
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("What happened: in ").append(name).append(" you spent ").append(spent).append(" across ")
                .append(expenses).append(" purchases and received ").append(received).append(".");
        List<Map.Entry<Category, Money>> top = byCategory.entrySet().stream()
                .sorted(Map.Entry.<Category, Money>comparingByValue(Comparator.reverseOrder())).limit(3).toList();
        if (!top.isEmpty()) {
            sb.append(" The biggest categories were ");
            for (int i = 0; i < top.size(); i++) {
                sb.append(i > 0 ? (i == top.size() - 1 ? " and " : ", ") : "")
                        .append(top.get(i).getKey().name()).append(" (").append(top.get(i).getValue()).append(")");
            }
            sb.append(".");
        }
        sb.append("\nWhat went well: ");
        List<String> good = new ArrayList<>();
        if (score.isAvailable()) {
            score.components().stream().filter(c -> c.points() == c.maxPoints())
                    .forEach(c -> good.add(c.name() + " is at full marks"));
        }
        if (status != null) {
            long within = status.lines().stream().filter(l -> l.level() == wealthsei.domain.AlertSeverity.INFO).count();
            if (within > 0) {
                good.add(within + " budget categories stayed under 80% of their limit");
            }
        }
        sb.append(good.isEmpty() ? "nothing stands out from the numbers alone." : String.join("; ", good) + ".");
        sb.append("\nWhat to fix: ");
        List<String> fix = new ArrayList<>();
        if (status != null) {
            for (BudgetLineStatus l : status.lines()) {
                if (l.level() == wealthsei.domain.AlertSeverity.OVER) {
                    fix.add(l.category().name() + " went over its limit (" + l.spent() + " of " + l.limit() + ")");
                }
            }
        }
        if (score.isAvailable()) {
            score.components().stream().filter(c -> c.points() < c.maxPoints() / 2)
                    .sorted(Comparator.comparingInt(ScoreComponent::points))
                    .forEach(c -> fix.add(c.name() + " scored " + c.points() + " of " + c.maxPoints()));
        }
        try {
            for (Insight i : analytics.habits(month)) {
                if (fix.size() >= 5) {
                    break;
                }
                fix.add(i.title());
            }
        } catch (ValidationException e) {
            // not enough history for habits; leave them out
        }
        sb.append(fix.isEmpty() ? "no problems found in the numbers." : String.join("; ", fix) + ".");
        if (score.isAvailable()) {
            sb.append("\nHealth score: ").append(score.total()).append(" / 100.");
        }
        return sb.toString();
    }
}
