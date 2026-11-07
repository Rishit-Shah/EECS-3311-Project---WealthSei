package wealthsei.presentation.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import wealthsei.agent.AgentResult;
import wealthsei.agent.AgentStatus;
import wealthsei.agent.ProposalDraft;
import wealthsei.analytics.ScenarioResult;
import wealthsei.domain.Money;
import wealthsei.domain.Verdict;

/** Three ways to use the AI agent: ask a question, check an expense, or try a what-if. */
@SuppressWarnings({"serial", "this-escape"})
public class AssistantView extends View {

    private static final int GAP = 14;
    private static final int RESULT_TEXT = 520;

    private final AppContext ctx;
    private final JPanel content = new JPanel(new BorderLayout());
    private ChatPanel chat;
    private int mode;

    private final InputField item = new InputField("What do you want to buy?", "Noise-cancelling headphones");
    private final InputField price = new InputField("Price", "250");
    private final InputField when = new InputField("When (optional, YYYY-MM-DD)", "");
    private final InputField describe = new InputField("Describe it in words (uses AI)", "Cancel Spotify and cap dining at 150");
    private final InputField cancel = new InputField("Cancel a subscription", "Spotify");
    private final InputField caps = new InputField("Cap a category (category=amount)", "dining=150");
    private final InputField income = new InputField("Extra income per month", "200");
    private final InputField buy = new InputField("One-time expense (amount@date)", "600@2026-12-01");
    private final JPanel affordResult = new JPanel(new BorderLayout());
    private final JPanel whatResult = new JPanel(new BorderLayout());

    public AssistantView(AppContext ctx) {
        this.ctx = ctx;
        content.setOpaque(false);
        affordResult.setOpaque(false);
        whatResult.setOpaque(false);
        refresh();
    }

    @Override
    public String title() {
        return "Assistant";
    }

    @Override
    public String[] headline() {
        return new String[] {"Ask anything", "about your money."};
    }

    @Override
    public String subtitle() {
        return ctx.aiOn()
                ? "Answers are checked against your data. The numbers come from code; the AI decides what to look up and says it plainly."
                : "No API key was found, so free-form questions are off. The affordability check and what-if simulator still work from fixed rules.";
    }

    @Override
    public void refresh() {
        removeAll();
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        Segmented tabs = new Segmented(new String[] {"Ask", "Can I afford it?", "What if…"}, mode, i -> {
            mode = i;
            showMode();
        });
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(0f);
        row.add(tabs);
        column.add(row);
        column.add(Box.createVerticalStrut(GAP));
        content.setAlignmentX(0f);
        column.add(content);
        add(column, BorderLayout.CENTER);
        showMode();
    }

    /** Switches to a tab (0 ask, 1 afford, 2 what-if) and rebuilds it; used by previews. */
    public void openMode(int m) {
        mode = m;
        refresh();
    }

    /** Fills the what-if boxes with the examples shown as hints and runs the simulator. */
    public void runExample() {
        caps.setText("dining=150");
        income.setText("200");
        runStructured();
    }

    private void showMode() {
        content.removeAll();
        switch (mode) {
            case 1 -> content.add(affordPage(), BorderLayout.CENTER);
            case 2 -> content.add(whatIfPage(), BorderLayout.CENTER);
            default -> content.add(askPage(), BorderLayout.CENTER);
        }
        content.revalidate();
        content.repaint();
    }

    // ---- ask ----

