package wealthsei.presentation.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import wealthsei.domain.AlertSeverity;
import wealthsei.domain.BudgetLineStatus;
import wealthsei.domain.Category;
import wealthsei.domain.DailyBalance;
import wealthsei.domain.GoalStatus;
import wealthsei.domain.Insight;
import wealthsei.domain.Money;
import wealthsei.domain.SavingsGoal;
import wealthsei.facade.WealthSeiFacade;

/** The home screen: balance, health, forecast, safe-to-spend, where the money went, budget, goals and what needs attention. */
@SuppressWarnings({"serial", "this-escape"})
public class DashboardView extends View {

    private static final int GAP = 14;

    private final AppContext ctx;
    private final WealthSeiFacade facade;
    private final Runnable goToProposals;
    private final Runnable goToTransactions;
    private java.time.YearMonth chosenMonth;
    private DashboardData data;

    public DashboardView(AppContext ctx) {
        this.ctx = ctx;
        this.facade = ctx.facade();
        this.goToProposals = () -> ctx.navigate(AppContext.PROPOSALS);
        this.goToTransactions = () -> ctx.navigate(AppContext.TRANSACTIONS);
        refresh();
    }

    @Override
    public String title() {
        return "Dashboard";
    }

    @Override
    public String[] headline() {
        return new String[] {"Your money,", "at a glance."};
    }

    @Override
    public String subtitle() {
        return data.hasData()
                ? "Where your money stands in " + monthName(false) + ". A clear view of what is safe, what changed, and what needs your attention."
                : "Import a bank CSV and this page fills in: balance, safe-to-spend, budget, goals and a 30-day forecast.";
    }

    private String monthName(boolean shortName) {
        return data.month().getMonth().getDisplayName(shortName ? TextStyle.SHORT : TextStyle.FULL, Locale.ENGLISH)
                + (shortName ? "" : " " + data.month().getYear());
    }

    @Override
    public void refresh() {
        data = DashboardData.load(facade, ctx.today(), chosenMonth);
        removeAll();
        add(toolbar(), BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        if (!data.hasData()) {
            grid.add(emptyState(), cell(0, 0, 12));
        } else {
            grid.add(balanceCard(), cell(0, 0, 8));
            grid.add(healthCard(), cell(8, 0, 4));
            grid.add(forecastCard(), cell(0, 1, 8));
            grid.add(safeCard(), cell(8, 1, 4));
            grid.add(spendingCard(), cell(0, 2, 12));
            grid.add(budgetCard(), cell(0, 3, 4));
            grid.add(goalsCard(), cell(4, 3, 4));
            grid.add(recurringCard(), cell(8, 3, 4));
            grid.add(attentionCard(), cell(0, 4, 12));
        }
        add(grid, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private javax.swing.JComponent toolbar() {
        JPanel bar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        bar.setOpaque(false);
        bar.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, GAP / 2, 0));
        List<java.time.YearMonth> months = new ArrayList<>();
        for (wealthsei.domain.Transaction t : facade.listTransactions(wealthsei.domain.TxFilter.all())) {
            java.time.YearMonth m = java.time.YearMonth.from(t.date());
            if (!months.contains(m)) {
                months.add(m);
            }
        }
        months.sort(java.util.Comparator.reverseOrder());
        if (!months.contains(data.month())) {
            months.add(0, data.month());
        }
        List<Menus.Option> options = new ArrayList<>();
        for (java.time.YearMonth m : months) {
            options.add(new Menus.Option(m.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + m.getYear()));
        }
        bar.add(new Dropdown("Month", options, months.indexOf(data.month()), i -> {
            chosenMonth = months.get(i);
            refresh();
        }));
        bar.add(Box.createHorizontalStrut(10));
        FlatButton[] planButton = new FlatButton[1];
        planButton[0] = new FlatButton("Edit plan", null, false,
                () -> Menus.popup(planButton[0], PlanDialogs.planMenu(), -1, i -> PlanDialogs.runPlanChoice(ctx, i)));
        bar.add(planButton[0]);
        return bar;
    }

    private static GridBagConstraints cell(int x, int y, int w) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = w;
        c.weightx = w;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(GAP / 2, GAP / 2, GAP / 2, GAP / 2);
        return c;
    }

