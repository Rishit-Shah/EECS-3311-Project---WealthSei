package wealthsei.presentation.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.swing.JComponent;

/** An area line chart of the balance over the next days, with the lowest point marked and a hover read-out. */
@SuppressWarnings({"serial", "this-escape"})
public class LineChart extends JComponent {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);

    private final List<LocalDate> dates;
    private final List<Double> values;
    private int hover = -1;

    public LineChart(List<LocalDate> dates, List<Double> values) {
        this.dates = dates;
        this.values = values;
        setOpaque(false);
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                hover = indexAt(e.getX());
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = -1;
                repaint();
            }
        };
        addMouseMotionListener(mouse);
        addMouseListener(mouse);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(500, 250);
    }

    private static final int LEFT = 46;
    private static final int RIGHT = 8;
    private static final int TOP = 12;
    private static final int BOTTOM = 26;

    private int indexAt(int x) {
        if (values.size() < 2) {
            return -1;
        }
        double t = (x - LEFT) / (double) (getWidth() - LEFT - RIGHT);
        int i = (int) Math.round(t * (values.size() - 1));
        return i < 0 || i >= values.size() ? -1 : i;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        if (values.size() < 2) {
            g.dispose();
            return;
        }
        int w = getWidth();
        int h = getHeight();
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        double pad = Math.max((max - min) * 0.18, 50);
        double lo = Math.min(min - pad, 0 > min ? min - pad : Math.max(0, min - pad));
        double hi = max + pad;
        int plotW = w - LEFT - RIGHT;
        int plotH = h - TOP - BOTTOM;

        g.setFont(Theme.regular(11f));
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i <= 4; i++) {
            double v = lo + (hi - lo) * i / 4;
            int y = TOP + plotH - (int) Math.round(plotH * i / 4.0);
            g.setColor(Theme.p().gridLine());
            g.setStroke(new BasicStroke(1f));
            g.drawLine(LEFT, y, w - RIGHT, y);
            g.setColor(Theme.p().muted());
            String label = Theme.compact(v);
            g.drawString(label, LEFT - 8 - fm.stringWidth(label), y + fm.getAscent() / 2 - 1);
        }
        int step = Math.max(1, (values.size() - 1) / 4);
        for (int i = 0; i < values.size(); i += step) {
            String label = dates.get(i).format(DAY);
            int x = LEFT + (int) Math.round(plotW * i / (double) (values.size() - 1));
            int tx = Math.max(LEFT - 10, Math.min(x - fm.stringWidth(label) / 2, w - RIGHT - fm.stringWidth(label)));
            g.setColor(Theme.p().muted());
            g.drawString(label, tx, h - 7);
        }

        Path2D line = new Path2D.Double();
        double[] xs = new double[values.size()];
        double[] ys = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            xs[i] = LEFT + plotW * i / (double) (values.size() - 1);
            ys[i] = TOP + plotH - plotH * (values.get(i) - lo) / (hi - lo);
            if (i == 0) {
                line.moveTo(xs[i], ys[i]);
            } else {
                line.lineTo(xs[i], ys[i]);
            }
        }
        Path2D area = new Path2D.Double(line);
        area.lineTo(xs[xs.length - 1], TOP + plotH);
        area.lineTo(xs[0], TOP + plotH);
        area.closePath();
        Color accent = Theme.p().accent();
        g.setPaint(new LinearGradientPaint(0, TOP, 0, TOP + plotH, new float[] {0f, 1f},
                new Color[] {Theme.alpha(accent, 85), Theme.alpha(accent, 0)}));
        g.fill(area);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(accent);
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(line);

        int lowest = 0;
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) < values.get(lowest)) {
                lowest = i;
            }
        }
        marker(g, xs[lowest], ys[lowest], Theme.p().warn());
        String low = "Lowest " + Theme.compact(values.get(lowest));
        g.setFont(Theme.bold(11f));
        g.setColor(Theme.p().warn());
        int lw = g.getFontMetrics().stringWidth(low);
        int lx = (int) Math.max(LEFT, Math.min(xs[lowest] - lw / 2.0, w - RIGHT - lw));
        g.drawString(low, lx, (int) ys[lowest] - 12);

        if (hover >= 0) {
            g.setColor(Theme.alpha(Theme.p().muted(), 90));
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {3f, 4f}, 0f));
            g.drawLine((int) xs[hover], TOP, (int) xs[hover], TOP + plotH);
            marker(g, xs[hover], ys[hover], accent);
            String t1 = dates.get(hover).format(DAY);
            String t2 = Theme.money(wealthsei.domain.Money.of(java.math.BigDecimal.valueOf(values.get(hover))));
            g.setFont(Theme.bold(12f));
            int bw = Math.max(g.getFontMetrics().stringWidth(t2), g.getFontMetrics().stringWidth(t1)) + 20;
            int bx = (int) Math.max(LEFT, Math.min(xs[hover] + 12, w - RIGHT - bw));
            int by = (int) Math.max(TOP, Math.min(ys[hover] - 24, TOP + plotH - 46));
            g.setColor(Theme.p().surfaceRaised());
            g.fillRoundRect(bx, by, bw, 46, 12, 12);
            g.setColor(Theme.p().border());
            g.drawRoundRect(bx, by, bw, 46, 12, 12);
            g.setColor(Theme.p().muted());
            g.setFont(Theme.regular(11f));
            g.drawString(t1, bx + 10, by + 17);
            g.setColor(Theme.p().text());
            g.setFont(Theme.bold(12.5f));
            g.drawString(t2, bx + 10, by + 36);
        }
        g.dispose();
    }

    private static void marker(Graphics2D g, double x, double y, Color c) {
        g.setColor(Theme.p().surface());
        g.fillOval((int) x - 6, (int) y - 6, 12, 12);
        g.setColor(c);
        g.fillOval((int) x - 4, (int) y - 4, 8, 8);
    }
}
