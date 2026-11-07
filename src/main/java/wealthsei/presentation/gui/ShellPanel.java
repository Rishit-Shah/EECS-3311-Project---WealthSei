package wealthsei.presentation.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import wealthsei.domain.ImportResult;
import wealthsei.facade.WealthSeiFacade;

/** Top bar, headline, the current screen and the assistant in the corner. Kept apart from the window so it can be drawn without a screen. */
@SuppressWarnings({"serial", "this-escape"})
public class ShellPanel extends JLayeredPane implements AppContext {

    private static final int MAX_WIDTH = 1200;

    private final WealthSeiFacade facade;
    private final Supplier<LocalDate> today;
    private final boolean aiOn;
    private final List<View> views = new ArrayList<>();
    private final JPanel base = new JPanel(new BorderLayout()) {
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.quality(g);
            Theme.paintPage(g, getWidth(), getHeight());
            g.dispose();
        }
    };
    private final ChatDock dock;
    private final ToastLayer toasts = new ToastLayer();
    private TopBar bar;
    private LocalDate shownDate;
    private int selected;

    public ShellPanel(WealthSeiFacade facade, Supplier<LocalDate> today, boolean aiOn, String dataFolder) {
        this.facade = facade;
        this.today = today;
        this.aiOn = aiOn;
        base.setOpaque(false);
        this.dock = new ChatDock(facade, aiOn);
        add(base, DEFAULT_LAYER);
        add(dock, PALETTE_LAYER);
        add(toasts, POPUP_LAYER);
        facade.addAlertListener(alert -> javax.swing.SwingUtilities.invokeLater(() -> toast(alert.message(),
                alert.severity() == wealthsei.domain.AlertSeverity.OVER ? Toast.Kind.ERROR
                        : alert.severity() == wealthsei.domain.AlertSeverity.WARNING ? Toast.Kind.WARNING : Toast.Kind.INFO)));
        this.shownDate = today.get();
        createViews();
        build();
    }

    @Override
    public void doLayout() {
        base.setBounds(0, 0, getWidth(), getHeight());
        dock.setBounds(0, 0, getWidth(), getHeight());
        toasts.setBounds(0, 0, getWidth(), getHeight());
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(1380, 880);
    }

    /** Shows the assistant popup (used for previews and by screens that want to point to it). */
    public ChatDock dock() {
        return dock;
    }

    /** The screen at this position (for previews). */
    public View view(int index) {
        return views.get(index);
    }

    public void select(int index) {
        if (index >= 0 && index < views.size()) {
            selected = index;
            rebuild();
        }
    }

    /** Called when the window comes back to the front: if midnight passed meanwhile, every date-based figure is recalculated. */
    public void refreshIfDateChanged() {
        if (!today.get().equals(shownDate)) {
            shownDate = today.get();
            rebuild();
        }
    }

    /** Re-reads the data and redraws the current screen. */
    public void refreshAll() {
        rebuild();
    }

    private void rebuild() {
        for (View v : views) {
            v.refresh();
        }
        base.removeAll();
        build();
        base.revalidate();
        base.repaint();
    }

    private void createViews() {
        views.add(new DashboardView(this));
        views.add(new TransactionsView(this));
        views.add(new GoalsView(this));
        views.add(new ProposalsView(this));
        views.add(new AssistantView(this));
        views.add(new ReportsView(this));
    }

    private void build() {

        FlatButton theme = new FlatButton("", Theme.isDark() ? Icons.Kind.SUN : Icons.Kind.MOON, false, () -> {
            Theme.setDark(!Theme.isDark());
            rebuild();
        }).onPage();
        FlatButton imp = new FlatButton("Import CSV", Icons.Kind.IMPORT, true, this::chooseAndImport);
        bar = new TopBar(i -> select(i), theme, imp);
        Icons.Kind[] icons = {Icons.Kind.DASHBOARD, Icons.Kind.TRANSACTIONS, Icons.Kind.GOALS, Icons.Kind.PROPOSALS,
            Icons.Kind.ASSISTANT, Icons.Kind.REPORTS};
        for (int i = 0; i < views.size(); i++) {
            bar.addItem(views.get(i).title(), icons[i]);
        }
        bar.setSelected(selected);
        bar.setBadge(3, facade.listPendingProposals(today.get()).size());
        base.add(bar, BorderLayout.NORTH);

        View current = views.get(selected);
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new javax.swing.BoxLayout(column, javax.swing.BoxLayout.Y_AXIS));
        HeroHeader hero = new HeroHeader(current.eyebrow(), current.headline()[0], current.headline()[1], current.subtitle());
        hero.setAlignmentX(0f);
        current.setAlignmentX(0f);
        column.add(hero);
        column.add(javax.swing.Box.createVerticalStrut(18));
        column.add(current);
        column.add(javax.swing.Box.createVerticalStrut(110));

        Page page = new Page();
        page.setLayout(new CenteredColumn(MAX_WIDTH));
        page.add(column);
        JScrollPane scroll = new JScrollPane(page, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        ModernScrollBar.apply(scroll);
        base.add(scroll, BorderLayout.CENTER);
    }

    // ---- AppContext ----

    @Override
    public WealthSeiFacade facade() {
        return facade;
    }

    @Override
    public LocalDate today() {
        return today.get();
    }

    @Override
    public boolean aiOn() {
        return aiOn;
    }

    @Override
    public void navigate(int screen) {
        select(screen);
    }

    @Override
    public void toast(String message, Toast.Kind kind) {
        toasts.show(new Toast(message, kind));
    }

    @Override
    public void changed() {
        for (View v : views) {
            v.refresh();
        }
        if (bar != null) {
            bar.setBadge(PROPOSALS, facade.listPendingProposals(today.get()).size());
        }
    }

    @Override
    public java.awt.Component window() {
        return this;
    }

    @Override
    public void askInPopup(String question) {
        dock.setOpen(true);
        dock.chat().send(question);
    }

    @Override
    public void importCsv() {
        chooseAndImport();
    }

    /** Holds the toasts, top right, newest at the bottom. Never takes mouse events. */
    private static final class ToastLayer extends JPanel {
        private static final long serialVersionUID = 1L;

        ToastLayer() {
            super(null);
            setOpaque(false);
        }

        void show(Toast t) {
            add(t);
            doLayout();
            repaint();
        }

        @Override
        public boolean contains(int x, int y) {
            return false;
        }

        @Override
        public void doLayout() {
            int y = 74;
            for (java.awt.Component c : getComponents()) {
                Dimension d = c.getPreferredSize();
                c.setBounds(getWidth() - d.width - 24, y, d.width, d.height);
                y += d.height + 4;
            }
        }
    }

    private void chooseAndImport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a bank CSV");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        try {
            ImportResult r = facade.importTransactions(Path.of(file.getPath()));
            refreshAll();
            JOptionPane.showMessageDialog(this, "Imported " + r.imported() + " transactions"
                    + (r.duplicates() > 0 ? ", skipped " + r.duplicates() + " duplicates" : "")
                    + (r.rejected().isEmpty() ? "" : ", left out " + r.rejected().size() + " rows"), "Import finished",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Could not import", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Scrolls with the window, always as wide as the viewport. */
    private static final class Page extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        Page() {
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle r, int o, int d) {
            return 20;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle r, int o, int d) {
            return Math.max(60, r.height - 60);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** Puts its one child in the middle, at most {@code maxWidth} wide, with side margins. */
    private static final class CenteredColumn implements LayoutManager {
        private final int maxWidth;

        CenteredColumn(int maxWidth) {
            this.maxWidth = maxWidth;
        }

        @Override
        public void addLayoutComponent(String name, java.awt.Component comp) {
        }

        @Override
        public void removeLayoutComponent(java.awt.Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            java.awt.Component c = parent.getComponent(0);
            return new Dimension(900, c.getPreferredSize().height + 8);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return new Dimension(900, parent.getComponent(0).getMinimumSize().height);
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets in = parent.getInsets();
            int avail = parent.getWidth() - in.left - in.right;
            int w = Math.min(maxWidth, Math.max(400, avail - 64));
            java.awt.Component c = parent.getComponent(0);
            int h = c.getPreferredSize().height;
            c.setBounds(in.left + (avail - w) / 2, in.top, w, h);
        }
    }
}
