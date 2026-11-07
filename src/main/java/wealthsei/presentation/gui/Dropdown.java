package wealthsei.presentation.gui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.JComponent;

/** A rounded drop-down: shows the current choice and opens a dark list. */
@SuppressWarnings({"serial", "this-escape"})
public class Dropdown extends JComponent {

    private final List<Menus.Option> options;
    private final String prefix;
    private final IntConsumer onChange;
    private int selected;

    /** @param prefix muted text before the value, such as "Month"; may be empty */
    public Dropdown(String prefix, List<Menus.Option> options, int selected, IntConsumer onChange) {
        this.prefix = prefix;
        this.options = options;
        this.selected = selected;
        this.onChange = onChange;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Menus.popup(Dropdown.this, Dropdown.this.options, Dropdown.this.selected, i -> {
                    Dropdown.this.selected = i;
                    repaint();
                    Dropdown.this.onChange.accept(i);
                });
            }
        });
    }

    public int selectedIndex() {
        return selected;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(Theme.regular(13f));
        int widest = 0;
        for (Menus.Option o : options) {
            widest = Math.max(widest, fm.stringWidth(o.label()));
        }
        int pre = prefix.isEmpty() ? 0 : fm.stringWidth(prefix) + 8;
        return new Dimension(widest + pre + 64, 40);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        int w = getWidth();
        int h = getHeight();
        g.setColor(Theme.p().surfaceRaised());
        g.fillRoundRect(0, 0, w, h, h, h);
        g.setColor(Theme.p().border());
        g.drawRoundRect(0, 0, w - 1, h - 1, h, h);
        g.setFont(Theme.regular(13f));
        FontMetrics fm = g.getFontMetrics();
        int x = 18;
        int base = (h - fm.getHeight()) / 2 + fm.getAscent();
        if (!prefix.isEmpty()) {
            g.setColor(Theme.p().muted());
            g.drawString(prefix, x, base);
            x += fm.stringWidth(prefix) + 8;
        }
        Menus.Option o = options.get(selected);
        if (o.dot() != null) {
            g.setColor(o.dot());
            g.fillOval(x, h / 2 - 4, 8, 8);
            x += 14;
        }
        g.setFont(Theme.bold(13f));
        g.setColor(Theme.p().text());
        g.drawString(o.label(), x, base);
        g.setStroke(new java.awt.BasicStroke(1.6f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        g.setColor(Theme.p().muted());
        g.drawLine(w - 26, h / 2 - 2, w - 22, h / 2 + 2);
        g.drawLine(w - 22, h / 2 + 2, w - 18, h / 2 - 2);
        g.dispose();
    }
}
