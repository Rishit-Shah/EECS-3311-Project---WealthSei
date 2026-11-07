package wealthsei.presentation.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import wealthsei.command.Proposal;
import wealthsei.domain.Categories;
import wealthsei.domain.Category;

/** Suggestions waiting for a decision, what was decided before, and the categories the assistant must never touch. */
@SuppressWarnings({"serial", "this-escape"})
public class ProposalsView extends View {

    private static final int GAP = 14;
    private static final int TEXT_WIDTH = 640;

    private final AppContext ctx;

    public ProposalsView(AppContext ctx) {
        this.ctx = ctx;
        refresh();
    }

    @Override
    public String title() {
        return "Proposals";
    }

    @Override
    public String[] headline() {
        return new String[] {"Suggestions,", "your call."};
    }

    @Override
    public String subtitle() {
        return "WealthSei never changes your plan by itself. Approve a suggestion and it is applied; undo it any time.";
    }

    @Override
    public void refresh() {
        removeAll();
        List<Proposal> pending = ctx.facade().listPendingProposals(ctx.today());
        List<Proposal> history = new ArrayList<>();
        for (Proposal p : ctx.facade().listAllProposals()) {
            if (!p.isPending()) {
                history.add(p);
            }
        }
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        Card waiting = new Card("Needs your decision", pending.isEmpty() ? "Nothing waiting" : pending.size() + " waiting", "01").minHeight(150);
        JPanel list = vertical();
        if (pending.isEmpty()) {
            list.add(new Label("No suggestions right now. Ask the assistant what you can change, or generate a monthly review.",
                    Theme.regular(14f), Theme.p().muted()));
            list.add(Box.createVerticalStrut(14));
            JPanel go = left();
            go.add(new FlatButton("Open the assistant", null, true, () -> ctx.navigate(AppContext.ASSISTANT)));
            go.add(Box.createHorizontalStrut(8));
            go.add(new FlatButton("Monthly review", null, false, () -> ctx.navigate(AppContext.REPORTS)));
            list.add(go);
        }
        for (Proposal p : pending) {
            list.add(pendingCard(p));
            list.add(Box.createVerticalStrut(10));
        }
        waiting.body().add(list, BorderLayout.NORTH);
        waiting.setAlignmentX(0f);
        column.add(waiting);
        column.add(Box.createVerticalStrut(GAP));

        Card done = new Card("Already decided", "History", "02").minHeight(150);
        JPanel hist = vertical();
        if (history.isEmpty()) {
            hist.add(new Label("Approved, rejected and undone suggestions will appear here.", Theme.regular(14f), Theme.p().muted()));
        }
        for (Proposal p : history) {
            hist.add(historyRow(p));
        }
        if (history.stream().anyMatch(p -> p.statusName().equals("Applied"))) {
            hist.add(Box.createVerticalStrut(14));
            JPanel undoRow = left();
            undoRow.add(new FlatButton("Undo the last applied change", null, false, this::undoLast));
            hist.add(undoRow);
        }
        done.body().add(hist, BorderLayout.NORTH);
        done.setAlignmentX(0f);
        column.add(done);
        column.add(Box.createVerticalStrut(GAP));

        Card prot = new Card("Guardrails", "Protected categories", "03").minHeight(150);
        JPanel box = vertical();
        Label help = new Label("The assistant never suggests cutting these. Tap a category to protect or unprotect it.",
                Theme.regular(13.5f), Theme.p().muted());
        box.add(help);
        box.add(Box.createVerticalStrut(14));
        Set<Category> protectedNow = ctx.facade().protectedCategories();
        JPanel flow = new JPanel(new java.awt.GridLayout(0, 5, 10, 10));
        flow.setOpaque(false);
        for (Category c : Categories.defaults()) {
            if (c.equals(Categories.INCOME)) {
                continue;
            }
            flow.add(new ToggleChip(c.name(), Theme.categoryColour(c), protectedNow.contains(c), on -> {
                ctx.facade().setProtectedCategory(c, on);
                ctx.toast(c.name() + (on ? " is now protected." : " is no longer protected."), Toast.Kind.INFO);
            }));
        }
        box.add(flow);
        prot.body().add(box, BorderLayout.NORTH);
        prot.setAlignmentX(0f);
        column.add(prot);

        add(column, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private static JPanel vertical() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private static JPanel left() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.setAlignmentX(0f);
        return p;
    }

    private static Text wrapped(String text, Color colour, float size, boolean bold) {
        return Text.wrapped(text, colour, size, bold, TEXT_WIDTH);
    }

    private JPanel pendingCard(Proposal p) {
        JPanel inner = new RaisedPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        inner.setAlignmentX(0f);

        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        head.setOpaque(false);
        head.setAlignmentX(0f);
        head.add(new Pill(kindName(p), Theme.p().accent()));
        inner.add(head);
        inner.add(Box.createVerticalStrut(10));
        inner.add(wrapped(p.title(), Theme.p().text(), 16f, true));
        inner.add(Box.createVerticalStrut(6));
        inner.add(wrapped(p.rationale(), Theme.p().muted(), 13.5f, false));
        inner.add(Box.createVerticalStrut(6));
        inner.add(wrapped("What happens: " + p.command().describe(), Theme.p().muted(), 12.5f, false));
        inner.add(Box.createVerticalStrut(16));

        JPanel buttons = left();
        buttons.add(new FlatButton("Approve", null, true, () -> decide(p, true)));
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(new FlatButton("Reject", null, false, () -> decide(p, false)));
        if (!p.traceId().isEmpty()) {
            buttons.add(Box.createHorizontalStrut(8));
            buttons.add(new FlatButton("How did the AI get here?", null, false,
                    () -> TraceDialog.show(ctx.window(), ctx.facade(), p.traceId())));
        }
        inner.add(buttons);
        return inner;
    }

    private static String kindName(Proposal p) {
        return switch (p.type()) {
            case BUDGET_ADJUSTMENT -> "BUDGET";
            case GOAL_ADJUSTMENT -> "GOAL";
            case FLAG_SUBSCRIPTION -> "RECURRING";
            case RECATEGORIZE -> "CATEGORY";
        };
    }

    private JPanel historyRow(Proposal p) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        row.setAlignmentX(0f);
        Color c = switch (p.statusName()) {
            case "Applied" -> Theme.p().good();
            case "Rejected" -> Theme.p().danger();
            case "Undone" -> Theme.p().info();
            default -> Theme.p().muted();
        };
        JPanel pill = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        pill.setOpaque(false);
        pill.add(new Pill(p.statusName().toUpperCase(java.util.Locale.ENGLISH), c));
        row.add(pill, BorderLayout.WEST);
        row.add(new Label(p.title(), Theme.regular(14f), Theme.p().text()), BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return row;
    }

    private void decide(Proposal p, boolean approve) {
        try {
            Proposal done = ctx.facade().decideProposal(p.id(), approve);
            ctx.toast(approve ? "Done: " + done.command().describe() : "Rejected. Nothing was changed.",
                    approve ? Toast.Kind.SUCCESS : Toast.Kind.INFO);
        } catch (RuntimeException e) {
            ctx.toast(e.getMessage(), Toast.Kind.ERROR);
        }
        ctx.changed();
    }

    private void undoLast() {
        ctx.facade().undoLastProposal().ifPresentOrElse(
                p -> ctx.toast("Undone: " + p.title(), Toast.Kind.INFO),
                () -> ctx.toast("Nothing to undo.", Toast.Kind.WARNING));
        ctx.changed();
    }

    /** A lighter rounded panel inside a card. */
    private static final class RaisedPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        RaisedPanel() {
            setOpaque(false);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.quality(g);
            g.setColor(Theme.p().surfaceRaised());
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
            g.setColor(Theme.p().border());
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
            g.dispose();
        }
    }
}
