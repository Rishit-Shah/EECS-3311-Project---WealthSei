package wealthsei.presentation.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;

/** A numbered line in "Needs attention": number tile, text, and a small arrow. */
@SuppressWarnings({"serial", "this-escape"})
public class AttentionRow extends JComponent {

    private final int number;
    private final Color colour;
    private final String main;
    private final String sub;
    private final boolean clickable;

    public AttentionRow(int number, Color colour, String main, String sub, Runnable onClick) {
        this.number = number;
        this.colour = colour;
        this.main = main;
        this.sub = sub;
        this.clickable = onClick != null;
        setOpaque(false);
        if (onClick != null) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onClick.run();
                }
            });
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 60);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, 60);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        int w = getWidth();
        int h = getHeight();
        g.setColor(Theme.alpha(colour, 46));
        g.fillRoundRect(0, (h - 34) / 2, 34, 34, 11, 11);
        g.setColor(colour);
        g.setFont(Theme.bold(11.5f));
        String n = String.format("%02d", number);
        java.awt.FontMetrics fm = g.getFontMetrics();
        g.drawString(n, (34 - fm.stringWidth(n)) / 2f, h / 2f + fm.getAscent() / 2f - 1);
        int x = 50;
        int avail = w - x - 26;
        g.setFont(Theme.bold(13.5f));
        g.setColor(Theme.p().text());
        g.drawString(ListRow.clip(main, g.getFontMetrics(), avail), x, h / 2 - 3);
        g.setFont(Theme.regular(12f));
        g.setColor(Theme.p().muted());
        g.drawString(ListRow.clip(sub, g.getFontMetrics(), avail), x, h / 2 + 14);
        if (clickable) {
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(Theme.p().muted());
            g.drawLine(w - 14, h / 2 - 5, w - 9, h / 2);
            g.drawLine(w - 9, h / 2, w - 14, h / 2 + 5);
        }
        g.setColor(Theme.p().gridLine());
        g.fillRect(0, h - 1, w, 1);
        g.dispose();
    }
}
