package wealthsei.analytics;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import wealthsei.domain.AlertSeverity;
import wealthsei.domain.Category;
import wealthsei.domain.Insight;
import wealthsei.domain.InsightType;
import wealthsei.domain.Money;
import wealthsei.domain.Transaction;

/**
 * Finds spending habits with plain statistics (no AI). Weekend, payday, month-end and
 * small-purchase rules look only at discretionary spending (categories that are not essential),
 * so rent and groceries do not hide the habits. Each insight carries its evidence.
 *
 * <p>Rules (all thresholds are constants below):
 * <ul>
 *   <li>Weekend spike: weekend days average at least 1.5 times a weekday, and at least 50 was spent on weekends.</li>
 *   <li>Payday splurge: the 3 days from the month's largest income are at least 2 times a usual day, and at least 50.</li>
 *   <li>Month-end crunch: the last 7 days average at least 1.5 times the earlier days, and at least 50.</li>
 *   <li>Small-purchase leak: at least 8 purchases under 15 that add up to at least 75.</li>
 *   <li>Unusual expense: above the category's mean + 2 standard deviations (from at least 4 other purchases).</li>
 * </ul>
 */
public class HabitAnalyzer {

    static final double WEEKEND_RATIO = 1.5;
    static final double PAYDAY_RATIO = 2.0;
    static final double MONTH_END_RATIO = 1.5;
    static final double MIN_SPIKE_SPEND = 50.0;
    static final Money SMALL_LIMIT = Money.of("15");
    static final int MIN_SMALL_COUNT = 8;
    static final double MIN_SMALL_TOTAL = 75.0;
    static final int MIN_CATEGORY_HISTORY = 4;

    /** Analyses the most recent month that has transactions. */
    public List<Insight> analyze(List<Transaction> txs) {
        Objects.requireNonNull(txs, "txs");
        return txs.stream().map(t -> YearMonth.from(t.date())).max(Comparator.naturalOrder())
                .map(m -> analyze(txs, m)).orElse(List.of());
    }

    /**
     * @param txs   the history (all months up to the one analysed)
     * @param month the month to look at
     */
    public List<Insight> analyze(List<Transaction> txs, YearMonth month) {
        Objects.requireNonNull(txs, "txs");
        Objects.requireNonNull(month, "month");
        List<Transaction> inMonth = txs.stream().filter(t -> YearMonth.from(t.date()).equals(month)).toList();
        if (inMonth.isEmpty()) {
            return List.of();
        }
        LocalDate lastDay = inMonth.stream().map(Transaction::date).max(LocalDate::compareTo).orElseThrow();
        List<Transaction> discretionary = inMonth.stream()
                .filter(Transaction::isExpense).filter(t -> !t.category().essential()).toList();
        List<Insight> found = new ArrayList<>();
        weekendSpike(month, lastDay, discretionary, found);
        paydaySplurge(month, lastDay, inMonth, discretionary, found);
        monthEndCrunch(month, lastDay, discretionary, found);
        smallPurchaseLeak(discretionary, found);
        unusualExpenses(txs, inMonth, found);
        found.sort(Comparator.comparing(Insight::severity).reversed().thenComparing(Insight::type));
        return found;
    }

    private void weekendSpike(YearMonth month, LocalDate lastDay, List<Transaction> discretionary, List<Insight> out) {
        int weekendDays = 0;
        int weekdayDays = 0;
        for (LocalDate d = month.atDay(1); !d.isAfter(lastDay); d = d.plusDays(1)) {
            if (isWeekend(d)) {
                weekendDays++;
            } else {
                weekdayDays++;
            }
        }
        if (weekendDays < 2 || weekdayDays < 5) {
            return;
        }
        double weekend = sum(discretionary, t -> isWeekend(t.date()));
        double weekday = sum(discretionary, t -> !isWeekend(t.date()));
        double weekendAvg = weekend / weekendDays;
        double weekdayAvg = weekday / weekdayDays;
        if (weekend < MIN_SPIKE_SPEND || weekendAvg < WEEKEND_RATIO * weekdayAvg) {
            return;
        }
        double ratio = weekdayAvg == 0 ? Double.POSITIVE_INFINITY : weekendAvg / weekdayAvg;
        String times = Double.isInfinite(ratio) ? "while nothing was spent on weekdays" : String.format("(%.1f times a weekday)", ratio);
        out.add(new Insight(InsightType.WEEKEND_SPIKE, "Weekend spending spike",
                "Weekend days averaged " + money(weekendAvg) + " versus " + money(weekdayAvg) + " on weekdays " + times
                        + "; " + money(weekend) + " in total over " + weekendDays + " weekend days.",
                ratio >= 2.0 ? AlertSeverity.WARNING : AlertSeverity.INFO,
                "A lot of your spending lands on weekends. Planning a weekend budget up front can keep it in check."));
    }