    // ---- top row ----

    private javax.swing.JComponent balanceCard() {
        Money balance = data.safe() == null ? null : data.safe().balance();
        String dollars = "–";
        String cents = "";
        if (balance != null) {
            String full = Theme.money(balance);
            int dot = full.lastIndexOf('.');
            dollars = full.substring(balance.isNegative() ? 2 : 1, dot);
            if (balance.isNegative()) {
                dollars = "-" + dollars;
            }
            cents = full.substring(dot);
        }
        List<HeroBalanceCard.Stat> stats = new ArrayList<>();
        stats.add(new HeroBalanceCard.Stat("Spent in " + monthName(true), Theme.money(data.spentThisMonth())));
        String pill = null;
        if (data.budget() != null && data.budget().totalLimit().isPositive()) {
            stats.add(new HeroBalanceCard.Stat("Budgeted", Theme.moneyWhole(data.budget().totalLimit())));
            Money diff = data.budget().totalSpent().subtract(data.budget().totalLimit());
            pill = diff.isPositive() ? "+" + Theme.money(diff) + " over" : Theme.money(diff.negate()) + " left";
        } else {
            stats.add(new HeroBalanceCard.Stat("Received", Theme.moneyWhole(data.receivedThisMonth())));
        }
        String note = data.forecast() == null ? "Add your balance and income to see the forecast"
                : "Lowest in the next 30 days: " + Theme.moneyWhole(data.forecast().lowestBalance());
        HeroBalanceCard card = new HeroBalanceCard("Balance today", dollars, cents, data.safe() == null ? null : "From your plan",
                stats, pill, note);
        card.setPreferredSize(new Dimension(600, 286));
        return card;
    }

