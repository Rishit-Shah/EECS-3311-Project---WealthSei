package wealthsei.presentation.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JPanel;

/** The bar across the top: logo, the six screens as a pill group, theme switch and Import. */
@SuppressWarnings({"serial", "this-escape"})
public class TopBar extends JPanel {

    private record Item(String name, Icons.Kind icon) {
    }

    private final List<Item> items = new ArrayList<>();
    private int selected;
    private int badgeIndex = -1;
    private int badgeCount;
    private final Tabs tabs = new Tabs();
    private final Consumer<Integer> onSelect;

    public TopBar(Consumer<Integer> onSelect, JComponent... actions) {
        super(new BorderLayout());
        this.onSelect = onSelect;
        setOpaque(false);
        setPreferredSize(new Dimension(100, 72));
        add(new Logo(), BorderLayout.WEST);
        JPanel centre = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 16));
        centre.setOpaque(false);
        centre.add(tabs);
        add(centre, BorderLayout.CENTER);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 16));
        right.setOpaque(false);
        for (JComponent a : actions) {
            right.add(a);
        }
        right.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 14));
        add(right, BorderLayout.EAST);
    }

    public void addItem(String name, Icons.Kind icon) {
        items.add(new Item(name, icon));
    }

    public void setSelected(int index) {
        selected = index;
    }

    public void setBadge(int index, int count) {
        badgeIndex = index;
        badgeCount = count;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setColor(Theme.navBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Theme.navBorder());
        g.fillRect(0, getHeight() - 1, getWidth(), 1);
        g.dispose();
    }

    /** The orange circle with a W, and the name. */
    private static final class Logo extends JComponent {
        private static final long serialVersionUID = 1L;

        Logo() {
            setOpaque(false);
            setPreferredSize(new Dimension(230, 72));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.quality(g);
            g.setPaint(new GradientPaint(24, 18, Theme.mix(Theme.p().accent(), Color.WHITE, 0.15f), 56, 54, Theme.p().accent()));
            g.fillOval(26, 16, 40, 40);
            Icons.draw(g, Icons.Kind.LOGO, 33, 23, 26, Theme.ON_ORANGE);
            g.setColor(Theme.pageText());
            g.setFont(Theme.tracked(Theme.bold(18f), -0.02f));
            g.drawString("WealthSei", 78, 42);
            g.dispose();
        }
    }

    /** The pill group in the middle. */
    private final class Tabs extends JComponent {
        private static final long serialVersionUID = 1L;
        private int hovered = -1;

        Tabs() {
            setOpaque(false);
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int i = at(e.getX(), e.getY());
                    if (i >= 0) {
                        onSelect.accept(i);
                    }
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    int i = at(e.getX(), e.getY());
                    if (i != hovered) {
                        hovered = i;
                        setCursor(Cursor.getPredefinedCursor(i >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = -1;
                    repaint();
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        private int tabWidth(int i, FontMetrics fm) {
            return fm.stringWidth(items.get(i).name()) + 52 + (i == badgeIndex && badgeCount > 0 ? 24 : 0);
        }

        private int at(int x, int y) {
            FontMetrics fm = getFontMetrics(Theme.font(java.awt.Font.PLAIN, 13f));
            int cx = 4;
            for (int i = 0; i < items.size(); i++) {
                int w = tabWidth(i, fm);
                if (x >= cx && x < cx + w && y >= 4 && y < getHeight() - 4) {
                    return i;
                }
                cx += w;
            }
            return -1;
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(Theme.font(java.awt.Font.PLAIN, 13f));
            int w = 8;
            for (int i = 0; i < items.size(); i++) {
                w += tabWidth(i, fm);
            }
            return new Dimension(w, 42);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.quality(g);
            int h = getHeight();
            g.setColor(Theme.alpha(Theme.pageButton(), Theme.isDark() ? 255 : 200));
            g.fillRoundRect(0, 0, getWidth() - 1, h - 1, h, h);
            g.setColor(Theme.pageButtonBorder());
            g.drawRoundRect(0, 0, getWidth() - 1, h - 1, h, h);
            g.setFont(Theme.font(java.awt.Font.PLAIN, 13f));
            FontMetrics fm = g.getFontMetrics();
            int cx = 4;
            for (int i = 0; i < items.size(); i++) {
                int w = tabWidth(i, fm);
                boolean sel = i == selected;
                if (sel) {
                    g.setColor(Theme.tabSelected());
                    g.fillRoundRect(cx, 4, w, h - 9, h, h);
                } else if (i == hovered) {
                    g.setColor(Theme.alpha(Theme.pageMuted(), 40));
                    g.fillRoundRect(cx, 4, w, h - 9, h, h);
                }
                Color c = sel ? Theme.tabSelectedText() : Theme.tabText();
                Icons.draw(g, items.get(i).icon(), cx + 16, (h - 16) / 2.0 - 0.5, 16, c);
                g.setFont(sel ? Theme.bold(13f) : Theme.font(java.awt.Font.PLAIN, 13f));
                g.setColor(c);
                g.drawString(items.get(i).name(), cx + 38, (h - fm.getHeight()) / 2f + fm.getAscent() - 1);
                if (i == badgeIndex && badgeCount > 0) {
                    String b = String.valueOf(badgeCount);
                    g.setFont(Theme.bold(10.5f));
                    int bw = Math.max(18, g.getFontMetrics().stringWidth(b) + 10);
                    int bx = cx + w - bw - 10;
                    g.setColor(Theme.p().accent());
                    g.fillRoundRect(bx, h / 2 - 9, bw, 18, 18, 18);
                    g.setColor(Theme.ON_ORANGE);
                    g.drawString(b, bx + (bw - g.getFontMetrics().stringWidth(b)) / 2f, h / 2f + 4);
                }
                cx += w;
            }
            g.dispose();
        }
    }
}
