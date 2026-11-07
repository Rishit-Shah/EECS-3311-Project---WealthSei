package wealthsei.presentation.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

/** A key for the chart: colour square, name, and an amount on the right. */
@SuppressWarnings({"serial", "this-escape"})
public class LegendRow extends JComponent {

    private final Color colour;
    private final String name;
    private final String amount;

    public LegendRow(Color colour, String name, String amount) {
        this.colour = colour;
        this.name = name;
        this.amount = amount;
        setOpaque(false);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(180, 30);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, 30);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        int w = getWidth();
        int h = getHeight();
        g.setColor(colour);
        g.fillRoundRect(0, h / 2 - 5, 10, 10, 4, 4);
        g.setFont(Theme.bold(12.5f));
        FontMetrics fm = g.getFontMetrics();
        int aw = fm.stringWidth(amount);
        g.setColor(Theme.p().muted());
        int base = (h - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(amount, w - aw, base);
        g.setFont(Theme.regular(13f));
        fm = g.getFontMetrics();
        g.setColor(Theme.p().text());
        g.drawString(ListRow.clip(name, fm, w - 20 - aw - 10), 20, base);
        g.dispose();
    }
}