    private void paydaySplurge(YearMonth month, LocalDate lastDay, List<Transaction> inMonth,
                               List<Transaction> discretionary, List<Insight> out) {
        Transaction payday = inMonth.stream().filter(Transaction::income)
                .max(Comparator.comparing(Transaction::amount)).orElse(null);
        if (payday == null) {
            return;
        }
        LocalDate start = payday.date();
        LocalDate end = start.plusDays(2);
        if (end.isAfter(lastDay)) {
            return;
        }
        int elapsed = lastDay.getDayOfMonth();
        int otherDays = elapsed - 3;
        if (otherDays < 7) {
            return;
        }
        double window = sum(discretionary, t -> !t.date().isBefore(start) && !t.date().isAfter(end));
        double others = sum(discretionary, t -> t.date().isBefore(start) || t.date().isAfter(end)) / otherDays;
        double windowPerDay = window / 3.0;
        if (window < MIN_SPIKE_SPEND || windowPerDay < PAYDAY_RATIO * others) {
            return;
        }
        double ratio = others == 0 ? Double.POSITIVE_INFINITY : windowPerDay / others;
        out.add(new Insight(InsightType.PAYDAY_SPLURGE, "Spending right after payday",
                "You spent " + money(window) + " in the 3 days from your " + payday.date() + " income, "
                        + money(windowPerDay) + " a day against " + money(others) + " on other days"
                        + (Double.isInfinite(ratio) ? "." : String.format(" (%.1f times).", ratio)),
                ratio >= 3.0 ? AlertSeverity.WARNING : AlertSeverity.INFO,
                "Money tends to leave quickly once your pay arrives. Moving savings out on payday can help."));
    }

    private void monthEndCrunch(YearMonth month, LocalDate lastDay, List<Transaction> discretionary, List<Insight> out) {
        LocalDate monthEnd = month.atEndOfMonth();
        if (!lastDay.equals(monthEnd)) {
            return;
        }
        LocalDate crunchStart = monthEnd.minusDays(6);
        int earlierDays = crunchStart.getDayOfMonth() - 1;
        if (earlierDays < 14) {
            return;
        }
        double last7 = sum(discretionary, t -> !t.date().isBefore(crunchStart));
        double earlier = sum(discretionary, t -> t.date().isBefore(crunchStart)) / earlierDays;
        double last7PerDay = last7 / 7.0;
        if (last7 < MIN_SPIKE_SPEND || last7PerDay < MONTH_END_RATIO * earlier) {
            return;
        }
        out.add(new Insight(InsightType.MONTH_END_CRUNCH, "Heavy spending at month end",
                "The last 7 days averaged " + money(last7PerDay) + " a day (" + money(last7) + ") against "
                        + money(earlier) + " a day earlier in the month.",
                AlertSeverity.INFO,
                "Spending picks up at the end of the month, just when the budget has the least room."));
    }

    private void smallPurchaseLeak(List<Transaction> discretionary, List<Insight> out) {
        List<Transaction> small = discretionary.stream().filter(t -> t.amount().isLessThan(SMALL_LIMIT)).toList();
        double total = small.stream().mapToDouble(t -> t.amount().amount().doubleValue()).sum();
        if (small.size() < MIN_SMALL_COUNT || total < MIN_SMALL_TOTAL) {
            return;
        }
        double all = sum(discretionary, t -> true);
        long percent = Math.round(100.0 * total / all);
        out.add(new Insight(InsightType.SMALL_PURCHASE_LEAK, "Small purchases add up",
                small.size() + " purchases under " + SMALL_LIMIT + " came to " + money(total) + " ("
                        + money(total / small.size()) + " each), " + percent + "% of discretionary spending.",
                total >= 2 * MIN_SMALL_TOTAL ? AlertSeverity.WARNING : AlertSeverity.INFO,
                "Many small purchases look harmless one at a time but add up. Cutting a few would free up real money."));
    }

    private void unusualExpenses(List<Transaction> history, List<Transaction> inMonth, List<Insight> out) {
        for (Transaction tx : inMonth) {
            if (!tx.isExpense()) {
                continue;
            }
            Category category = tx.category();
            List<Double> others = history.stream()
                    .filter(t -> t.isExpense() && t.category().equals(category) && !t.id().equals(tx.id())
                            && !t.date().isAfter(tx.date()))
                    .map(t -> t.amount().amount().doubleValue()).toList();
            if (others.size() < MIN_CATEGORY_HISTORY) {
                continue;
            }
            double mean = others.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double variance = others.stream().mapToDouble(v -> (v - mean) * (v - mean)).sum() / others.size();
            double std = Math.sqrt(variance);
            double amount = tx.amount().amount().doubleValue();
            boolean unusual = std == 0 ? amount > 2 * mean : amount > mean + 2 * std;
            if (unusual) {
                out.add(new Insight(InsightType.UNUSUAL_EXPENSE, "Unusual expense: " + category.name(),
                        tx.merchant() + " on " + tx.date() + " was " + tx.amount() + ", while " + category.name()
                                + " purchases usually cost about " + money(mean) + ".",
                        AlertSeverity.WARNING,
                        "This one is much larger than your usual " + category.name().toLowerCase()
                                + " spending. Worth a look in case it was a one-off or a mistake."));
            }
        }
    }

    private static boolean isWeekend(LocalDate d) {
        return d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private static double sum(List<Transaction> txs, java.util.function.Predicate<Transaction> filter) {
        return txs.stream().filter(filter).mapToDouble(t -> t.amount().amount().doubleValue()).sum();
    }

    private static String money(double value) {
        return Money.of(BigDecimal.valueOf(value)).toString();
    }
}
