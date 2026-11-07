package wealthsei.presentation.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeSet;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import wealthsei.domain.ExportFormat;
import wealthsei.domain.HealthScore;
import wealthsei.domain.MonthlyReview;
import wealthsei.domain.ScoreComponent;
import wealthsei.domain.Transaction;
import wealthsei.domain.TxFilter;
import wealthsei.service.ReviewResult;

/** The monthly review (written summary, health score, suggested fixes) and saving a report to a file. */
@SuppressWarnings({"serial", "this-escape"})
public class ReportsView extends View {

    private static final int GAP = 14;
    private static final int TEXT = 560;

    private final AppContext ctx;
    private List<YearMonth> months = new ArrayList<>();
    private int monthIndex;
    private int formatIndex;
    private String lastNote = "";
    private boolean working;

    public ReportsView(AppContext ctx) {
        this.ctx = ctx;
        refresh();
    }

    @Override
    public String title() {
        return "Reports";
    }

    @Override
    public String[] headline() {
        return new String[] {"Your month,", "explained."};
    }

    @Override
    public String subtitle() {
        return "A written review with your health score and up to three suggested fixes, plus reports you can save as Markdown, CSV or text.";
    }

    private YearMonth month() {
        return months.isEmpty() ? YearMonth.from(ctx.today()) : months.get(Math.min(monthIndex, months.size() - 1));
    }

