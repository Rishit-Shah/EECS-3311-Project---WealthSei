package wealthsei.presentation.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeSet;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import wealthsei.domain.Categories;
import wealthsei.domain.Category;
import wealthsei.domain.Money;
import wealthsei.domain.Transaction;
import wealthsei.domain.TxFilter;

/** Every transaction, with month, category and search filters, one-click category fixes, and undo/redo. */
@SuppressWarnings({"serial", "this-escape"})
public class TransactionsView extends View {

    private static final int PAGE = 120;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);

    private final AppContext ctx;
    private final List<Category> categoryChoices = new ArrayList<>();
    private List<YearMonth> months = List.of();
    private int monthIndex;
    private int categoryIndex;
    private String search = "";
    private int shown = PAGE;
    private final JPanel listHost = new JPanel(new BorderLayout());
    private final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    private final Label summary = new Label("", Theme.regular(13f), Theme.pageMuted());

    public TransactionsView(AppContext ctx) {
        this.ctx = ctx;
        listHost.setOpaque(false);
        controls.setOpaque(false);
        categoryChoices.addAll(Categories.defaults());
        categoryChoices.add(Category.UNCATEGORIZED);
        refresh();
    }

    @Override
    public String title() {
        return "Transactions";
    }

    @Override
    public String[] headline() {
        return new String[] {"Every transaction,", "accounted for."};
    }

    @Override
    public String subtitle() {
        return "Search, filter and fix a category in one click. WealthSei remembers the merchant next time, and every fix can be undone.";
    }

    @Override
    public void refresh() {
        removeAll();
        List<Transaction> all = ctx.facade().listTransactions(TxFilter.all());
        TreeSet<YearMonth> found = new TreeSet<>(Comparator.reverseOrder());
        for (Transaction t : all) {
            found.add(YearMonth.from(t.date()));
        }
        months = new ArrayList<>(found);
        if (monthIndex > months.size()) {
            monthIndex = 0;
        }

        Card card = new Card("Activity", "All transactions", "01");
        JPanel body = card.body();
        body.setLayout(new BorderLayout(0, 14));
        buildControls();
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(controls, BorderLayout.CENTER);
        summary.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 14, 0, 0));
        top.add(summary, BorderLayout.SOUTH);
        body.add(top, BorderLayout.NORTH);
        listHost.removeAll();
        body.add(listHost, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
        fillList();
        revalidate();
        repaint();
    }

    private void buildControls() {
        controls.removeAll();
        List<Menus.Option> monthOptions = new ArrayList<>();
        monthOptions.add(new Menus.Option("All time"));
        for (YearMonth m : months) {
            monthOptions.add(new Menus.Option(m.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + m.getYear()));
        }
        controls.add(new Dropdown("Month", monthOptions, Math.min(monthIndex, monthOptions.size() - 1), i -> {
            monthIndex = i;
            shown = PAGE;
            fillList();
        }));
        List<Menus.Option> catOptions = new ArrayList<>();
        catOptions.add(new Menus.Option("All categories"));
        for (Category c : categoryChoices) {
            catOptions.add(new Menus.Option(c.name(), Theme.categoryColour(c)));
        }
        controls.add(new Dropdown("Category", catOptions, categoryIndex, i -> {
            categoryIndex = i;
            shown = PAGE;
            fillList();
        }));
        SearchField field = new SearchField("Search merchant", 220, text -> {
            search = text;
            shown = PAGE;
            fillList();
        });
        field.setText(search);
        controls.add(field);
        controls.add(new FlatButton("Add transaction", null, true, this::addTransaction));
        controls.add(new FlatButton("Undo", null, false, this::undo));
        controls.add(new FlatButton("Redo", null, false, this::redo));
    }

    private List<Transaction> filtered() {
        TxFilter f = TxFilter.all();
        if (monthIndex > 0 && monthIndex <= months.size()) {
            YearMonth m = months.get(monthIndex - 1);
            f = TxFilter.between(m.atDay(1), m.atEndOfMonth());
        }
        if (categoryIndex > 0) {
            f = f.withCategory(categoryChoices.get(categoryIndex - 1));
        }
        if (!search.isBlank()) {
            f = f.withMerchant(search.trim());
        }
        List<Transaction> list = new ArrayList<>(ctx.facade().listTransactions(f));
        list.sort(Comparator.comparing(Transaction::date).reversed().thenComparing(Transaction::id));
        return list;
    }

    private void fillList() {
        listHost.removeAll();
        List<Transaction> list = filtered();
        Money in = Money.ZERO;
        Money out = Money.ZERO;
        int uncategorised = 0;
        for (Transaction t : list) {
            if (t.income()) {
                in = in.add(t.amount());
            } else {
                out = out.add(t.amount());
            }
            if (t.category().isUncategorized()) {
                uncategorised++;
            }
        }
        summary.setText(list.size() + " shown   ·   in " + Theme.money(in) + "   ·   out " + Theme.money(out)
                + (uncategorised > 0 ? "   ·   " + uncategorised + " need a category" : ""));
        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        if (list.isEmpty()) {
            Label empty = new Label(ctx.facade().listTransactions(TxFilter.all()).isEmpty()
                    ? "Nothing here yet. Use Import CSV to bring in a bank export."
                    : "No transactions match these filters.", Theme.regular(14f), Theme.p().muted());
            empty.setBorder(javax.swing.BorderFactory.createEmptyBorder(26, 4, 26, 4));
            rows.add(empty);
        }
        int count = Math.min(shown, list.size());
        for (int i = 0; i < count; i++) {
            rows.add(new Row(list.get(i)));
        }
        if (list.size() > count) {
            rows.add(Box.createVerticalStrut(12));
            FlatButton more = new FlatButton("Show " + Math.min(PAGE, list.size() - count) + " more", null, false, () -> {
                shown += PAGE;
                fillList();
            });
            more.setAlignmentX(0f);
            rows.add(more);
        }
        listHost.add(rows, BorderLayout.NORTH);
        listHost.revalidate();
        listHost.repaint();
        controls.revalidate();
        controls.repaint();
    }

    private void change(Transaction tx, Category target) {
        if (target.equals(tx.category())) {
            return;
        }
        try {
            ctx.facade().correctCategory(tx.id(), target);
            ctx.toast(tx.merchant() + " moved to " + target.name() + ". Next time it is filed there automatically.", Toast.Kind.SUCCESS);
            afterChange();
        } catch (RuntimeException e) {
            ctx.toast(e.getMessage(), Toast.Kind.ERROR);
        }
    }

    private void addTransaction() {
        String[] v = Dialogs.form(this, "Add a transaction",
                "Type it in by hand. Leave category empty and WealthSei files it for you. Type 'income' in the last box for money received.",
                new String[]{"Date (YYYY-MM-DD)", "Amount", "Merchant", "Category (optional, e.g. groceries)", "Type 'income' if money received (optional)"},
                new String[]{LocalDate.now().toString(), "", "", "", ""}, "Add");
        if (v == null) {
            return;
        }
        try {
            java.util.Optional<Category> chosen = v[3].isBlank() ? java.util.Optional.empty()
                    : java.util.Optional.of(Categories.find(v[3].trim().toLowerCase(Locale.ROOT))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown category '" + v[3] + "'")));
            Transaction tx = ctx.facade().addTransaction(new wealthsei.domain.NewTransaction(
                    Dialogs.date(v[0]), Dialogs.money(v[1]), v[2], "", chosen, v[4].equalsIgnoreCase("income")));
            ctx.toast("Added " + tx.merchant() + " to " + tx.category().name() + ".", Toast.Kind.SUCCESS);
            afterChange();
        } catch (RuntimeException e) {
            ctx.toast(e.getMessage(), Toast.Kind.ERROR);
        }
    }

    private void undo() {
        Optional<Transaction> t = ctx.facade().undoCorrection();
        if (t.isPresent()) {
            ctx.toast("Undone: " + t.get().merchant() + " is back in " + t.get().category().name() + ".", Toast.Kind.INFO);
            afterChange();
        } else {
            ctx.toast("Nothing to undo.", Toast.Kind.WARNING);
        }
    }

    private void redo() {
        Optional<Transaction> t = ctx.facade().redoCorrection();
        if (t.isPresent()) {
            ctx.toast("Redone: " + t.get().merchant() + " is in " + t.get().category().name() + " again.", Toast.Kind.INFO);
            afterChange();
        } else {
            ctx.toast("Nothing to redo.", Toast.Kind.WARNING);
        }
    }

    private void afterChange() {
        fillList();
        ctx.changed();
    }

    /** One transaction: date, merchant, a category chip you can click, and the amount. */
    private final class Row extends JComponent {
        private static final long serialVersionUID = 1L;
        private static final int CHIP_W = 176;
        private static final int AMOUNT_W = 120;
        private final Transaction tx;
        private boolean overChip;
        private boolean over;

        Row(Transaction tx) {
            this.tx = tx;
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (chip().contains(e.getPoint())) {
                        List<Menus.Option> options = new ArrayList<>();
                        int selected = -1;
                        for (int i = 0; i < categoryChoices.size(); i++) {
                            Category c = categoryChoices.get(i);
                            options.add(new Menus.Option(c.name(), Theme.categoryColour(c)));
                            if (c.equals(tx.category())) {
                                selected = i;
                            }
                        }
                        Menus.popup(Row.this, options, selected, i -> change(tx, categoryChoices.get(i)));
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    over = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    over = false;
                    overChip = false;
                    repaint();
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    boolean now = chip().contains(e.getPoint());
                    if (now != overChip) {
                        overChip = now;
                        setCursor(Cursor.getPredefinedCursor(now ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                        repaint();
                    }
                }
            });
        }

        private Rectangle chip() {
            return new Rectangle(getWidth() - AMOUNT_W - CHIP_W - 14, (getHeight() - 30) / 2, CHIP_W, 30);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(300, 58);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, 58);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.quality(g);
            int w = getWidth();
            int h = getHeight();
            if (over) {
                g.setColor(Theme.alpha(Theme.p().muted(), 22));
                g.fillRoundRect(-8, 2, w + 16, h - 4, 16, 16);
            }
            g.setColor(Theme.alpha(Theme.p().border(), 255));
            g.fillRect(0, h - 1, w, 1);

            g.setFont(Theme.bold(12f));
            g.setColor(Theme.p().muted());
            FontMetrics fm = g.getFontMetrics();
            g.drawString(tx.date().format(DAY).toUpperCase(Locale.ENGLISH), 0, (h - fm.getHeight()) / 2f + fm.getAscent());

            int textX = 80;
            int textW = chip().x - textX - 16;
            g.setFont(Theme.bold(14f));
            g.setColor(Theme.p().text());
            fm = g.getFontMetrics();
            boolean hasSub = !tx.description().isBlank() && !tx.description().equalsIgnoreCase(tx.merchant());
            float mainY = hasSub ? 22 : (h - fm.getHeight()) / 2f + fm.getAscent();
            g.drawString(ListRow.clip(tx.merchant(), fm, textW), textX, mainY);
            if (hasSub) {
                g.setFont(Theme.regular(12f));
                g.setColor(Theme.p().muted());
                fm = g.getFontMetrics();
                g.drawString(ListRow.clip(tx.description(), fm, textW), textX, 40);
            }

            Rectangle c = chip();
            Color col = Theme.categoryColour(tx.category());
            boolean unc = tx.category().isUncategorized();
            Color edge = unc ? Theme.p().warn() : col;
            g.setColor(Theme.alpha(edge, overChip ? 70 : 38));
            g.fillRoundRect(c.x, c.y, c.width, c.height, c.height, c.height);
            if (unc) {
                g.setColor(Theme.alpha(edge, 150));
                g.drawRoundRect(c.x, c.y, c.width - 1, c.height - 1, c.height, c.height);
            }
            g.setColor(edge);
            g.fillOval(c.x + 14, c.y + c.height / 2 - 4, 8, 8);
            g.setFont(Theme.bold(12.5f));
            g.setColor(Theme.p().text());
            fm = g.getFontMetrics();
            g.drawString(ListRow.clip(tx.category().name(), fm, c.width - 62), c.x + 30, c.y + (c.height - fm.getHeight()) / 2f + fm.getAscent());
            g.setStroke(new java.awt.BasicStroke(1.5f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            g.setColor(Theme.p().muted());
            int ax = c.x + c.width - 18;
            g.drawLine(ax - 3, c.y + c.height / 2 - 2, ax, c.y + c.height / 2 + 1);
            g.drawLine(ax, c.y + c.height / 2 + 1, ax + 3, c.y + c.height / 2 - 2);

            String amount = (tx.income() ? "+" : "-") + Theme.money(tx.amount());
            g.setFont(Theme.bold(14f));
            g.setColor(tx.income() ? Theme.p().good() : Theme.p().text());
            fm = g.getFontMetrics();
            g.drawString(amount, w - fm.stringWidth(amount), (h - fm.getHeight()) / 2f + fm.getAscent());
            g.dispose();
        }
    }

    /** For the render harness: today's date is not needed here, but kept for symmetry with other screens. */
    LocalDate now() {
        return ctx.today();
    }
}
