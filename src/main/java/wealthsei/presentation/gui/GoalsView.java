package wealthsei.presentation.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import wealthsei.domain.FeasibilityReport;
import wealthsei.domain.GoalOption;
import wealthsei.domain.GoalRequest;
import wealthsei.domain.GoalStatus;
import wealthsei.domain.Money;
import wealthsei.domain.SavingsGoal;

/** What you are saving for: progress, status, a feasibility check, and what to change when a goal is at risk. */
@SuppressWarnings({"serial", "this-escape"})
public class GoalsView extends View {

    private static final DateTimeFormatter DUE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final int GAP = 14;

    private final AppContext ctx;
    private final Set<String> planOpen = new HashSet<>();

    public GoalsView(AppContext ctx) {
        this.ctx = ctx;
        refresh();
    }

    @Override
    public String title() {
        return "Goals";
    }

    @Override
    public String[] headline() {
        return new String[] {"What you are", "saving for."};
    }

    @Override
    public String subtitle() {
        return "Each goal, whether it is on track, and what to change if it is not. Fixes arrive as proposals you approve.";
    }

    @Override
    public void refresh() {
        removeAll();
        List<SavingsGoal> goals = ctx.facade().getGoals();
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bar.setOpaque(false);
        bar.setAlignmentX(0f);
        bar.add(new FlatButton("New goal", null, true, this::addGoal));
        bar.add(Box.createHorizontalStrut(14));
        bar.add(new Label(goals.isEmpty() ? "No goals yet" : goals.size() + (goals.size() == 1 ? " goal" : " goals")
                + " · " + Theme.money(totalSaved(goals)) + " saved of " + Theme.money(totalTarget(goals)), Theme.regular(13f), Theme.pageMuted()));
        column.add(bar);
        column.add(Box.createVerticalStrut(GAP));

        if (goals.isEmpty()) {
            Card empty = new Card("Start here", "Your first goal", "01").minHeight(160);
            empty.body().add(new Label("Pick something to save for, such as a laptop or an emergency fund, and a date. WealthSei tells you whether it is realistic.",
                    Theme.regular(14f), Theme.p().muted()), BorderLayout.NORTH);
            empty.setAlignmentX(0f);
            column.add(empty);
        } else {
            JPanel grid = new JPanel(new GridBagLayout());
            grid.setOpaque(false);
            grid.setAlignmentX(0f);
            for (int i = 0; i < goals.size(); i++) {
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = i % 2;
                c.gridy = i / 2;
                c.weightx = 0.5;
                c.fill = GridBagConstraints.BOTH;
                c.anchor = GridBagConstraints.NORTH;
                c.insets = new Insets(0, c.gridx == 0 ? 0 : GAP / 2, GAP, c.gridx == 0 ? GAP / 2 : 0);
                grid.add(goalCard(goals.get(i), i + 1), c);
            }
            column.add(grid);
        }
        add(column, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private static Money totalSaved(List<SavingsGoal> goals) {
        Money m = Money.ZERO;
        for (SavingsGoal g : goals) {
            m = m.add(g.saved());
        }
        return m;
    }

    private static Money totalTarget(List<SavingsGoal> goals) {
        Money m = Money.ZERO;
        for (SavingsGoal g : goals) {
            m = m.add(g.target());
        }
        return m;
    }

    static Color statusColour(GoalStatus s) {
        return switch (s) {
            case ON_TRACK -> Theme.p().good();
            case AT_RISK -> Theme.p().warn();
            case BEHIND -> Theme.p().danger();
            case ACHIEVED -> Theme.p().info();
        };
    }

    static String statusText(GoalStatus s) {
        return switch (s) {
            case ON_TRACK -> "ON TRACK";
            case AT_RISK -> "AT RISK";
            case BEHIND -> "BEHIND";
            case ACHIEVED -> "ACHIEVED";
        };
    }

    private Card goalCard(SavingsGoal g, int n) {
        Card card = new Card("Goal", g.name(), String.format("%02d", n)).minHeight(250);
        JPanel body = card.body();
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        double fraction = g.target().isPositive() ? g.saved().amount().doubleValue() / g.target().amount().doubleValue() : 0;
        long days = ChronoUnit.DAYS.between(ctx.today(), g.deadline());
        String due = g.isAchieved() ? "Done" : days < 0 ? "Due " + g.deadline().format(DUE) + " (passed)"
                : "Due " + g.deadline().format(DUE) + " · " + days + (days == 1 ? " day" : " days") + " left";
        GoalRow row = new GoalRow(Theme.money(g.saved()) + " of " + Theme.money(g.target()), due, fraction, statusColour(g.status()));
        row.setAlignmentX(0f);
        col.add(row);
        col.add(Box.createVerticalStrut(10));

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        chips.setOpaque(false);
        chips.setAlignmentX(0f);
        chips.add(new Pill(statusText(g.status()), statusColour(g.status())));
        chips.add(Box.createHorizontalStrut(10));
        chips.add(new Label("Setting aside " + Theme.money(g.monthlyContribution()) + " a month", Theme.regular(12.5f), Theme.p().muted()));
        col.add(chips);
        col.add(Box.createVerticalStrut(14));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(0f);
        boolean open = planOpen.contains(g.id());
        buttons.add(new FlatButton(open ? "Hide plan" : "Plan", null, true, () -> {
            if (!planOpen.remove(g.id())) {
                planOpen.add(g.id());
            }
            refresh();
        }));
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(new FlatButton("Add money", null, false, () -> addMoney(g)));
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(new FlatButton("Set monthly", null, false, () -> setMonthly(g)));
        col.add(buttons);

        if (open) {
            col.add(Box.createVerticalStrut(16));
            col.add(planPanel(g));
        }
        body.add(col, BorderLayout.NORTH);
        return card;
    }

    private JPanel planPanel(SavingsGoal g) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(0f);
        try {
            FeasibilityReport r = ctx.facade().getGoalFeasibility(g.id(), ctx.today());
            Label head = new Label(r.feasible() ? "Reachable on time" : "Not reachable on time at this pace",
                    Theme.bold(14f), r.feasible() ? Theme.p().good() : Theme.p().warn());
            head.setAlignmentX(0f);
            p.add(head);
            p.add(Box.createVerticalStrut(4));
            Label need = new Label("Needs " + Theme.money(r.requiredMonthly()) + " a month. You can spare about "
                    + Theme.money(r.monthlySurplus()) + ".", Theme.regular(13f), Theme.p().muted());
            need.setAlignmentX(0f);
            p.add(need);
            p.add(Box.createVerticalStrut(10));
            for (GoalOption o : r.options()) {
                ListRow row = new ListRow(Theme.p().accent(), o.description(), o.impact(), null, null);
                row.setAlignmentX(0f);
                p.add(row);
            }
            if (!r.feasible() || r.requiredMonthly().compareTo(g.monthlyContribution()) > 0) {
                p.add(Box.createVerticalStrut(10));
                JPanel fixRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                fixRow.setOpaque(false);
                fixRow.setAlignmentX(0f);
                fixRow.add(new FlatButton("Suggest a fix", null, true, () -> proposeFix(g)));
                p.add(fixRow);
            }
        } catch (RuntimeException e) {
            p.add(new Label(e.getMessage(), Theme.regular(13f), Theme.p().danger()));
        }
        return p;
    }

    private void proposeFix(SavingsGoal g) {
        try {
            ctx.facade().proposeGoalFix(g.id(), ctx.today());
            ctx.changed();
            ctx.toast("Proposal created for " + g.name() + ". Review it under Proposals.", Toast.Kind.SUCCESS);
        } catch (RuntimeException e) {
            ctx.toast(e.getMessage(), Toast.Kind.WARNING);
        }
    }

    private void addGoal() {
        String[] v = Dialogs.form(ctx.window(), "New goal", "What are you saving for, how much, and by when?",
                new String[] {"Name", "Target amount", "Deadline (YYYY-MM-DD)", "Already saved"},
                new String[] {"", "", ctx.today().plusMonths(6).toString(), "0"}, "Create goal");
        if (v == null) {
            return;
        }
        try {
            if (v[0].isBlank()) {
                throw new IllegalArgumentException("Please give the goal a name");
            }
            SavingsGoal made = ctx.facade().addGoal(new GoalRequest(v[0], Dialogs.money(v[1]), Dialogs.date(v[2]), Dialogs.money(v[3])), ctx.today());
            planOpen.add(made.id());
            refresh();
            ctx.toast("Goal added: " + made.name() + ".", Toast.Kind.SUCCESS);
        } catch (RuntimeException e) {
            ctx.toast(e.getMessage(), Toast.Kind.ERROR);
        }
    }

    private void addMoney(SavingsGoal g) {
        String[] v = Dialogs.form(ctx.window(), "Add to " + g.name(), "How much did you just put toward this goal?",
                new String[] {"Amount"}, new String[] {""}, "Add");
        if (v == null) {
            return;
        }
        try {
            ctx.facade().addToGoal(g.id(), Dialogs.money(v[0]), ctx.today());
            refresh();
            ctx.toast("Added " + Theme.money(Dialogs.money(v[0])) + " to " + g.name() + ".", Toast.Kind.SUCCESS);
        } catch (RuntimeException e) {
            ctx.toast(e.getMessage(), Toast.Kind.ERROR);
        }
    }

    private void setMonthly(SavingsGoal g) {
        String[] v = Dialogs.form(ctx.window(), "Monthly amount for " + g.name(), "How much do you plan to set aside each month?",
                new String[] {"Per month"}, new String[] {g.monthlyContribution().amount().toPlainString()}, "Save");
        if (v == null) {
            return;
        }
        try {
            SavingsGoal updated = ctx.facade().setGoalMonthlyContribution(g.id(), Dialogs.money(v[0]), ctx.today());
            refresh();
            ctx.toast(updated.name() + " is now " + statusText(updated.status()).toLowerCase(Locale.ENGLISH) + ".", Toast.Kind.INFO);
        } catch (RuntimeException e) {
            ctx.toast(e.getMessage(), Toast.Kind.ERROR);
        }
    }

    /** For previews and tests: opens the plan of every goal. */
    public void openAllPlans() {
        for (SavingsGoal g : ctx.facade().getGoals()) {
            planOpen.add(g.id());
        }
        refresh();
    }

    LocalDate now() {
        return ctx.today();
    }
}
