package wealthsei.presentation.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.util.List;
import javax.swing.JComponent;

/** A ring split into coloured slices with a total in the middle. */
@SuppressWarnings({"serial", "this-escape"})
public class DonutChart extends JComponent {

    /** One slice. */
    public record Slice(String label, double value, Color colour) {
    }

    private final List<Slice> slices;
    private final String centreTop;
    private final String centreBottom;

    public DonutChart(List<Slice> slices, String centreTop, String centreBottom) {
        this.slices = slices;
        this.centreTop = centreTop;
        this.centreBottom = centreBottom;
        setOpaque(false);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(190, 190);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        int size = Math.min(getWidth(), getHeight()) - 8;
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;
        float thickness = Math.max(16f, size * 0.15f);
        double total = slices.stream().mapToDouble(Slice::value).sum();
        double inset = thickness / 2;
        if (total <= 0) {
            g.setColor(Theme.p().surfaceRaised());
            g.setStroke(new BasicStroke(thickness));
            g.draw(new Arc2D.Double(x + inset, y + inset, size - thickness, size - thickness, 0, 360, Arc2D.OPEN));
        } else {
            double angle = 90;
            double gap = slices.size() > 1 ? 2.2 : 0;
            for (Slice s : slices) {
                double sweep = -360.0 * s.value() / total;
                g.setColor(s.colour());
                g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
                double drawSweep = sweep + (sweep < -gap ? gap : 0);
                g.draw(new Arc2D.Double(x + inset, y + inset, size - thickness, size - thickness, angle, drawSweep, Arc2D.OPEN));
                angle += sweep;
            }
        }
        g.setFont(Theme.bold(size / 8.2f));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Theme.p().text());
        g.drawString(centreTop, getWidth() / 2f - fm.stringWidth(centreTop) / 2f, getHeight() / 2f + fm.getAscent() / 2f - 6);
        g.setFont(Theme.regular(11.5f));
        fm = g.getFontMetrics();
        g.setColor(Theme.p().muted());
        g.drawString(centreBottom, getWidth() / 2f - fm.stringWidth(centreBottom) / 2f, getHeight() / 2f + fm.getAscent() / 2f + 14);
        g.dispose();
    }
}
