package wealthsei.presentation.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import javax.swing.JComponent;

/** A goal as a small progress ring with its name and amounts beside it. */
@SuppressWarnings({"serial", "this-escape"})
public class GoalRow extends JComponent {

    private final String name;
    private final String detail;
    private final double fraction;
    private final Color colour;

    public GoalRow(String name, String detail, double fraction, Color colour) {
        this.name = name;
        this.detail = detail;
        this.fraction = Math.max(0, Math.min(1, fraction));
        this.colour = colour;
        setOpaque(false);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 76);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, 76);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        int h = getHeight();
        int d = 52;
        int y = (h - d) / 2;
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Theme.p().surfaceRaised());
        g.draw(new Arc2D.Double(4, y + 4, d - 8, d - 8, 0, 360, Arc2D.OPEN));
        if (fraction > 0) {
            g.setColor(colour);
            g.draw(new Arc2D.Double(4, y + 4, d - 8, d - 8, 90, -360 * fraction, Arc2D.OPEN));
        }
        String pct = Math.round(fraction * 100) + "%";
        g.setFont(Theme.bold(12f));
        java.awt.FontMetrics fm = g.getFontMetrics();
        g.setColor(Theme.p().text());
        g.drawString(pct, (d - fm.stringWidth(pct)) / 2f, y + d / 2f + fm.getAscent() / 2f - 1);
        int x = d + 16;
        g.setFont(Theme.bold(14f));
        g.setColor(Theme.p().text());
        g.drawString(ListRow.clip(name, g.getFontMetrics(), getWidth() - x), x, h / 2 - 3);
        g.setFont(Theme.regular(12f));
        g.setColor(Theme.p().muted());
        g.drawString(ListRow.clip(detail, g.getFontMetrics(), getWidth() - x), x, h / 2 + 15);
        g.setColor(Theme.p().gridLine());
        g.fillRect(0, h - 1, getWidth(), 1);
        g.dispose();
    }
}