    @Override
    public void refresh() {
        removeAll();
        YearMonth previous = months.isEmpty() ? null : month();
        TreeSet<YearMonth> found = new TreeSet<>(Comparator.reverseOrder());
        for (Transaction t : ctx.facade().listTransactions(TxFilter.all())) {
            found.add(YearMonth.from(t.date()));
        }
        months = new ArrayList<>(found);
        monthIndex = previous != null && months.contains(previous) ? months.indexOf(previous) : 0;

        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bar.setOpaque(false);
        if (!months.isEmpty()) {
            List<Menus.Option> opts = new ArrayList<>();
            for (YearMonth m : months) {
                opts.add(new Menus.Option(name(m)));
            }
            bar.add(new Dropdown("Month", opts, monthIndex, i -> {
                monthIndex = i;
                lastNote = "";
                refresh();
            }));
        }
        column.add(bar, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTH;
        c.gridx = 0;
        c.weightx = 0.64;
        c.weighty = 1;
        c.insets = new Insets(GAP, 0, 0, GAP / 2);
        grid.add(reviewCard(), c);
        c.gridx = 1;
        c.weightx = 0.36;
        c.insets = new Insets(GAP, GAP / 2, 0, 0);
        grid.add(exportCard(), c);
        column.add(grid, BorderLayout.CENTER);
        add(column, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private static String name(YearMonth m) {
        return m.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + m.getYear();
    }

    // ---- review ----

    private Card reviewCard() {
        Card card = new Card("Monthly review", name(month()), "01").minHeight(440);
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        if (months.isEmpty()) {
            col.add(Text.wrapped("Import a bank CSV first. The review needs at least one month of transactions.", Theme.p().muted(), 14f, false, TEXT));
            card.body().add(col, BorderLayout.NORTH);
            return card;
        }
        Optional<MonthlyReview> existing = ctx.facade().getMonthlyReview(month());
        if (existing.isPresent()) {
            MonthlyReview r = existing.get();
            col.add(Text.wrapped(r.narrative(), Theme.p().text(), 14.5f, false, TEXT));
            col.add(Box.createVerticalStrut(18));
            if (r.healthScore().isAvailable()) {
                col.add(scorePanel(r.healthScore()));
                col.add(Box.createVerticalStrut(16));
            }
            if (!lastNote.isEmpty()) {
                col.add(Text.wrapped(lastNote, Theme.p().muted(), 12.5f, false, TEXT));
                col.add(Box.createVerticalStrut(12));
            }
        } else {
            col.add(Text.wrapped("No review for this month yet. Generate one to get a short written summary, your health score and up to three fixes you can approve.",
                    Theme.p().muted(), 14f, false, TEXT));
            col.add(Box.createVerticalStrut(18));
        }
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(0f);
        buttons.add(new FlatButton(working ? "Working…" : existing.isPresent() ? "Generate again" : "Generate review", null, true, this::generate));
        if (existing.isPresent()) {
            buttons.add(Box.createHorizontalStrut(8));
            buttons.add(new FlatButton("See proposals", null, false, () -> ctx.navigate(AppContext.PROPOSALS)));
            if (!existing.get().traceId().isEmpty()) {
                buttons.add(Box.createHorizontalStrut(8));
                buttons.add(new FlatButton("How was this written?", null, false,
                        () -> TraceDialog.show(ctx.window(), ctx.facade(), existing.get().traceId())));
            }
        }
        col.add(buttons);
        card.body().add(col, BorderLayout.NORTH);
        return card;
    }

    private JPanel scorePanel(HealthScore s) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setAlignmentX(0f);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        Label head = new Label("Health score  " + s.total() + " / 100"
                + s.changeFromLastMonth().stream().mapToObj(d -> d == 0 ? "   (same as last month)" : "   (" + (d > 0 ? "+" : "") + d + " from last month)").findFirst().orElse(""),
                Theme.bold(14f), Theme.p().text());
        head.setAlignmentX(0f);
        p.add(head);
        p.add(Box.createVerticalStrut(8));
        for (ScoreComponent c : s.components()) {
            double f = c.maxPoints() == 0 ? 0 : c.points() / (double) c.maxPoints();
            Color col = f >= 0.7 ? Theme.p().good() : f >= 0.4 ? Theme.p().warn() : Theme.p().danger();
            ProgressRow row = new ProgressRow(c.name() + " · " + c.detail(), c.points() + " / " + c.maxPoints(), f, col, col);
            row.setAlignmentX(0f);
            p.add(row);
        }
        return p;
    }

    private void generate() {
        if (working) {
            return;
        }
        working = true;
        YearMonth m = month();
        refresh();
        new SwingWorker<ReviewResult, Void>() {
            @Override
            protected ReviewResult doInBackground() {
                return ctx.facade().generateMonthlyReview(m);
            }

            @Override
            protected void done() {
                working = false;
                try {
                    ReviewResult r = get();
                    StringBuilder note = new StringBuilder();
                    if (r.metricsOnly()) {
                        note.append("Written from your numbers only (the AI was not used). ");
                    }
                    if (r.replacedEarlier()) {
                        note.append("Replaced the earlier review. ");
                    }
                    if (!r.proposals().isEmpty()) {
                        note.append(r.proposals().size()).append(r.proposals().size() == 1 ? " fix is" : " fixes are").append(" waiting under Proposals. ");
                    } else if (r.alreadyWaiting() > 0) {
                        note.append(r.alreadyWaiting()).append(" earlier suggestions are still waiting under Proposals. ");
                    }
                    note.append(r.note());
                    lastNote = note.toString().trim();
                    ctx.toast("Review for " + name(m) + " is ready.", Toast.Kind.SUCCESS);
                } catch (Exception e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    lastNote = "";
                    ctx.toast(cause.getMessage() == null ? "Could not make the review." : cause.getMessage(), Toast.Kind.ERROR);
                }
                ctx.changed();
            }
        }.execute();
    }

    // ---- export ----

    private Card exportCard() {
        Card card = new Card("Save a copy", "Export", "02").minHeight(440);
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.add(Text.wrapped("Saves " + (months.isEmpty() ? "the month" : name(month())) + " as a file you can open in a spreadsheet or share: spending by category, budget, goals and the review.",
                Theme.p().muted(), 13.5f, false, 300));
        col.add(Box.createVerticalStrut(16));
        List<Menus.Option> formats = List.of(new Menus.Option("Markdown (.md)"), new Menus.Option("CSV (.csv)"), new Menus.Option("Plain text (.txt)"));
        JPanel pick = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pick.setOpaque(false);
        pick.setAlignmentX(0f);
        pick.add(new Dropdown("Format", formats, formatIndex, i -> formatIndex = i));
        col.add(pick);
        col.add(Box.createVerticalStrut(14));
        JPanel save = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        save.setOpaque(false);
        save.setAlignmentX(0f);
        save.add(new FlatButton("Save report…", null, true, this::export));
        col.add(save);
        card.body().add(col, BorderLayout.NORTH);
        return card;
    }

    private void export() {
        if (months.isEmpty()) {
            ctx.toast("Nothing to export yet. Import a CSV first.", Toast.Kind.WARNING);
            return;
        }
        ExportFormat format = new ExportFormat[] {ExportFormat.MARKDOWN, ExportFormat.CSV, ExportFormat.TEXT}[formatIndex];
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save report");
        chooser.setSelectedFile(new File(System.getProperty("user.home"), "wealthsei-" + month() + "." + format.extension()));
        if (chooser.showSaveDialog(ctx.window()) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path target = chooser.getSelectedFile().toPath();
        boolean overwrite = false;
        if (Files.exists(target)) {
            int answer = JOptionPane.showConfirmDialog(ctx.window(), target.getFileName() + " already exists. Replace it?", "Replace file?",
                    JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
            overwrite = true;
        }
        try {
            Path written = ctx.facade().exportReport(month(), format, target, overwrite);
            ctx.toast("Saved " + written.getFileName() + ".", Toast.Kind.SUCCESS);
        } catch (RuntimeException e) {
            ctx.toast(e.getMessage() == null ? "Could not save the file." : e.getMessage(), Toast.Kind.ERROR);
        }
    }
}