    private JPanel askPage() {
        Card card = new Card("Conversation", "Ask WealthSei", "01").minHeight(620);
        if (chat == null) {
            chat = new ChatPanel(ctx.facade(), ctx.aiOn(), 640);
        }
        JPanel holder = new JPanel(new BorderLayout(0, 10));
        holder.setOpaque(false);
        holder.setPreferredSize(new Dimension(300, 520));
        holder.add(chat, BorderLayout.CENTER);
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        foot.setOpaque(false);
        foot.add(new FlatButton("How did you get this?", null, false,
                () -> TraceDialog.show(ctx.window(), ctx.facade(), "")));
        foot.add(Box.createHorizontalStrut(8));
        foot.add(new FlatButton("Clear conversation", null, false, () -> {
            ctx.facade().clearConversation();
            chat = null;
            showMode();
        }));
        holder.add(foot, BorderLayout.SOUTH);
        card.body().add(holder, BorderLayout.CENTER);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    // ---- can I afford it ----

    private JPanel affordPage() {
        Card form = new Card("Check an expense", "Can I afford it?", "01").minHeight(420);
        JPanel f = vertical();
        f.add(item);
        f.add(Box.createVerticalStrut(12));
        f.add(price);
        f.add(Box.createVerticalStrut(12));
        f.add(when);
        f.add(Box.createVerticalStrut(18));
        f.add(left(new FlatButton("Check", null, true, this::checkAfford)));
        f.add(Box.createVerticalStrut(12));
        f.add(Text.wrapped("The answer weighs your balance, upcoming bills, budget and goals. The AI explains it but can never be more optimistic than the rules.",
                Theme.p().muted(), 12.5f, false, 330));
        form.body().add(f, BorderLayout.NORTH);

        Card result = new Card("Verdict", "What WealthSei thinks", "02").minHeight(420);
        result.body().add(affordResult, BorderLayout.CENTER);
        if (affordResult.getComponentCount() == 0) {
            affordResult.add(Text.wrapped("Fill in the item and price, then press Check.", Theme.p().muted(), 14f, false, RESULT_TEXT), BorderLayout.NORTH);
        }
        return twoColumns(form, result, 0.36);
    }

    private void checkAfford() {
        Money p;
        LocalDate date;
        try {
            p = Dialogs.money(price.text());
            date = when.text().isEmpty() ? null : Dialogs.date(when.text());
        } catch (RuntimeException e) {
            ctx.toast(e.getMessage(), Toast.Kind.ERROR);
            return;
        }
        String what = item.text();
        showWorking(affordResult, "Checking your numbers…");
        background(() -> ctx.facade().canIAfford(what, p, date), r -> showAnswer(affordResult, r, true));
    }

    // ---- what if ----

    private JPanel whatIfPage() {
        Card form = new Card("Try a change", "What if…", "01").minHeight(520);
        JPanel f = vertical();
        f.add(describe);
        f.add(Box.createVerticalStrut(10));
        f.add(left(new FlatButton("Run with AI", null, true, this::runDescribed)));
        f.add(Box.createVerticalStrut(18));
        f.add(Text.wrapped("Or set the changes yourself, no AI needed:", Theme.p().muted(), 12.5f, false, 330));
        f.add(Box.createVerticalStrut(10));
        for (InputField x : List.of(cancel, caps, income, buy)) {
            f.add(x);
            f.add(Box.createVerticalStrut(10));
        }
        f.add(left(new FlatButton("Simulate", null, false, this::runStructured)));
        form.body().add(f, BorderLayout.NORTH);

        Card result = new Card("Side by side", "Baseline vs. with changes", "02").minHeight(520);
        result.body().add(whatResult, BorderLayout.CENTER);
        if (whatResult.getComponentCount() == 0) {
            whatResult.add(Text.wrapped("Describe a change, or fill in the boxes, to see what happens to your balance and goals over the next months.",
                    Theme.p().muted(), 14f, false, RESULT_TEXT), BorderLayout.NORTH);
        }
        return twoColumns(form, result, 0.36);
    }

    private void runDescribed() {
        if (!ctx.aiOn()) {
            ctx.toast("Describing a change in words needs the AI model (no API key found). Use the boxes below instead.", Toast.Kind.WARNING);
            return;
        }
        String text = describe.text();
        showWorking(whatResult, "Working out the changes…");
        background(() -> ctx.facade().runWhatIf(text), r -> showAnswer(whatResult, r, false));
    }

    private void runStructured() {
        Map<String, String> fields = new LinkedHashMap<>();
        put(fields, "cancel", cancel.text());
        put(fields, "caps", caps.text());
        put(fields, "income_increase", income.text());
        put(fields, "one_time_expenses", buy.text());
        try {
            ScenarioResult r = ctx.facade().simulateWhatIf(fields, ctx.today());
            List<ProposalDraft> drafts = ctx.facade().whatIfDrafts(fields, ctx.today());
            showScenario(r, drafts);
        } catch (RuntimeException e) {
            ctx.toast(e.getMessage(), Toast.Kind.ERROR);
        }
    }

    private static void put(Map<String, String> m, String key, String value) {
        if (!value.isBlank()) {
            m.put(key, value);
        }
    }

    private void showScenario(ScenarioResult r, List<ProposalDraft> drafts) {
        whatResult.removeAll();
        JPanel p = vertical();
        for (String c : r.changes()) {
            p.add(Text.wrapped("•  " + c, Theme.p().text(), 13.5f, false, RESULT_TEXT));
            p.add(Box.createVerticalStrut(3));
        }
        p.add(Box.createVerticalStrut(12));
        JPanel table = new JPanel(new GridLayout(0, 4, 8, 10));
        table.setOpaque(false);
        table.setAlignmentX(0f);
        for (String h : new String[] {"", "Baseline", "With changes", "Change"}) {
            table.add(new Label(h, Theme.bold(11.5f), Theme.p().muted()));
        }
        row(table, "End balance", r.baselineEndBalance(), r.scenarioEndBalance(), r.endBalanceDelta());
        row(table, "Lowest balance", r.baselineLowest(), r.scenarioLowest(), r.scenarioLowest().subtract(r.baselineLowest()));
        row(table, "Left per month", r.baselineMonthlySavings(), r.scenarioMonthlySavings(), r.monthlySavingsDelta());
        p.add(table);
        p.add(Box.createVerticalStrut(6));
        for (ScenarioResult.GoalImpact g : r.goalImpacts()) {
            p.add(Box.createVerticalStrut(8));
            p.add(Text.wrapped("Goal '" + g.goalName() + "' reached: " + (g.baselineCompletion() == null ? "not reached" : g.baselineCompletion())
                    + "  →  " + (g.scenarioCompletion() == null ? "not reached" : g.scenarioCompletion()), Theme.p().muted(), 13f, false, RESULT_TEXT));
        }
        if (r.typicalMonthlySpending().isPositive()) {
            p.add(Box.createVerticalStrut(8));
            p.add(Text.wrapped("Both columns include your typical everyday spending of " + Theme.money(r.typicalMonthlySpending()) + " a month.",
                    Theme.p().muted(), 12f, false, RESULT_TEXT));
        }
        addDraftButton(p, drafts, "");
        whatResult.add(p, BorderLayout.NORTH);
        whatResult.revalidate();
        whatResult.repaint();
    }

    private void row(JPanel table, String label, Money a, Money b, Money delta) {
        table.add(new Label(label, Theme.regular(13.5f), Theme.p().text()));
        table.add(new Label(Theme.money(a), Theme.regular(13.5f), Theme.p().muted()));
        table.add(new Label(Theme.money(b), Theme.bold(13.5f), Theme.p().text()));
        table.add(new Label((delta.isNegative() ? "" : "+") + Theme.money(delta), Theme.bold(13.5f),
                delta.isNegative() ? Theme.p().danger() : delta.isPositive() ? Theme.p().good() : Theme.p().muted()));
    }

    // ---- shared ----

    private void showWorking(JPanel target, String text) {
        target.removeAll();
        target.add(Text.wrapped(text, Theme.p().muted(), 14f, false, RESULT_TEXT), BorderLayout.NORTH);
        target.revalidate();
        target.repaint();
    }

    private void showAnswer(JPanel target, AgentResult r, boolean withVerdict) {
        target.removeAll();
        JPanel p = vertical();
        if (withVerdict && r.verdict() != null) {
            Verdict v = r.verdict();
            Label big = new Label(v.label(), Theme.bold(40f), verdictColour(v));
            big.setAlignmentX(0f);
            p.add(big);
            p.add(Box.createVerticalStrut(10));
        }
        p.add(Text.wrapped(r.answer(), Theme.p().text(), 14.5f, false, RESULT_TEXT));
        p.add(Box.createVerticalStrut(12));
        String note = r.status() == AgentStatus.COMPLETED ? "Every figure checked against your data."
                : r.status() == AgentStatus.MODEL_UNAVAILABLE ? "The AI was not used; this answer comes from fixed rules."
                : "Note: " + r.status().name().toLowerCase(java.util.Locale.ENGLISH).replace('_', ' ') + ". The answer may be partial.";
        p.add(Text.wrapped(note, Theme.p().muted(), 12.5f, false, RESULT_TEXT));
        p.add(Box.createVerticalStrut(14));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(0f);
        buttons.add(new FlatButton("How did you get this?", null, false,
                () -> TraceDialog.show(ctx.window(), ctx.facade(), r.traceId())));
        p.add(buttons);
        addDraftButton(p, r.drafts(), r.traceId());
        target.add(p, BorderLayout.NORTH);
        target.revalidate();
        target.repaint();
    }

    private void addDraftButton(JPanel p, List<ProposalDraft> drafts, String traceId) {
        if (drafts.isEmpty()) {
            return;
        }
        p.add(Box.createVerticalStrut(14));
        p.add(left(new FlatButton("Turn " + drafts.size() + (drafts.size() == 1 ? " change" : " changes") + " into proposals", null, true, () -> {
            try {
                ctx.facade().createProposals(drafts, traceId);
                ctx.changed();
                ctx.toast("Proposals created. Review them under Proposals.", Toast.Kind.SUCCESS);
                ctx.navigate(AppContext.PROPOSALS);
            } catch (RuntimeException e) {
                ctx.toast(e.getMessage(), Toast.Kind.ERROR);
            }
        })));
    }

    private static Color verdictColour(Verdict v) {
        return switch (v) {
            case YES -> Theme.p().good();
            case YES_BUT -> Theme.p().warn();
            case WAIT -> Theme.p().info();
            case NO -> Theme.p().danger();
        };
    }

    private void background(Supplier<AgentResult> work, Consumer<AgentResult> done) {
        new SwingWorker<AgentResult, Void>() {
            @Override
            protected AgentResult doInBackground() {
                return work.get();
            }

            @Override
            protected void done() {
                try {
                    done.accept(get());
                } catch (Exception e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    ctx.toast(cause.getMessage() == null ? "Something went wrong." : cause.getMessage(), Toast.Kind.ERROR);
                }
            }
        }.execute();
    }

    private static JPanel vertical() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private static JPanel left(java.awt.Component c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.setAlignmentX(0f);
        p.add(c);
        return p;
    }

    private static JPanel twoColumns(Card a, Card b, double leftWeight) {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTH;
        c.weighty = 1;
        c.weightx = leftWeight;
        c.insets = new Insets(0, 0, 0, GAP / 2);
        grid.add(a, c);
        c.gridx = 1;
        c.weightx = 1 - leftWeight;
        c.insets = new Insets(0, GAP / 2, 0, 0);
        grid.add(b, c);
        return grid;
    }
}