    private Card healthCard() {
        Card card = new Card("Financial health", null, null).minHeight(286);
        Integer score = data.score() == null ? null : data.score().total();
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        ScoreRing ring = new ScoreRing(score);
        ring.setPreferredSize(new Dimension(130, 130));
        ring.setMaximumSize(new Dimension(130, 130));
        ring.setAlignmentX(Component.CENTER_ALIGNMENT);
        col.add(Box.createVerticalGlue());
        col.add(ring);
        col.add(Box.createVerticalStrut(14));
        String verdict = score == null ? "Not enough information yet" : score >= 70 ? "In good shape" : score >= 40 ? "Room to improve" : "Needs attention";
        col.add(centred(verdict, 16f, true, Theme.p().text()));
        col.add(Box.createVerticalStrut(4));
        col.add(centred(score == null ? "Set a budget, a plan and a goal" : weakest(), 12f, false, Theme.p().muted()));
        if (score != null) {
            col.add(Box.createVerticalStrut(12));
            JPanel row = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.CENTER_ALIGNMENT);
            row.add(new FlatButton("See breakdown", null, false, this::showBreakdown));
            col.add(row);
        }
        col.add(Box.createVerticalGlue());
        card.body().add(col, BorderLayout.CENTER);
        return card;
    }

    private void showBreakdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("Score: ").append(data.score().total()).append(" / 100");
        data.score().changeFromLastMonth().ifPresent(d -> sb.append("  (").append(d >= 0 ? "+" : "").append(d).append(" from last month)"));
        sb.append("\n\n");
        for (wealthsei.domain.ScoreComponent c : data.score().components()) {
            sb.append(c.name()).append(": ").append(c.points()).append(" of ").append(c.maxPoints()).append("\n   ").append(c.detail()).append("\n\n");
        }
        if (!data.score().excluded().isEmpty()) {
            sb.append("Not counted yet: ").append(String.join(", ", data.score().excluded())).append("\n\n");
        }
        sb.append(data.score().explanation());
        Dialogs.message(ctx.window(), "How the score is made", sb.toString().trim());
    }

    private String weakest() {
        return data.score().components().stream()
                .filter(c -> c.points() < c.maxPoints())
                .min(java.util.Comparator.comparingDouble(c -> c.points() / (double) c.maxPoints()))
                .map(c -> "Weakest part: " + c.name().toLowerCase(Locale.ENGLISH))
                .orElse("Every part is at full marks");
    }

    // ---- second row ----

    private Card forecastCard() {
        Card card = new Card("Next 30 days", "Where your balance is heading", "01").minHeight(330);
        if (data.forecast() == null) {
            card.body().add(actionHint("Add your balance and income to see where your money is heading.", "Set my balance",
                    () -> PlanDialogs.balance(ctx)), BorderLayout.CENTER);
            return card;
        }
        List<LocalDate> dates = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (DailyBalance b : data.forecast().days()) {
            dates.add(b.date());
            values.add(b.balance().amount().doubleValue());
        }
        card.body().add(new LineChart(dates, values), BorderLayout.CENTER);
        return card;
    }

    private Card safeCard() {
        Card card = new Card("Every day", "Safe to spend", "02").minHeight(330);
        JPanel col = column();
        if (data.safe() == null) {
            card.body().add(actionHint("Needs a plan with your balance and income.", "Set my balance",
                    () -> PlanDialogs.balance(ctx)), BorderLayout.CENTER);
            return card;
        }
        col.add(Box.createVerticalGlue());
        col.add(text(Theme.money(data.safe().dailyAllowance()), 40f, true, data.safe().isOverCommitted() ? Theme.p().danger() : Theme.p().accent()));
        col.add(text("per day for " + data.safe().daysLeft() + " more days", 13f, false, Theme.p().muted()));
        col.add(Box.createVerticalStrut(18));
        col.add(new ListRow(null, "Free this month", null, Theme.moneyWhole(data.safe().remainingThisMonth()), Theme.p().text()));
        col.add(new ListRow(null, "Bills coming up", null, Theme.moneyWhole(data.safe().upcomingBills()), Theme.p().text()));
        col.add(new ListRow(null, "Set aside for goals", null, Theme.moneyWhole(data.safe().goalReserve()), Theme.p().text()));
        col.add(Box.createVerticalGlue());
        card.body().add(col, BorderLayout.CENTER);
        return card;
    }

    // ---- where it went ----

    private Card spendingCard() {
        Card card = new Card(monthName(false), "Where it went", "03").minHeight(290);
        List<DonutChart.Slice> slices = new ArrayList<>();
        List<Map.Entry<Category, Money>> entries = new ArrayList<>(data.spendingByCategory().entrySet());
        double other = 0;
        for (int i = 0; i < entries.size(); i++) {
            double v = entries.get(i).getValue().amount().doubleValue();
            if (i < 5) {
                slices.add(new DonutChart.Slice(entries.get(i).getKey().name(), v, Theme.categoryColour(entries.get(i).getKey())));
            } else {
                other += v;
            }
        }
        if (other > 0) {
            slices.add(new DonutChart.Slice("Other", other, Theme.p().muted()));
        }
        if (slices.isEmpty()) {
            card.body().add(hint("No spending recorded for this month yet.", ""), BorderLayout.CENTER);
            return card;
        }
        DonutChart donut = new DonutChart(slices, Theme.moneyWhole(data.spentThisMonth()), "spent");
        donut.setPreferredSize(new Dimension(200, 200));
        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 30));
        left.add(donut, BorderLayout.CENTER);
        left.setPreferredSize(new Dimension(240, 200));
        JPanel legend = new JPanel(new GridLayout(0, 3, 40, 6));
        legend.setOpaque(false);
        for (DonutChart.Slice s : slices) {
            legend.add(new LegendRow(s.colour(), s.label(), Theme.moneyWhole(Money.of(java.math.BigDecimal.valueOf(s.value())))));
        }
        JPanel legendHolder = new JPanel(new BorderLayout());
        legendHolder.setOpaque(false);
        legendHolder.add(legend, BorderLayout.CENTER);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(left, BorderLayout.WEST);
        JPanel centre = new JPanel(new java.awt.GridBagLayout());
        centre.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        centre.add(legendHolder, c);
        wrap.add(centre, BorderLayout.CENTER);
        card.body().add(wrap, BorderLayout.CENTER);
        return card;
    }

    // ---- bottom row ----

    private Card budgetCard() {
        Card card = new Card("Worst first", "Budget", "04").minHeight(340);
        if (data.budget() == null) {
            FlatButton[] b = new FlatButton[1];
            b[0] = new FlatButton("Set a limit", null, true, () -> PlanDialogs.budgetLimit(ctx, b[0], data.month()));
            card.body().add(actionHint("No budget for this month yet.", b), BorderLayout.CENTER);
            return card;
        }
        JPanel col = column();
        List<BudgetLineStatus> lines = new ArrayList<>(data.budget().lines());
        lines.sort((a, b) -> Double.compare(ratio(b), ratio(a)));
        col.add(Box.createVerticalStrut(4));
        for (BudgetLineStatus l : lines.stream().limit(5).toList()) {
            Color colour = l.level() == AlertSeverity.OVER ? Theme.p().danger()
                    : l.level() == AlertSeverity.WARNING ? Theme.p().warn() : Theme.p().good();
            col.add(new ProgressRow(l.category().name(),
                    Theme.moneyWhole(l.spent()) + " of " + Theme.moneyWhole(l.limit()) + "  ·  " + Math.round(ratio(l) * 100) + "%",
                    ratio(l), colour, Theme.categoryColour(l.category())));
            col.add(Box.createVerticalStrut(6));
        }
        col.add(Box.createVerticalStrut(8));
        JPanel row = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        FlatButton[] b = new FlatButton[1];
        b[0] = new FlatButton("Set a limit", null, false, () -> PlanDialogs.budgetLimit(ctx, b[0], data.month()));
        row.add(b[0]);
        col.add(row);
        card.body().add(col, BorderLayout.NORTH);
        return card;
    }

    private static double ratio(BudgetLineStatus l) {
        return l.limit().isZero() ? (l.spent().isZero() ? 0 : 2) : l.spent().amount().doubleValue() / l.limit().amount().doubleValue();
    }

    private Card goalsCard() {
        Card card = new Card("In progress", "Goals", "05").minHeight(340);
        if (data.goals().isEmpty()) {
            card.body().add(actionHint("No savings goals yet.", "Create a goal", () -> ctx.navigate(AppContext.GOALS)), BorderLayout.CENTER);
            return card;
        }
        JPanel col = column();
        for (SavingsGoal g : data.goals().stream().limit(4).toList()) {
            double f = g.target().isZero() ? 0 : g.saved().amount().doubleValue() / g.target().amount().doubleValue();
            Color colour = g.status() == GoalStatus.BEHIND ? Theme.p().danger()
                    : g.status() == GoalStatus.AT_RISK ? Theme.p().warn() : Theme.p().good();
            col.add(new GoalRow(g.name(), Theme.moneyWhole(g.saved()) + " of " + Theme.moneyWhole(g.target()) + "  ·  "
                    + g.status().name().replace('_', ' ').toLowerCase(Locale.ENGLISH), f, colour));
        }
        card.body().add(col, BorderLayout.NORTH);
        return card;
    }

    private Card recurringCard() {
        wealthsei.analytics.RecurringReport report = data.recurring();
        String eyebrow = report == null || report.payments().isEmpty() ? "Subscriptions and bills"
                : Theme.moneyWhole(report.monthlyTotal()) + " a month";
        Card card = new Card(eyebrow, "Recurring payments", "06").minHeight(340);
        if (report == null || report.payments().isEmpty()) {
            card.body().add(hint("No repeating payments found yet.", "Import at least two months of history"), BorderLayout.CENTER);
            return card;
        }
        JPanel col = column();
        for (wealthsei.domain.RecurringPayment p : report.payments().stream().limit(5).toList()) {
            String sub = p.frequency().name().charAt(0) + p.frequency().name().substring(1).toLowerCase(Locale.ENGLISH)
                    + " · next " + p.nextDue()
                    + (p.priceIncrease().isPositive() ? " · up " + Theme.money(p.priceIncrease()) : "");
            Color dot = p.priceIncrease().isPositive() || p.flaggedForReview() ? Theme.p().warn() : Theme.p().info();
            ListRow row = new ListRow(dot, p.merchant(), sub, Theme.money(p.amount()), Theme.p().text());
            row.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            row.setToolTipText("Click to flag this for review");
            row.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    boolean flagged = p.flaggedForReview();
                    Menus.popup(row, List.of(new Menus.Option(flagged ? "Remove the review flag" : "Flag for review")), -1, i -> {
                        try {
                            facade.flagRecurring(p.merchant(), !flagged, ctx.today());
                            ctx.toast(flagged ? p.merchant() + " is no longer flagged." : p.merchant() + " flagged for review.", Toast.Kind.INFO);
                            ctx.changed();
                        } catch (RuntimeException ex) {
                            ctx.toast(ex.getMessage(), Toast.Kind.ERROR);
                        }
                    });
                }
            });
            col.add(row);
        }
        col.add(Box.createVerticalStrut(8));
        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        FlatButton[] review = new FlatButton[1];
        review[0] = new FlatButton("AI review", null, false, () -> reviewRecurring(review[0]));
        actions.add(review[0]);
        col.add(actions);
        card.body().add(col, BorderLayout.NORTH);
        return card;
    }

    /** Asks for the short review in the background, so the window does not freeze while the model answers. */
    private void reviewRecurring(FlatButton button) {
        button.setEnabled(false);
        ctx.toast("Reviewing your recurring payments...", Toast.Kind.INFO);
        new javax.swing.SwingWorker<wealthsei.analytics.RecurringReview, Void>() {
            @Override
            protected wealthsei.analytics.RecurringReview doInBackground() {
                return facade.reviewRecurring(ctx.today());
            }

            @Override
            protected void done() {
                button.setEnabled(true);
                try {
                    wealthsei.analytics.RecurringReview r = get();
                    Dialogs.message(ctx.window(), r.byAi() ? "AI review of your recurring payments" : "Recurring payments summary",
                            r.byAi() ? r.text() : r.text() + "\n\n" + r.note());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException e) {
                    ctx.toast("Could not review: " + e.getCause().getMessage(), Toast.Kind.ERROR);
                }
            }
        }.execute();
    }

    /** The most rows the Needs attention card shows (two columns of four). */
    private static final int MAX_ATTENTION = 8;

    private Card attentionCard() {
        List<AttentionRow> rows = new ArrayList<>();
        if (!data.pending().isEmpty()) {
            rows.add(new AttentionRow(rows.size() + 1, Theme.p().accent(),
                    data.pending().size() + " suggestion" + (data.pending().size() == 1 ? "" : "s") + " waiting for you",
                    data.pending().get(0).title(), goToProposals));
        }
        if (data.budget() != null) {
            int overShown = 0;
            for (BudgetLineStatus l : data.budget().lines()) {
                if (l.level() == AlertSeverity.OVER && overShown < 3 && rows.size() < MAX_ATTENTION) {
                    overShown++;
                    rows.add(new AttentionRow(rows.size() + 1, Theme.p().danger(), l.category().name() + " is over budget",
                            Theme.moneyWhole(l.spent().subtract(l.limit())) + " above the limit", null));
                }
            }
        }
        int unusualShown = 0;
        for (Insight i : data.habits()) {
            // Several unusual expenses can fill the whole card, so show at most two of them and leave room for the other habits.
            boolean unusual = i.title().startsWith("Unusual expense");
            if (unusual && unusualShown >= 2) {
                continue;
            }
            if (unusual) {
                unusualShown++;
            }
            if (rows.size() < MAX_ATTENTION) {
                rows.add(new AttentionRow(rows.size() + 1, Theme.p().warn(), i.title(), i.evidence(),
                        () -> Dialogs.message(ctx.window(), i.title(), i.evidence() + "\n\n" + i.explanation())));
            }
        }
        for (SavingsGoal g : data.goals()) {
            if (g.status() == GoalStatus.BEHIND && rows.size() < MAX_ATTENTION) {
                rows.add(new AttentionRow(rows.size() + 1, Theme.p().danger(), g.name() + " is behind schedule", "Open Goals for options", null));
            }
        }
        Card card = new Card(rows.size() + (rows.size() == 1 ? " signal found" : " signals found"), "Needs attention", "07").minHeight(240);
        if (rows.isEmpty()) {
            rows.add(new AttentionRow(1, Theme.p().good(), "Nothing needs attention", "You are on track", null));
        }
        JPanel col = new JPanel(new GridLayout(0, 2, 40, 0));
        col.setOpaque(false);
        rows.forEach(col::add);
        card.body().add(col, BorderLayout.NORTH);
        return card;
    }

    // ---- helpers ----

    private static JPanel column() {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        return col;
    }

    private static Label text(String s, float size, boolean bold, Color colour) {
        Label l = new Label(s, bold ? Theme.tracked(Theme.bold(size), -0.03f) : Theme.regular(size), colour);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static Label centred(String s, float size, boolean bold, Color colour) {
        Label l = new Label(s, bold ? Theme.bold(size) : Theme.regular(size), colour) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(java.awt.Graphics g0) {
                java.awt.Graphics2D g = (java.awt.Graphics2D) g0.create();
                Theme.quality(g);
                g.setFont(getFont());
                g.setColor(getForeground());
                String shown = ListRow.clip(getText(), g.getFontMetrics(), getWidth());
                java.awt.FontMetrics fm = g.getFontMetrics();
                g.drawString(shown, (getWidth() - fm.stringWidth(shown)) / 2f, (getHeight() - fm.getHeight()) / 2f + fm.getAscent());
                g.dispose();
            }
        };
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private static JPanel actionHint(String line, String buttonText, Runnable action) {
        return actionHint(line, new FlatButton[] {new FlatButton(buttonText, null, true, action)});
    }

    private static JPanel actionHint(String line, FlatButton[] button) {
        JPanel p = column();
        p.add(Box.createVerticalGlue());
        p.add(text(line, 13.5f, false, Theme.p().muted()));
        p.add(Box.createVerticalStrut(14));
        JPanel row = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(button[0]);
        p.add(row);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private static JPanel hint(String line, String example) {
        JPanel p = column();
        p.add(Box.createVerticalGlue());
        p.add(text(line, 13.5f, false, Theme.p().muted()));
        if (!example.isEmpty()) {
            p.add(Box.createVerticalStrut(8));
            p.add(text(example, 12f, false, Theme.p().accent()));
        }
        p.add(Box.createVerticalGlue());
        return p;
    }

    private Card emptyState() {
        Card card = new Card("Start here", "Welcome to WealthSei", null).minHeight(300);
        JPanel col = column();
        col.add(Box.createVerticalGlue());
        col.add(text("Import a bank CSV and your dashboard fills in.", 14f, false, Theme.p().muted()));
        col.add(Box.createVerticalStrut(16));
        Label go = text("Go to Transactions to import  →", 14f, true, Theme.p().accent());
        go.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        go.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                goToTransactions.run();
            }
        });
        col.add(go);
        col.add(Box.createVerticalGlue());
        card.body().add(col, BorderLayout.CENTER);
        return card;
    }
}
